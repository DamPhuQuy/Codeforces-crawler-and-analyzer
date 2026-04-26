package com.cf.analysis.crawler;

import com.cf.analysis.model.submission.Submission;
import com.cf.analysis.model.user.User;
import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client gọi Codeforces API để lấy thông tin user và submission.
 *
 * Codeforces API documentation: https://codeforces.com/apiHelp
 *
 * Lưu ý quan trọng:
 * - API có rate limit ~1 req/giây → luôn sleep giữa các request
 * - Source code KHÔNG có trong API → phải scrape HTML từ trang web
 * - Chỉ lấy submission có verdict = "OK" (Accepted)
 */
public class CodeforcesApiClient {

    private static final String API_BASE        = "https://codeforces.com/api";
    private static final long   DELAY_API_MS    = 1200; // Delay giữa các API call
    private static final long   DELAY_SCRAPE_MS = 2500; // Delay khi scrape HTML (dài hơn)

    private final OkHttpClient httpClient;
    private final Gson         gson;

    public CodeforcesApiClient() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.gson = new GsonBuilder().create();
    }

    // ==================== User Info ====================

    /**
     * Lấy thông tin user từ Codeforces API.
     * Endpoint: GET /api/user.info?handles={handle}
     *
     * @param handle Codeforces handle cần tìm
     * @return User với đầy đủ thông tin, null nếu không tồn tại
     * @throws IOException nếu lỗi mạng hoặc API trả về lỗi
     */
    public User getUserInfo(String handle) throws IOException, InterruptedException {
        String url = API_BASE + "/user.info?handles=" + handle;
        JsonObject response = callApi(url);

        if (!"OK".equals(response.get("status").getAsString())) {
            String comment = response.has("comment") ? response.get("comment").getAsString() : "Unknown error";
            throw new IOException("Codeforces API lỗi: " + comment);
        }

        JsonArray results = response.getAsJsonArray("result");
        if (results == null || results.isEmpty()) return null;

        JsonObject u = results.get(0).getAsJsonObject();

        User user = new User();
        user.setHandle(u.get("handle").getAsString());

        // Tên hiển thị: firstName + lastName nếu có, không thì dùng handle
        String firstName = getStringOrNull(u, "firstName");
        String lastName  = getStringOrNull(u, "lastName");
        if (firstName != null) {
            user.setDisplayName((firstName + " " + (lastName != null ? lastName : "")).trim());
        } else {
            user.setDisplayName(u.get("handle").getAsString());
        }

        user.setRating(getIntOrDefault(u, "rating", 0));
        user.setMaxRating(getIntOrDefault(u, "maxRating", 0));
        user.setRank(getStringOrDefault(u, "rank", "newbie"));
        user.setCountry(getStringOrDefault(u, "country", ""));
        user.setAvatarUrl(getStringOrDefault(u, "titlePhoto", ""));

        Thread.sleep(DELAY_API_MS);
        return user;
    }

    // ==================== Submissions ====================

    /**
     * Lấy danh sách submissions mới nhất của user (chỉ Accepted).
     * Endpoint: GET /api/user.status?handle={handle}&from=1&count={count}
     *
     * @param handle      Handle của user
     * @param maxCount    Số lượng submission tối đa cần kiểm tra
     * @param minSubId    Chỉ lấy submission có ID > giá trị này (để lấy mới)
     * @return Danh sách submission (chưa có source code - cần scrape riêng)
     */
    public List<Submission> getUserSubmissions(String handle, int maxCount, long minSubId)
            throws IOException, InterruptedException {

        String url = String.format("%s/user.status?handle=%s&from=1&count=%d", API_BASE, handle, maxCount);
        JsonObject response = callApi(url);

        if (!"OK".equals(response.get("status").getAsString())) {
            throw new IOException("Không lấy được submissions của " + handle);
        }

        List<Submission> results = new ArrayList<>();
        JsonArray items = response.getAsJsonArray("result");

        for (JsonElement element : items) {
            JsonObject s = element.getAsJsonObject();

            long subId = s.get("id").getAsLong();
            // Dừng khi gặp submission cũ hơn (submissions được trả về mới trước)
            if (subId <= minSubId) break;

            // Chỉ lấy Accepted
            String verdict = getStringOrDefault(s, "verdict", "");
            if (!"OK".equals(verdict)) continue;

            Submission sub = new Submission();
            sub.setSubmissionId(subId);
            sub.setUserHandle(handle);
            sub.setVerdict(verdict);

            if (s.has("contestId") && !s.get("contestId").isJsonNull()) {
                sub.setContestId(s.get("contestId").getAsInt());
            }

            // Thông tin bài toán
            if (s.has("problem") && !s.get("problem").isJsonNull()) {
                JsonObject problem = s.getAsJsonObject("problem");
                sub.setProblemName(getStringOrDefault(problem, "name", ""));
                sub.setProblemIndex(getStringOrDefault(problem, "index", ""));
            }

            sub.setLanguage(getStringOrDefault(s, "programmingLanguage", ""));
            sub.setTimeMs(getIntOrDefault(s, "timeConsumedMillis", 0));

            if (s.has("memoryConsumedBytes") && !s.get("memoryConsumedBytes").isJsonNull()) {
                sub.setMemoryKb(s.get("memoryConsumedBytes").getAsInt() / 1024);
            }

            // Thời gian submit (epoch seconds → SQL Timestamp)
            long epochSeconds = s.get("creationTimeSeconds").getAsLong();
            sub.setSubmittedAt(new Timestamp(epochSeconds * 1000L));

            results.add(sub);
        }

        Thread.sleep(DELAY_API_MS);
        return results;
    }

    /**
     * Lấy source code của submission bằng cách scrape HTML từ trang Codeforces.
     * Codeforces API KHÔNG cung cấp source code trực tiếp.
     *
     * URL format: https://codeforces.com/contest/{contestId}/submission/{submissionId}
     *
     * @return Source code string, hoặc null nếu không lấy được (code bị ẩn)
     */
    public String getSubmissionSourceCode(int contestId, long submissionId)
            throws IOException, InterruptedException {

        String url = String.format(
            "https://codeforces.com/contest/%d/submission/%d",
            contestId, submissionId
        );

        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            String html = response.body().string();
            return extractSourceFromHtml(html);
        } finally {
            Thread.sleep(DELAY_SCRAPE_MS); // Delay dài hơn khi scrape
        }
    }

    /**
     * Trích xuất source code từ HTML của trang submission.
     * Source code nằm trong: <pre id="program-source-text">...</pre>
     */
    private String extractSourceFromHtml(String html) {
        // Tìm thẻ pre chứa source code
        String marker = "id=\"program-source-text\"";
        int start = html.indexOf(marker);
        if (start == -1) {
            // Thử format khác
            marker = "class=\"program-source\"";
            start  = html.indexOf(marker);
            if (start == -1) return null;
        }

        // Tìm dấu > để lấy content bên trong
        int contentStart = html.indexOf('>', start) + 1;
        if (contentStart <= 0) return null;

        // Tìm thẻ đóng </pre>
        int contentEnd = html.indexOf("</pre>", contentStart);
        if (contentEnd == -1) return null;

        String code = html.substring(contentStart, contentEnd);

        // Decode HTML entities
        code = code.replace("&lt;",   "<")
                   .replace("&gt;",   ">")
                   .replace("&amp;",  "&")
                   .replace("&quot;", "\"")
                   .replace("&#39;",  "'")
                   .replace("&#x27;", "'")
                   .replace("&nbsp;", " ");

        return code.trim();
    }

    // ==================== HTTP Helper ====================

    /**
     * Gọi Codeforces API và parse JSON response.
     */
    private JsonObject callApi(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "CodeforcesExamAnalysis/1.0")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("HTTP " + response.code() + " khi gọi: " + url);
            }
            return gson.fromJson(response.body().string(), JsonObject.class);
        }
    }

    // ==================== JSON Helpers ====================

    private String getStringOrNull(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : null;
    }

    private String getStringOrDefault(JsonObject obj, String key, String def) {
        String v = getStringOrNull(obj, key);
        return v != null ? v : def;
    }

    private int getIntOrDefault(JsonObject obj, String key, int def) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : def;
    }
}
