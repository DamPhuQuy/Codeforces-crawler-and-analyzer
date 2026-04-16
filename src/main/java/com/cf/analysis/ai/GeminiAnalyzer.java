package com.cf.analysis.ai;

import com.cf.analysis.model.Analysis;
import com.cf.analysis.model.Submission;
import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Phân tích source code bằng Google Gemini AI.
 *
 * Sử dụng Gemini REST API trực tiếp (không dùng SDK để tránh dependency issues).
 * Endpoint: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
 *
 * Phân tích 6 tiêu chí phát hiện AI-generated code:
 * 1. too_clean       - Code quá sạch
 * 2. textbook_comments - Comment kiểu sách giáo khoa
 * 3. perfect_naming  - Đặt tên quá chuẩn
 * 4. ai_pattern      - Pattern giống AI
 * 5. too_perfect     - Không có lỗi vặt
 * 6. wrong_style     - Style không giống CP
 */
public class GeminiAnalyzer {

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private String     apiKey;
    private final OkHttpClient httpClient;
    private final Gson         gson;

    public GeminiAnalyzer(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // Gemini có thể chậm
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    /**
     * Phân tích một submission bằng Gemini AI.
     *
     * @param submission Submission cần phân tích (phải có sourceCode)
     * @return Analysis object chứa kết quả đầy đủ
     */
    public Analysis analyze(Submission submission) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình Gemini API Key! Vào tab Cài Đặt để nhập key.");
        }
        if (submission.getSourceCode() == null || submission.getSourceCode().isBlank()) {
            throw new IllegalArgumentException("Submission #" + submission.getSubmissionId() + " không có source code!");
        }

        String prompt      = buildPrompt(submission);
        String rawResponse = callGemini(prompt);
        return parseResponse(rawResponse, submission.getId());
    }

    // ==================== Prompt Builder ====================

    /**
     * Xây dựng prompt chi tiết gửi cho Gemini.
     * Yêu cầu trả về JSON với format chính xác để parse được.
     */
    private String buildPrompt(Submission submission) {
        // Giới hạn source code nếu quá dài (Gemini có token limit)
        String code = submission.getSourceCode();
        if (code.length() > 8000) {
            code = code.substring(0, 8000) + "\n// ... (code bị cắt do quá dài)";
        }

        return """
            Bạn là chuyên gia phân tích code competitive programming. Hãy phân tích đoạn code sau.
            
            **Thông tin submission:**
            - Ngôn ngữ: %s
            - Bài toán: %s
            - Kết quả nộp: %s
            
            **Source code:**
            ```
            %s
            ```
            
            **YÊU CẦU:** Trả về JSON thuần (không có text gì khác, không có markdown):
            
            {
              "data_structures": ["Array", "HashMap"],
              "algorithms": ["BFS", "Dynamic Programming"],
              "ai_detected": true,
              "ai_confidence": 0.75,
              "ai_indicators": {
                "too_clean": { "detected": true, "evidence": "Không có debug print, formatting hoàn hảo" },
                "textbook_comments": { "detected": false, "evidence": "" },
                "perfect_naming": { "detected": true, "evidence": "Biến đặt tên như adjacencyList, shortestDistance" },
                "ai_pattern": { "detected": false, "evidence": "" },
                "too_perfect": { "detected": true, "evidence": "Không có biến thừa, không có code comment tạm" },
                "wrong_style": { "detected": false, "evidence": "" }
              },
              "highlighted_lines": [
                { "line": 5, "reason": "Biến đặt tên quá chuẩn, không giống dân CP", "category": "perfect_naming" }
              ],
              "time_complexity": "O(n log n)",
              "space_complexity": "O(n)",
              "difficulty_score": 6,
              "explanation": "Code sử dụng thuật toán BFS với HashMap để lưu khoảng cách..."
            }
            
            **Hướng dẫn đánh giá 6 tiêu chí AI:**
            1. too_clean: Code quá gọn gàng bất thường - dân CP thường để lại debug print, code thừa
            2. textbook_comments: Comment giải thích chi tiết từng bước như // Initialize distance array with infinity
            3. perfect_naming: adjacencyList, shortestDistance thay vì adj, dist - dân CP dùng tên ngắn gọn
            4. ai_pattern: Cấu trúc code generic, helper functions riêng, Stream API không cần thiết
            5. too_perfect: Xử lý hết edge cases, không có biến unused, import dọn sạch
            6. wrong_style: import java.util.HashMap thay vì import java.util.*, class đặt tên cẩn thận thay vì Main
            
            **CTDL cần nhận dạng:** Array/ArrayList, LinkedList, Stack, Queue, Deque, PriorityQueue, HashMap, HashSet, TreeMap, TreeSet, Trie, Graph (Adjacency List/Matrix), Binary Tree, Segment Tree, Fenwick Tree/BIT, Union-Find/DSU, Sparse Table
            
            **Thuật toán cần nhận dạng:** Sorting, Binary Search, BFS, DFS, Dijkstra, Bellman-Ford, Floyd-Warshall, Dynamic Programming, Greedy, Divide & Conquer, Two Pointers, Sliding Window, KMP, Suffix Array, Backtracking, Topological Sort, SCC, Network Flow, Convex Hull, FFT
            
            Trả về JSON thuần, không có ```json hay text nào khác.
            """.formatted(
                submission.getLanguage(),
                submission.getProblemName(),
                submission.getVerdict(),
                code
            );
    }

    // ==================== API Call ====================

    /**
     * Gọi Gemini REST API và trả về text response.
     */
    private String callGemini(String prompt) throws IOException {
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

    /**
     * Trích xuất text từ JSON response của Gemini.
     * Response format: { candidates: [{ content: { parts: [{ text: "..." }] } }] }
     */
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
            JsonArray  respParts   = respContent.getAsJsonArray("parts");

            if (respParts == null || respParts.isEmpty()) {
                throw new IOException("Gemini response không có content!");
            }

            return respParts.get(0).getAsJsonObject().get("text").getAsString();

        } catch (JsonParseException e) {
            throw new IOException("Không parse được Gemini response: " + e.getMessage());
        }
    }

    // ==================== Response Parser ====================

    /**
     * Parse JSON text từ Gemini thành Analysis object.
     * Robust - không crash nếu thiếu field nào.
     */
    private Analysis parseResponse(String rawText, long submissionId) {
        Analysis analysis = new Analysis();
        analysis.setSubmissionId(submissionId);
        analysis.setRawJson(rawText);

        try {
            String cleanJson = extractJson(rawText);
            JsonObject obj   = gson.fromJson(cleanJson, JsonObject.class);

            // CTDL
            List<String> ds = parseStringList(obj, "data_structures");
            analysis.setDataStructures(ds);

            // Thuật toán
            List<String> algos = parseStringList(obj, "algorithms");
            analysis.setAlgorithms(algos);

            // AI detection
            analysis.setAiDetected(obj.has("ai_detected") && obj.get("ai_detected").getAsBoolean());
            analysis.setAiConfidence(obj.has("ai_confidence") ? obj.get("ai_confidence").getAsDouble() : 0.0);

            // 6 tiêu chí
            if (obj.has("ai_indicators") && obj.get("ai_indicators").isJsonObject()) {
                analysis.setAiIndicators(parseIndicators(obj.getAsJsonObject("ai_indicators")));
            } else {
                analysis.setAiIndicators(new Analysis.AiIndicators());
            }

            // Highlighted lines
            analysis.setHighlightedLines(parseHighlightedLines(obj));

            // Complexity
            analysis.setTimeComplexity( obj.has("time_complexity")  ? obj.get("time_complexity").getAsString()  : "N/A");
            analysis.setSpaceComplexity(obj.has("space_complexity") ? obj.get("space_complexity").getAsString() : "N/A");
            analysis.setDifficultyScore(obj.has("difficulty_score") ? obj.get("difficulty_score").getAsInt()    : 0);
            analysis.setExplanation(    obj.has("explanation")      ? obj.get("explanation").getAsString()      : "");

        } catch (Exception e) {
            // Nếu parse thất bại, set giá trị an toàn và ghi lỗi vào explanation
            System.err.println("⚠️ Lỗi parse Gemini response: " + e.getMessage());
            analysis.setDataStructures(new ArrayList<>());
            analysis.setAlgorithms(new ArrayList<>());
            analysis.setAiIndicators(new Analysis.AiIndicators());
            analysis.setHighlightedLines(new ArrayList<>());
            analysis.setExplanation("⚠️ Lỗi phân tích: " + e.getMessage()
                                    + "\n\nRaw response:\n" + rawText.substring(0, Math.min(500, rawText.length())));
        }

        return analysis;
    }

    /**
     * Trích xuất JSON từ text, xử lý trường hợp Gemini thêm markdown.
     */
    private String extractJson(String text) {
        text = text.trim();

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

        // Tìm { ... } đầu tiên và cuối cùng
        int braceStart = text.indexOf('{');
        int braceEnd   = text.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            return text.substring(braceStart, braceEnd + 1);
        }

        return text;
    }

    // ==================== Parse Helpers ====================

    private List<String> parseStringList(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray(key)) {
                if (!e.isJsonNull()) list.add(e.getAsString());
            }
        }
        return list;
    }

    private Analysis.AiIndicators parseIndicators(JsonObject indicators) {
        Analysis.AiIndicators ai = new Analysis.AiIndicators();

        ai.tooClean               = getIndicatorDetected(indicators, "too_clean");
        ai.tooCleanEvidence       = getIndicatorEvidence(indicators, "too_clean");
        ai.textbookComments       = getIndicatorDetected(indicators, "textbook_comments");
        ai.textbookCommentsEvidence = getIndicatorEvidence(indicators, "textbook_comments");
        ai.perfectNaming          = getIndicatorDetected(indicators, "perfect_naming");
        ai.perfectNamingEvidence  = getIndicatorEvidence(indicators, "perfect_naming");
        ai.aiPattern              = getIndicatorDetected(indicators, "ai_pattern");
        ai.aiPatternEvidence      = getIndicatorEvidence(indicators, "ai_pattern");
        ai.tooPerfect             = getIndicatorDetected(indicators, "too_perfect");
        ai.tooPerfectEvidence     = getIndicatorEvidence(indicators, "too_perfect");
        ai.wrongStyle             = getIndicatorDetected(indicators, "wrong_style");
        ai.wrongStyleEvidence     = getIndicatorEvidence(indicators, "wrong_style");

        return ai;
    }

    private List<Analysis.HighlightedLine> parseHighlightedLines(JsonObject obj) {
        List<Analysis.HighlightedLine> lines = new ArrayList<>();
        if (!obj.has("highlighted_lines") || !obj.get("highlighted_lines").isJsonArray()) return lines;

        for (JsonElement e : obj.getAsJsonArray("highlighted_lines")) {
            if (!e.isJsonObject()) continue;
            JsonObject l  = e.getAsJsonObject();
            int    line   = l.has("line")     ? l.get("line").getAsInt()        : 0;
            String reason = l.has("reason")   ? l.get("reason").getAsString()   : "";
            String cat    = l.has("category") ? l.get("category").getAsString() : "";
            if (line > 0) lines.add(new Analysis.HighlightedLine(line, reason, cat));
        }
        return lines;
    }

    private boolean getIndicatorDetected(JsonObject indicators, String key) {
        if (!indicators.has(key) || !indicators.get(key).isJsonObject()) return false;
        JsonObject o = indicators.getAsJsonObject(key);
        return o.has("detected") && o.get("detected").getAsBoolean();
    }

    private String getIndicatorEvidence(JsonObject indicators, String key) {
        if (!indicators.has(key) || !indicators.get(key).isJsonObject()) return "";
        JsonObject o = indicators.getAsJsonObject(key);
        return o.has("evidence") && !o.get("evidence").isJsonNull()
               ? o.get("evidence").getAsString() : "";
    }
}
