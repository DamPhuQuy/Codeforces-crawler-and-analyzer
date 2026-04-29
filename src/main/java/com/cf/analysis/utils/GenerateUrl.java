package com.cf.analysis.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import io.github.cdimascio.dotenv.Dotenv;

public class GenerateUrl {

    static Dotenv dotenv = Dotenv.load();

    private static final String apiKey = dotenv.get("API_KEY");
    private static final String apiSecret = dotenv.get("API_SECRET");

    public static String generateApi(String methodName, Map<String, String> extraParams) throws Exception {

        String rand = String.format("%06d", new Random().nextInt(1_000_000));
        long time = System.currentTimeMillis() / 1000;

        Map<String, String> params = new TreeMap<>();

        params.put("apiKey", apiKey);
        params.put("time", String.valueOf(time));

        if (extraParams != null) {
            params.putAll(extraParams);
        }

        List<String> paramList = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramList.add(entry.getKey() + "=" + entry.getValue());
        }
        String apiString = String.join("&", paramList);

        String toHash = String.format("%s/%s?%s#%s", rand, methodName, apiString, apiSecret);

        String hash = sha512(toHash);
        String apiSig = rand + hash;

        return String.format(
                "https://codeforces.com/api/%s?%s&apiSig=%s",
                methodName,
                apiString,
                apiSig
        );
    }


    private static String sha512(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private GenerateUrl() {}
}
