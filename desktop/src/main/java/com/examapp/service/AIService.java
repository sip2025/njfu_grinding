package com.examapp.service;

import com.examapp.data.AICacheManager;
import com.examapp.data.AISettingsManager;
import com.examapp.model.Question;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI服务类
 * 负责与AI API通信
 */
public class AIService {
    private static AIService instance;
    private final AISettingsManager settingsManager;
    private final AICacheManager cacheManager;
    private final OkHttpClient client;
    private final Gson gson;

    private AIService() {
        this.settingsManager = AISettingsManager.getInstance();
        this.cacheManager = AICacheManager.getInstance();
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }

    /**
     * 获取模型列表的回调接口
     */
    public interface ModelsCallback {
        void onSuccess(List<String> models);
        void onError(String error);
    }

    /**
     * 获取可用模型列表
     */
    public void fetchModels(ModelsCallback callback) {
        System.out.println("[AIService] ========== 开始获取模型列表 ==========");
        String baseUrl = settingsManager.getBaseUrl().trim();
        String apiKey = settingsManager.getApiKey().trim();
        System.out.println("[AIService] Base URL: " + baseUrl);
        System.out.println("[AIService] API Key: " + (apiKey.isEmpty() ? "未设置" : "已设置(" + apiKey.length() + "字符)"));

        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            System.err.println("[AIService] 错误: Base URL或API Key为空");
            callback.onError("Base URL和API Key不能为空");
            return;
        }

        // 构建模型列表API URL
        String modelsUrl;
        if (baseUrl.endsWith("#")) {
            // #标记表示用户提供了完整URL，不需要补全任何路径
            modelsUrl = baseUrl.substring(0, baseUrl.length() - 1);
            System.out.println("[AIService] 使用自定义完整URL（不补全）");
        } else {
            String cleanUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            modelsUrl = cleanUrl + "/v1/models";
            System.out.println("[AIService] 使用标准路径补全");
        }
        System.out.println("[AIService] 请求URL: " + modelsUrl);
        System.out.println("[AIService] 协议: " + (modelsUrl.startsWith("https") ? "HTTPS" : "HTTP"));

        Request request = new Request.Builder()
                .url(modelsUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        System.out.println("[AIService] 发送GET请求...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("[AIService] 网络请求失败: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
                callback.onError("网络请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("[AIService] 收到响应: HTTP " + response.code() + " " + response.message());
                try {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "无响应体";
                        System.err.println("[AIService] 请求失败: HTTP " + response.code());
                        System.err.println("[AIService] 错误详情: " + errorBody);
                        callback.onError("请求失败: HTTP " + response.code() + " - " + response.message());
                        return;
                    }

                    String responseBody = response.body().string();
                    System.out.println("[AIService] 响应体长度: " + responseBody.length() + " 字符");
                    System.out.println("[AIService] 响应内容: " + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");
                    
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                    List<String> models = new ArrayList<>();
                    if (jsonResponse.has("data")) {
                        JsonArray dataArray = jsonResponse.getAsJsonArray("data");
                        System.out.println("[AIService] 找到 " + dataArray.size() + " 个模型");
                        for (int i = 0; i < dataArray.size(); i++) {
                            JsonObject modelObj = dataArray.get(i).getAsJsonObject();
                            if (modelObj.has("id")) {
                                String modelId = modelObj.get("id").getAsString();
                                models.add(modelId);
                                System.out.println("[AIService]   - " + modelId);
                            }
                        }
                    }

                    if (models.isEmpty()) {
                        System.err.println("[AIService] 未找到可用模型");
                        callback.onError("未找到可用模型");
                    } else {
                        System.out.println("[AIService] 成功获取 " + models.size() + " 个模型");
                        callback.onSuccess(models);
                    }
                } catch (Exception e) {
                    System.err.println("[AIService] 解析响应失败: " + e.getMessage());
                    e.printStackTrace();
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 发送聊天请求的回调接口
     */
    public interface ChatCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    /**
     * 分析题目（使用固定提示词）
     */
    public void askQuestion(Question question, ChatCallback callback) {
        askQuestion(question, callback, false);
    }

    /**
     * 分析题目（使用固定提示词，可选强制刷新）
     */
    public void askQuestion(Question question, ChatCallback callback, boolean forceRefresh) {
        System.out.println("[AIService] ========== 开始分析题目 ==========");
        
        if (!settingsManager.isConfigured()) {
            callback.onError("请先在设置中配置AI参数");
            return;
        }

        // 构建选项文本
        StringBuilder optionsText = new StringBuilder();
        List<String> options = question.getOptions();
        if (options != null && !options.isEmpty()) {
            for (String option : options) {
                optionsText.append(option).append("\n");
            }
        } else {
            optionsText.append("无选项(判断题)");
        }

        // 使用固定提示词模板
        String promptTemplate = settingsManager.getAnswerPrompt();
        String prompt = promptTemplate
                .replace("{question}", question.getQuestionText())
                .replace("{options}", optionsText.toString())
                .replace("{answer}", question.getFormattedAnswer());

        System.out.println("[AIService] 题目: " + question.getQuestionText());
        System.out.println("[AIService] 答案: " + question.getFormattedAnswer());

        // 检查缓存（除非强制刷新）
        if (!forceRefresh) {
            String cacheKey = cacheManager.generateKey(prompt);
            String cachedResponse = cacheManager.getCache(cacheKey);
            if (cachedResponse != null) {
                System.out.println("命中AI缓存: " + cacheKey);
                System.out.println("[AIService] 命中缓存，直接返回响应");
                callback.onSuccess(cachedResponse);
                return;
            }
        }

        // 发送请求
        sendChatRequest(prompt, callback);
    }

    /**
     * 发送聊天请求
     */
    public void sendChatRequest(String prompt, ChatCallback callback) {
        System.out.println("[AIService] ========== 开始发送聊天请求 ==========");

        // 检查缓存
        String cacheKey = cacheManager.generateKey(prompt);
        String cachedResponse = cacheManager.getCache(cacheKey);
        if (cachedResponse != null) {
            System.out.println("命中AI缓存: " + cacheKey);
            System.out.println("[AIService] 命中缓存，直接返回响应");
            callback.onSuccess(cachedResponse);
            return;
        }

        System.out.println("[AIService] 未命中缓存，发起网络请求");
        String baseUrl = settingsManager.getBaseUrl().trim();
        String apiKey = settingsManager.getApiKey().trim();
        String model = settingsManager.getModel();
        
        System.out.println("[AIService] Base URL: " + baseUrl);
        System.out.println("[AIService] 模型: " + model);
        System.out.println("[AIService] Temperature: " + settingsManager.getTemperature());
        System.out.println("[AIService] Top-P: " + settingsManager.getTopP());
        System.out.println("[AIService] 提示词长度: " + prompt.length() + " 字符");

        if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
            System.err.println("[AIService] 错误: 配置不完整");
            callback.onError("请先配置AI设置");
            return;
        }

        // 构建聊天API URL
        String chatUrl;
        if (baseUrl.endsWith("#")) {
            // #标记表示用户提供了完整URL，不需要补全任何路径
            chatUrl = baseUrl.substring(0, baseUrl.length() - 1);
            System.out.println("[AIService] 使用自定义完整URL（不补全）");
        } else {
            String cleanUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            chatUrl = cleanUrl + "/v1/chat/completions";
            System.out.println("[AIService] 使用标准路径补全");
        }
        System.out.println("[AIService] 请求URL: " + chatUrl);
        System.out.println("[AIService] 协议: " + (chatUrl.startsWith("https") ? "HTTPS" : "HTTP"));

        // 构建请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("temperature", settingsManager.getTemperature());
        requestBody.addProperty("top_p", settingsManager.getTopP());

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        requestBody.add("messages", messages);

        String requestJson = requestBody.toString();
        System.out.println("[AIService] 请求体: " + requestJson.substring(0, Math.min(200, requestJson.length())) + "...");

        RequestBody body = RequestBody.create(
                requestJson,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(chatUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        System.out.println("[AIService] 发送POST请求...");
        sendRequestWithRetry(request, cacheKey, callback, 0);
    }

    /**
     * 带重试机制的请求发送
     */
    private void sendRequestWithRetry(Request request, String cacheKey, ChatCallback callback, int retryCount) {
        int maxRetry = settingsManager.getMaxRetry();
        System.out.println("[AIService] 尝试次数: " + (retryCount + 1) + "/" + (maxRetry + 1));

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("[AIService] 请求失败(尝试 " + (retryCount + 1) + "): " + e.getMessage());
                if (retryCount < maxRetry) {
                    System.out.println("[AIService] 准备重试...");
                    sendRequestWithRetry(request, cacheKey, callback, retryCount + 1);
                } else {
                    System.err.println("[AIService] 达到最大重试次数，放弃");
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("[AIService] 收到响应(尝试 " + (retryCount + 1) + "): HTTP " + response.code());
                try {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "无响应体";
                        System.err.println("[AIService] 请求失败: HTTP " + response.code());
                        System.err.println("[AIService] 错误详情: " + errorBody);
                        
                        if (retryCount < maxRetry) {
                            System.out.println("[AIService] 准备重试...");
                            sendRequestWithRetry(request, cacheKey, callback, retryCount + 1);
                        } else {
                            System.err.println("[AIService] 达到最大重试次数，放弃");
                            callback.onError("请求失败: HTTP " + response.code() + " - " + response.message());
                        }
                        return;
                    }

                    String responseBody = response.body().string();
                    System.out.println("[AIService] 响应体长度: " + responseBody.length() + " 字符");
                    System.out.println("[AIService] 响应内容: " + responseBody.substring(0, Math.min(300, responseBody.length())) + "...");
                    
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                    String content = null;
                    // 兼容 OpenAI 标准格式
                    if (jsonResponse.has("choices")) {
                        JsonArray choices = jsonResponse.getAsJsonArray("choices");
                        if (choices.size() > 0) {
                            JsonObject firstChoice = choices.get(0).getAsJsonObject();
                            if (firstChoice.has("message")) {
                                JsonObject message = firstChoice.getAsJsonObject("message");
                                if (message.has("content")) {
                                    content = message.get("content").getAsString();
                                }
                            }
                        }
                    }
                    // 兼容 Claude/通义千问等模型的格式
                    else if (jsonResponse.has("content")) {
                        JsonArray contentArray = jsonResponse.getAsJsonArray("content");
                        if (contentArray.size() > 0) {
                            JsonObject firstContent = contentArray.get(0).getAsJsonObject();
                            if (firstContent.has("text")) {
                                content = firstContent.get("text").getAsString();
                            }
                        }
                    }

                    if (content != null) {
                        System.out.println("[AIService] 成功获取AI响应，长度: " + content.length() + " 字符");
                        cacheManager.saveCache(cacheKey, content);
                        callback.onSuccess(content);
                    } else {
                        System.err.println("[AIService] 响应格式错误: 无法解析 'choices' 或 'content' 字段");
                        callback.onError("响应格式错误，无法解析AI回答");
                    }
                } catch (Exception e) {
                    System.err.println("[AIService] 解析响应失败: " + e.getMessage());
                    e.printStackTrace();
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }
        });
    }
}