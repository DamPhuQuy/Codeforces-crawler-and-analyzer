package com.cf.analysis.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cf.analysis.model.analysis.AiIndicators;
import com.cf.analysis.model.analysis.AiResult;
import com.cf.analysis.model.analysis.Analysis;
import com.cf.analysis.model.analysis.AnalysisOutput;
import com.cf.analysis.model.analysis.ComplexityAnalysis;
import com.cf.analysis.model.analysis.Indicator;
import com.cf.analysis.model.submission.Submission;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiAnalyzer {

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_BACKOFF_MS = 1000;

    private String apiKey;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public GeminiAnalyzer(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public Analysis analyze(Submission submission) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình Gemini API Key! Vào tab Cài Đặt để nhập key.");
        }
        if (submission.getSourceCode() == null || submission.getSourceCode().isBlank()) {
            throw new IllegalArgumentException("Submission #" + submission.getSubmissionId() + " không có source code!");
        }

        String prompt = buildPrompt(submission);
        String rawResponse = callGemini(prompt);
        return parseResponse(rawResponse, submission.getId());
    }


    private String buildPrompt(Submission submission) {
        String code = submission.getSourceCode();
        if (code.length() > 8000) {
            code = code.substring(0, 8000) + "\n// ... (code bị cắt do quá dài)";
        }

        return """
            Bạn là chuyên gia phân tích code competitive programming.

            Mục tiêu:
            - Phân tích cấu trúc dữ liệu và thuật toán
            - Đánh giá chất lượng code
            - Phát hiện các dấu hiệu BẤT THƯỜNG có thể liên quan đến việc sử dụng AI

            QUAN TRỌNG:
            - KHÔNG được kết luận chắc chắn code có sử dụng AI
            - Chỉ được đưa ra "mức độ nghi ngờ" (ai_suspicion_score từ 0 → 1)
            - Mọi nhận định phải có evidence cụ thể BẰNG TIẾNG VIỆT
            - Hãy khách quan: code tốt KHÔNG tự động nghĩa là AI-generated
            - Chỉ đánh giá cao khi có NHIỀU dấu hiệu bất thường cùng lúc

            ---

            Thông tin submission:
            - Ngôn ngữ: %s
            - Bài toán: %s
            - Kết quả: %s

            Source code:
            %s

            ---

            Trả về JSON thuần (TẤT CẢ NỘI DUNG PHẢI BẰNG TIẾNG VIỆT):

            {
                "data_structures": [],
                "algorithms": [],
                "ai_suspicion_score": 0.0,
                "ai_indicators": {
                    "too_clean": { "score": 0.0, "evidence": "" },
                    "textbook_comments": { "score": 0.0, "evidence": "" },
                    "perfect_naming": { "score": 0.0, "evidence": "" },
                    "ai_pattern": { "score": 0.0, "evidence": "" },
                    "too_perfect": { "score": 0.0, "evidence": "" },
                    "wrong_style": { "score": 0.0, "evidence": "" }
                },
                "highlighted_lines": [],
                "time_complexity": "",
                "space_complexity": "",
                "difficulty_score": 5,
                "confidence": 0.0,
                "explanation": ""
            }

            ---

            Hướng dẫn:

            - ai_suspicion_score:
            = trung bình các indicator.score (KHÔNG phải tổng)
            = chỉ cao (>0.6) khi có NHIỀU dấu hiệu bất thường

            - confidence:
            = độ chắc chắn của phân tích (không phải AI detection)

            - difficulty_score:
            dựa vào:
                + thuật toán
                + độ dài code
                + complexity

            - explanation:
            = nhận xét tổng quan BẰNG TIẾNG VIỆT về code, thuật toán, và các dấu hiệu AI (nếu có)

            ---

            Các dấu hiệu cần chú ý (mỗi indicator có score 0.0-1.0):

            1. too_clean:
            code cực kỳ sạch, không có dấu vết debug/trial-error
            evidence: giải thích BẰNG TIẾNG VIỆT

            2. textbook_comments:
            comment mang tính giáo trình, giải thích quá chi tiết
            evidence: giải thích BẰNG TIẾNG VIỆT

            3. perfect_naming:
            tên biến quá đầy đủ (adjacencyList vs adj)
            evidence: giải thích BẰNG TIẾNG VIỆT

            4. ai_pattern:
            structure giống template AI (helper functions, abstraction không cần thiết)
            evidence: giải thích BẰNG TIẾNG VIỆT

            5. too_perfect:
            không có lỗi nhỏ, không có code thừa, quá hoàn hảo
            evidence: giải thích BẰNG TIẾNG VIỆT

            6. wrong_style:
            style không giống CP (Java verbose, class naming chuẩn)
            evidence: giải thích BẰNG TIẾNG VIỆT

            ---

            LƯU Ý: Tất cả các trường "evidence" và "explanation" PHẢI viết bằng TIẾNG VIỆT.
            Chỉ trả về JSON, không có text ngoài.
        """.formatted(
                submission.getLanguage(),
                submission.getProblemName(),
                submission.getVerdict(),
                code
            );
    }


    private String callGemini(String prompt) throws IOException {
        IOException lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return callGeminiOnce(prompt);
            } catch (IOException e) {
                lastException = e;

                // Không retry nếu là lỗi client (4xx)
                if (e.getMessage().contains("HTTP 4")) {
                    throw e;
                }

                // Exponential backoff
                if (attempt < MAX_RETRIES - 1) {
                    int backoffMs = INITIAL_BACKOFF_MS * (1 << attempt);
                    System.err.println("Gemini API lỗi (attempt " + (attempt + 1) + "/" + MAX_RETRIES + "), retry sau " + backoffMs + "ms: " + e.getMessage());
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Bị interrupt khi retry", ie);
                    }
                }
            }
        }

        throw new IOException("Gemini API thất bại sau " + MAX_RETRIES + " lần thử", lastException);
    }

    /**
     * Thực hiện một lần gọi API đến Gemini.
     */
    private String callGeminiOnce(String prompt) throws IOException {
        // Xây dựng request body theo Gemini API format
        JsonObject requestBody = new JsonObject();

        JsonArray  contents = new JsonArray();
        JsonObject content  = new JsonObject();
        JsonArray  parts    = new JsonArray();
        JsonObject part     = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        // Cấu hình generation: temperature thấp = nhất quán, không sáng tạo
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature",     0.1);
        genConfig.addProperty("maxOutputTokens", 2048);
        genConfig.addProperty("responseMimeType", "application/json");
        requestBody.add("generationConfig", genConfig);

        // Build HTTP request
        String      urlWithKey  = GEMINI_URL + "?key=" + apiKey;
        RequestBody requestHttp = RequestBody.create(
            gson.toJson(requestBody),
            MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(urlWithKey)
            .post(requestHttp)
            .build();

        // Execute và extract text
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errBody = response.body() != null ? response.body().string() : "no body";
                throw new IOException("Gemini API lỗi HTTP " + response.code() + ": " + errBody);
            }
            return extractTextFromResponse(response.body().string());
        }
    }

    private String extractTextFromResponse(String jsonBody) throws IOException {
        try {
            JsonObject obj        = gson.fromJson(jsonBody, JsonObject.class);
            JsonArray  candidates = obj.getAsJsonArray("candidates");

            if (candidates == null || candidates.isEmpty()) {
                throw new IOException("Gemini không trả về candidates!");
            }

            JsonObject candidate = candidates.get(0).getAsJsonObject();

            // Kiểm tra finish reason
            if (candidate.has("finishReason")) {
                String reason = candidate.get("finishReason").getAsString();
                if ("SAFETY".equals(reason)) {
                    throw new IOException("Gemini từ chối phân tích vì lý do safety!");
                }
            }

            JsonObject respContent = candidate.getAsJsonObject("content");
            if (respContent == null) {
                throw new IOException("Gemini response không có content!");
            }

            JsonArray respParts = respContent.getAsJsonArray("parts");
            if (respParts == null || respParts.isEmpty()) {
                throw new IOException("Gemini response không có parts!");
            }

            JsonElement textElement = respParts.get(0).getAsJsonObject().get("text");
            if (textElement == null || textElement.isJsonNull()) {
                throw new IOException("Gemini response không có text!");
            }

            return textElement.getAsString();

        } catch (JsonParseException e) {
            throw new IOException("Không parse được Gemini response: " + e.getMessage());
        } catch (NullPointerException e) {
            throw new IOException("Gemini response thiếu field bắt buộc: " + e.getMessage());
        }
    }

    private Analysis parseResponse(String rawText, long submissionId) {
        Analysis analysis = new Analysis();
        analysis.setSubmissionId(submissionId);

        try {
            String cleanJson = extractJson(rawText);
            JsonObject obj   = gson.fromJson(cleanJson, JsonObject.class);

            // === ComplexityAnalysis ===
            ComplexityAnalysis complexity = new ComplexityAnalysis();
            complexity.setDataStructures(parseStringList(obj, "data_structures"));
            complexity.setAlgorithms(parseStringList(obj, "algorithms"));
            complexity.setTimeComplexity(obj.has("time_complexity") ? obj.get("time_complexity").getAsString() : "N/A");
            complexity.setSpaceComplexity(obj.has("space_complexity") ? obj.get("space_complexity").getAsString() : "N/A");
            complexity.setDifficultyScore(obj.has("difficulty_score") ? obj.get("difficulty_score").getAsInt() : 0);
            analysis.setComplexityAnalysis(complexity);

            // === AiResult ===
            AiResult aiResult = new AiResult();
            float aiConfidence = obj.has("ai_suspicion_score") ? obj.get("ai_suspicion_score").getAsFloat() : 0.0f;
            aiResult.setAiConfidence(aiConfidence);

            if (obj.has("ai_indicators") && obj.get("ai_indicators").isJsonObject()) {
                aiResult.setAiIndicators(parseIndicators(obj.getAsJsonObject("ai_indicators")));
            } else {
                aiResult.setAiIndicators(new AiIndicators());
            }
            analysis.setAiResult(aiResult);

            // === AnalysisOutput ===
            AnalysisOutput output = new AnalysisOutput();
            output.setExplanation(obj.has("explanation") ? obj.get("explanation").getAsString() : "");
            output.setRawJson(rawText);
            analysis.setAnalysisOutput(output);

        } catch (Exception e) {
            // Nếu parse thất bại, set giá trị an toàn và ghi lỗi vào explanation
            System.err.println("Lỗi parse Gemini response: " + e.getMessage());

            analysis.setComplexityAnalysis(new ComplexityAnalysis());
            analysis.setAiResult(new AiResult());

            AnalysisOutput output = new AnalysisOutput();
            output.setExplanation("Lỗi phân tích: " + e.getMessage()
                                    + "\n\nRaw response:\n" + rawText.substring(0, Math.min(500, rawText.length())));
            output.setRawJson(rawText);
            analysis.setAnalysisOutput(output);
        }

        return analysis;
    }

    private String extractJson(String text) {
        text = text.trim();

        // Nếu đã là JSON thuần (bắt đầu bằng {)
        if (text.startsWith("{")) {
            return text;
        }

        // Thử tìm ```json ... ```
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end   = text.indexOf("```", start);
            if (end > start) return text.substring(start, end).trim();
        }

        // Thử tìm ``` ... ```
        if (text.startsWith("```")) {
            int start = text.indexOf('\n') + 1;
            int end   = text.lastIndexOf("```");
            if (end > start) return text.substring(start, end).trim();
        }

        // Tìm { ... } đầu tiên và cuối cùng (fallback)
        int braceStart = text.indexOf('{');
        int braceEnd   = text.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            return text.substring(braceStart, braceEnd + 1);
        }

        return text;
    }

    private List<String> parseStringList(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray(key)) {
                if (!e.isJsonNull()) list.add(e.getAsString());
            }
        }
        return list;
    }

    private AiIndicators parseIndicators(JsonObject indicators) {
        AiIndicators ai = new AiIndicators();

        ai.setTooClean(parseIndicator(indicators, "too_clean"));
        ai.setTextbookComments(parseIndicator(indicators, "textbook_comments"));
        ai.setPerfectNaming(parseIndicator(indicators, "perfect_naming"));
        ai.setAiPattern(parseIndicator(indicators, "ai_pattern"));
        ai.setTooPerfect(parseIndicator(indicators, "too_perfect"));
        ai.setWrongStyle(parseIndicator(indicators, "wrong_style"));

        return ai;
    }

    private Indicator parseIndicator(JsonObject indicators, String key) {
        if (!indicators.has(key) || !indicators.get(key).isJsonObject()) {
            return new Indicator(false, "");
        }

        JsonObject obj = indicators.getAsJsonObject(key);

        // Lấy score (ưu tiên) hoặc detected (fallback)
        float score = 0.0f;
        if (obj.has("score") && !obj.get("score").isJsonNull()) {
            score = obj.get("score").getAsFloat();
        } else if (obj.has("detected") && !obj.get("detected").isJsonNull()) {
            score = obj.get("detected").getAsBoolean() ? 1.0f : 0.0f;
        }

        // Lấy evidence
        String evidence = "";
        if (obj.has("evidence") && !obj.get("evidence").isJsonNull()) {
            evidence = obj.get("evidence").getAsString();
        }

        // Detected = true nếu score > 0.5
        boolean detected = score > 0.5f;

        return new Indicator(detected, evidence);
    }

    private boolean getIndicatorDetected(JsonObject indicators, String key) {
        if (!indicators.has(key) || !indicators.get(key).isJsonObject()) return false;
        JsonObject o = indicators.getAsJsonObject(key);

        // Ưu tiên score, fallback sang detected
        if (o.has("score") && !o.get("score").isJsonNull()) {
            return o.get("score").getAsFloat() > 0.5f;
        }
        if (o.has("detected") && !o.get("detected").isJsonNull()) {
            return o.get("detected").getAsBoolean();
        }
        return false;
    }

    private String getIndicatorEvidence(JsonObject indicators, String key) {
        if (!indicators.has(key) || !indicators.get(key).isJsonObject()) return "";
        JsonObject o = indicators.getAsJsonObject(key);
        return o.has("evidence") && !o.get("evidence").isJsonNull()
               ? o.get("evidence").getAsString() : "";
    }
}
