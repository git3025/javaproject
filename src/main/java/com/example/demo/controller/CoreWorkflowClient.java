package com.example.demo.controller;

import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CoreWorkflowClient {
    private final String baseUrl;
    private final String workflowId;
    private final String appId;
    private final PrivateKey privateKey;
    private final OkHttpClient client;

    /**
     * 构造函数，初始化工作流客户端
     */
    public CoreWorkflowClient(String baseUrl, String workflowId, String appId, PrivateKey privateKey) {
        this.baseUrl = baseUrl;
        this.workflowId = workflowId;
        this.appId = appId;
        this.privateKey = privateKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 从PEM文件加载私钥
     */
    public static PrivateKey loadPrivateKeyFromPemFile(String pemFilePath) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(pemFilePath))) {
            StringBuilder sb = new StringBuilder();
            String line;
            boolean inKey = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("-----BEGIN PRIVATE KEY-----")) {
                    inKey = true;
                    continue;
                }
                if (line.startsWith("-----END PRIVATE KEY-----")) {
                    inKey = false;
                    continue;
                }
                if (inKey) {
                    sb.append(line);
                }
            }

            byte[] keyBytes = Base64.getDecoder().decode(sb.toString());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        }
    }

    /**
     * 生成请求签名
     */
    private String generateSignature(Map<String, String> params) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // 按参数名排序
        StringBuilder sb = new StringBuilder();
        params.keySet().stream()
                .sorted()
                .forEach(key -> sb.append(key).append(params.get(key)));

        String dataToSign = sb.toString();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * 调用工作流
     */
    public JSONObject invokeWorkflow(String knowledge, String stage, String subject, int number) throws Exception {
        // 1. 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("knowledge", knowledge);
        requestBody.put("stage", stage);
        requestBody.put("subject", subject);
        requestBody.put("number", number);

        // 2. 生成签名参数
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String nonce = UUID.randomUUID().toString();
        Map<String, String> signParams = new HashMap<>();
        signParams.put("app_id", appId);
        signParams.put("timestamp", timestamp);
        signParams.put("nonce", nonce);
        String signature = generateSignature(signParams);

        // 3. 构建请求
        Request request = new Request.Builder()
                .url(baseUrl  + workflowId + "/execute") // 注意添加/v1/
                .addHeader("App-Id", appId)
                .addHeader("Timestamp", timestamp)
                .addHeader("Nonce", nonce)
                .addHeader("Signature", signature)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .build();

        // 4. 发送请求并记录日志
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "null";
                throw new IOException("API Error: " + response.code() + " - " + errorBody);
            }
            return JSONObject.parseObject(response.body().string());
        }
    }
}