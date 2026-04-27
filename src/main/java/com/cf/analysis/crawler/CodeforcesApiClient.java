package com.cf.analysis.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cf.analysis.model.submission.Submission;
import com.cf.analysis.model.submission.TestSet;
import com.cf.analysis.model.submission.Verdict;
import com.cf.analysis.model.user.User;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

    private static final RateLimiter apiRateLimiter = RateLimiter.create(0.8);
    private static final RateLimiter scrapeRateLimiter = RateLimiter.create(0.4);

    Dotenv dotenv = Dotenv.load();

    private static final String apiBase = "https://codeforces.com/api";

    private static final String userInfoUrl = "/user.info?handles=";

    private static final String userStatusUrl = "/user.status?handle=";

    private static final String sourceCodeUrl = "https://codeforces.com/contest/";

    private final OkHttpClient httpClient;
    private final Gson gson;

    public CodeforcesApiClient() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.gson = new GsonBuilder().create();
    }

    public List<User> getUserInfo(List<String> handles) throws IOException, InterruptedException {
        if (handles == null || handles.isEmpty()) return Collections.emptyList();

        apiRateLimiter.acquire();

        String url = apiBase + userInfoUrl + String.join(";", handles);
        JsonObject response = callApi(url);

        if (!"OK".equals(response.get("status").getAsString())) {
            String comment = response.has("comment") ? response.get("comment").getAsString() : "Unknown error";
            throw new IOException("Codeforces API lỗi: " + comment);
        }

        JsonArray results = response.getAsJsonArray("result");
        if (results == null || results.isEmpty()) return Collections.emptyList();

        List<User> users = new ArrayList<>();
        for (JsonElement element : results) {
            JsonObject u = element.getAsJsonObject();
            User user = new User(u.get("handle").getAsString());

            String firstName = getStringOrNull(u, "firstName");
            String lastName  = getStringOrNull(u, "lastName");
            user.setFirstName(firstName != null ? firstName : "");
            user.setLastName(lastName != null ? lastName : "");

            user.setRating(getIntOrDefault(u, "rating", 0));
            user.setMaxRating(getIntOrDefault(u, "maxRating", 0));
            user.setRank(getStringOrDefault(u, "rank", "newbie"));
            user.setCountry(getStringOrDefault(u, "country", ""));
            user.setAvatarUrl(getStringOrDefault(u, "titlePhoto", ""));

            users.add(user);
        }
        return users;
    }

    public List<Submission> getUserSubmissions(String handle, int maxCount, long minSubId)
            throws IOException, InterruptedException {

        apiRateLimiter.acquire();

        String url = apiBase + userStatusUrl + handle + "&from=1&count=" + maxCount;
        JsonObject response = callApi(url);

        if (!"OK".equals(response.get("status").getAsString())) {
            throw new IOException("Không lấy được submissions của " + handle);
        }

        List<Submission> results = new ArrayList<>();
        JsonArray items = response.getAsJsonArray("result");

        for (JsonElement element : items) {
            JsonObject s = element.getAsJsonObject();
            Integer subId = s.get("id").getAsInt();

            if (subId <= minSubId) {
                break;
            }

            if (isAcceptedSubmission(s)) {
                Submission sub = createSubmissionFromJson(handle, subId, s);
                results.add(sub);
            }
        }

        return results;
    }

    private boolean isAcceptedSubmission(JsonObject s) {
        return s.has("verdict") && "OK".equals(s.get("verdict").getAsString());
    }

    private Submission createSubmissionFromJson(String handle, Integer subId, JsonObject s) {
        Submission sub = new Submission(subId);
        sub.setUserHandle(handle);
        sub.setVerdict(Verdict.OK);

        if (s.has("contestId") && !s.get("contestId").isJsonNull()) {
            sub.setContestId(s.get("contestId").getAsInt());
        }

        sub.setCreationTimeSeconds(getIntOrDefault(s, "creationTimeSeconds", 0));
        sub.setRelativeTimeSeconds(getIntOrDefault(s, "relativeTimeSeconds", 0));

        String programmingLanguage = getStringOrDefault(s, "programmingLanguage", "");
        sub.setProgrammingLanguage(programmingLanguage);
        sub.setLanguage(programmingLanguage);

        sub.setPassedTestCount(getIntOrDefault(s, "passedTestCount", 0));
        sub.setTimeConsumedMillis(getIntOrDefault(s, "timeConsumedMillis", 0));
        sub.setMemoryConsumedBytes(getIntOrDefault(s, "memoryConsumedBytes", 0));
        sub.setPoints(getFloatOrDefault(s, "points", 0.0f));
        sub.setTestSet(parseTestSet(s));

        return sub;
    }

    private TestSet parseTestSet(JsonObject s) {
        if (s.has("testset") && !s.get("testset").isJsonNull()) {
            String testSetStr = s.get("testset").getAsString().toUpperCase();
            try {
                return TestSet.valueOf(testSetStr);
            } catch (IllegalArgumentException e) {
                return TestSet.SAMPLES;
            }
        }
        return TestSet.SAMPLES;
    }

    private float getFloatOrDefault(JsonObject obj, String key, float def) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsFloat() : def;
    }

    // scrape html
    public String getSubmissionSourceCode(int contestId, long submissionId)
            throws IOException, InterruptedException {

        scrapeRateLimiter.acquire();

        String url = sourceCodeUrl + contestId + "/submission/" + submissionId;

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
