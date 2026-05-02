package com.cf.analysis.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cf.analysis.model.submission.Submission;
import com.cf.analysis.model.submission.TestSet;
import com.cf.analysis.model.submission.Verdict;
import com.cf.analysis.model.user.User;
import com.cf.analysis.utils.GenerateUrl;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CodeforcesApiCaller {
    private static final ThreadLocal<RateLimiter> rateLimiter = ThreadLocal.withInitial(() ->
            RateLimiter.create(0.8)
    );

    private final OkHttpClient httpClient;
    private final Gson gson;

    public CodeforcesApiCaller(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    private JsonObject callApi(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("HTTP " + response.code() + " when call: " + url);
            }
            return gson.fromJson(response.body().string(), JsonObject.class);
        }
    }

    public List<User> getUserInfo(List<String> handles) throws IOException {
        if (handles == null || handles.isEmpty()) return List.of();

        rateLimiter.get().acquire();

        String queryMethod = "user.info";

        String handlesParam = String.join(";", handles);

        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("handles", handlesParam);

        try {
            String apiUrl = GenerateUrl.generateApi(queryMethod, extraParams);

            JsonObject userInfo = callApi(apiUrl);

            if (!"OK".equals(userInfo.get("status").getAsString())) {
                String comment = userInfo.has("comment") ? userInfo.get("comment").getAsString() : "";
                throw new IOException("API error: " + comment);
            }

            JsonArray results = userInfo.getAsJsonArray("result");

            if (results == null || results.isEmpty()) return List.of();

            List<User> users = new ArrayList<>();

            for (JsonElement result : results) {
                JsonObject u = result.getAsJsonObject();
                users.add(parseUser(u));
            }

            return users;

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return List.of();
        }
    }


    private User parseUser(JsonObject jsonUser) {
        User u = new User(jsonUser.get("handle").getAsString());

        u.setFirstName(jsonUser.has("firstName") ? jsonUser.get("firstName").getAsString() : "");
        u.setLastName(jsonUser.has("lastName") ? jsonUser.get("lastName").getAsString() : "");

        u.setRating(jsonUser.has("rating") ? jsonUser.get("rating").getAsInt() : 0);
        u.setMaxRating(jsonUser.has("maxRating") ? jsonUser.get("maxRating").getAsInt() : 0);
        u.setRank(jsonUser.has("rank") ? jsonUser.get("rank").getAsString() : "newbie");
        u.setCountry(jsonUser.has("country") ? jsonUser.get("country").getAsString() : "");
        u.setAvatarUrl(jsonUser.has("avatarUrl") ? jsonUser.get("avatarUrl").getAsString() : "");

        return u;
    }

    public List<Submission> getUserSubmissions(String handle, int maxCount, long minSubId)
            throws IOException {

        rateLimiter.get().acquire();

        try {
            String method = "user.status";

            Map<String, String> params = new HashMap<>();
            params.put("handle", handle);
            params.put("from", "1");
            params.put("count", String.valueOf(maxCount));

            String url = GenerateUrl.generateApi(method, params);

            JsonObject response = callApi(url);

            if (!"OK".equals(response.get("status").getAsString())) {
                throw new IOException("Không lấy được submissions của " + handle);
            }

            List<Submission> results = new ArrayList<>();
            JsonArray items = response.getAsJsonArray("result");

            for (JsonElement element : items) {
                JsonObject s = element.getAsJsonObject();
                int subId = s.get("id").getAsInt();

                System.out.println("subId = " + subId);

                if (subId <= minSubId) {
                    break;
                }

                if (isAcceptedSubmission(s)) {
                    Submission sub = parseSubmission(handle, subId, s);
                    results.add(sub);
                }
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private boolean isAcceptedSubmission(JsonObject s) {
        System.out.println("verdict = " + s.get("verdict").getAsString());
        return s.has("verdict") && "OK".equals(s.get("verdict").getAsString());
    }

    private Submission parseSubmission(String handle, Integer subId, JsonObject s) {
        Submission sub = new Submission(subId);
        sub.setUserHandle(handle);
        sub.setVerdict(Verdict.OK);

        if (s.has("contestId") && !s.get("contestId").isJsonNull()) {
            sub.setContestId(s.get("contestId").getAsInt());
        }

        sub.setCreationTimeSeconds(s.has("creationTimeSeconds") ? s.get("creationTimeSeconds").getAsInt() : 0);
        sub.setRelativeTimeSeconds(s.has("relativeTimeSeconds") ? s.get("relativeTimeSeconds").getAsInt() : 0);

        String programmingLanguage = s.has("programmingLanguage") ? s.get("programmingLanguage").getAsString() : "";
        sub.setProgrammingLanguage(programmingLanguage);

        sub.setPassedTestCount(s.has("passedTestCount") ? s.get("passedTestCount").getAsInt() : 0);
        sub.setTimeConsumedMillis(
                s.has("timeConsumedMillis") ? s.get("timeConsumedMillis").getAsInt() : 0
        );

        sub.setMemoryConsumedBytes(
                s.has("memoryConsumedBytes") ? s.get("memoryConsumedBytes").getAsInt() : 0
        );
        sub.setPoints(s.has("points") ? s.get("points").getAsFloat() : 0);
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
}
