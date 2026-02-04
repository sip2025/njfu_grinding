package com.examapp.service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.examapp.MainActivity;
import com.examapp.R;
import com.examapp.data.AISettingsManager;
import com.examapp.data.QuestionManager;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class AIProcessingService extends Service {
private static final String CHANNEL_ID = "ai_processing_channel";
private static final int NOTIFICATION_ID = 1001;
public static final String EXTRA_SUBJECT_ID = "subject_id";
public static final String EXTRA_FIX_QUESTIONS = "fix_questions";
public static final String EXTRA_GENERATE_EXPLANATIONS = "generate_explanations";
public static final String EXTRA_CONCURRENCY = "concurrency";
private AISettingsManager aiSettingsManager;
private QuestionManager questionManager;
private OkHttpClient client;
private Handler mainHandler;
private ExecutorService executorService;
private Semaphore semaphore;
private String subjectId;
private boolean fixQuestions;
private boolean generateExplanations;
private int concurrency;
private AtomicInteger totalQuestions = new AtomicInteger(0);
private AtomicInteger questionsNeedingFix = new AtomicInteger(0);
private AtomicInteger processedQuestions = new AtomicInteger(0);
private AtomicInteger successCount = new AtomicInteger(0);
private AtomicInteger failureCount = new AtomicInteger(0);
private List<Question> questionsToProcess = new ArrayList<>();
@Override
public void onCreate() {
super.onCreate();
aiSettingsManager = AISettingsManager.getInstance(this);
questionManager = QuestionManager.getInstance(this);
mainHandler = new Handler(Looper.getMainLooper());
client = new OkHttpClient.Builder()
.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
.readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
.writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
.build();
createNotificationChannel();
}
@Override
public int onStartCommand(Intent intent, int flags, int startId) {
if (intent == null) {
stopSelf();
return START_NOT_STICKY;
}
subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID);
fixQuestions = intent.getBooleanExtra(EXTRA_FIX_QUESTIONS, false);
generateExplanations = intent.getBooleanExtra(EXTRA_GENERATE_EXPLANATIONS, false);
concurrency = intent.getIntExtra(EXTRA_CONCURRENCY, 5);
startForeground(NOTIFICATION_ID, createNotification("准备处理...", 0, 0));
startProcessing();
return START_NOT_STICKY;
}
private void startProcessing() {
Subject subject = questionManager.getSubject(subjectId);
if (subject == null || subject.getQuestions() == null) {
stopSelf();
return;
}
List<Question> allQuestions = subject.getQuestions();
totalQuestions.set(allQuestions.size());
List<Question> bracketFixList = new ArrayList<>();
if (fixQuestions) {
for (Question question : allQuestions) {
String type = question.getType();
if (("单选题".equals(type) || "多选题".equals(type)) &&
needsBracketFix(question.getQuestionText())) {
bracketFixList.add(question);
}
}
String detectMessage = String.format("检测完成: 共%d道题,需修复括号%d道",
totalQuestions.get(), bracketFixList.size());
updateNotification(detectMessage, 0, totalQuestions.get());
}
boolean actuallyFixBrackets = false;
boolean actuallyGenerateExplanations = generateExplanations;
if (fixQuestions && generateExplanations) {
if (!bracketFixList.isEmpty()) {
questionsToProcess.addAll(allQuestions);
actuallyFixBrackets = true;
} else {
questionsToProcess.addAll(allQuestions);
actuallyFixBrackets = false;
}
} else if (fixQuestions && !bracketFixList.isEmpty()) {
questionsToProcess.addAll(bracketFixList);
actuallyFixBrackets = true;
} else if (generateExplanations) {
questionsToProcess.addAll(allQuestions);
actuallyFixBrackets = false;
}
questionsNeedingFix.set(questionsToProcess.size());
if (questionsToProcess.isEmpty()) {
onProcessingComplete();
return;
}
final boolean finalFixBrackets = actuallyFixBrackets;
final boolean finalGenerateExplanations = actuallyGenerateExplanations;
executorService = Executors.newFixedThreadPool(concurrency);
semaphore = new Semaphore(concurrency);
for (int i = 0; i < questionsToProcess.size(); i++) {
final int index = i;
final Question question = questionsToProcess.get(i);
executorService.execute(() -> {
try {
semaphore.acquire();
processQuestion(question, index, finalFixBrackets, finalGenerateExplanations);
} catch (InterruptedException e) {
e.printStackTrace();
onQuestionProcessed(false);
} finally {
semaphore.release();
}
});
}
executorService.shutdown();
}
private boolean needsBracketFix(String questionText) {
if (questionText.contains("( )") || questionText.contains("()") ||
questionText.contains("（ ）") || questionText.contains("（）") ||
questionText.contains("(　)") || questionText.contains("（　）")) {
return false;
}
return true;
}
private void processQuestion(Question question, int index, boolean doFixBrackets, boolean doGenerateExplanations) {
String type = question.getType();
boolean needsFix = doFixBrackets &&
("单选题".equals(type) || "多选题".equals(type)) &&
needsBracketFix(question.getQuestionText());
if (needsFix) {
fixQuestionBrackets(question, () -> {
if (doGenerateExplanations) {
generateExplanation(question, () -> onQuestionProcessed(true));
} else {
onQuestionProcessed(true);
}
});
} else if (doGenerateExplanations) {
generateExplanation(question, () -> onQuestionProcessed(true));
} else {
onQuestionProcessed(true);
}
}
private void fixQuestionBrackets(Question question, Runnable onComplete) {
fixQuestionBracketsWithRetry(question, onComplete, 0, false);
}
private void fixQuestionBracketsWithRetry(Question question, Runnable onComplete,
int retryCount, boolean useStrictPrompt) {
int maxRetry = aiSettingsManager.getMaxRetry();
if (retryCount >= maxRetry) {
android.util.Log.e("AIProcessingService",
String.format("括号修复失败,已达到最大重试次数(%d次): %s",
maxRetry, question.getQuestionText()));
onComplete.run();
return;
}
String questionText = question.getQuestionText();
String prompt;
if (useStrictPrompt) {
prompt = aiSettingsManager.getFixBracketStrictPrompt();
} else {
prompt = aiSettingsManager.getFixBracketPrompt();
}
prompt = prompt.replace("{question}", questionText)
.replace("{answer}", question.getFormattedAnswer());
callAI(prompt, new AICallback() {
@Override
public void onSuccess(String response) {
String fixedText = response.trim();
ValidationResult validation = validateBracketFix(questionText, fixedText);
if (validation.isValid) {
question.setQuestionText(fixedText);
questionManager.updateQuestion(subjectId, question);
onComplete.run();
} else {
android.util.Log.w("AIProcessingService",
String.format("括号修复验证失败(重试%d/%d次): %s. 原因: %s",
retryCount + 1, maxRetry, questionText, validation.reason));
boolean nextUseStrict = retryCount >= 2 || useStrictPrompt;
mainHandler.postDelayed(() -> {
fixQuestionBracketsWithRetry(question, onComplete,
retryCount + 1, nextUseStrict);
}, 1000);
}
}
@Override
public void onError(String error) {
android.util.Log.e("AIProcessingService",
String.format("AI调用失败(重试%d/%d次): %s", retryCount + 1, maxRetry, error));
mainHandler.postDelayed(() -> {
fixQuestionBracketsWithRetry(question, onComplete,
retryCount + 1, useStrictPrompt);
}, 2000);
}
});
}
private ValidationResult validateBracketFix(String original, String fixed) {
int originalBracketCount = countBrackets(original);
int fixedBracketCount = countBrackets(fixed);
int addedBrackets = fixedBracketCount - originalBracketCount;
if (addedBrackets == 0) {
return new ValidationResult(false, "未添加括号");
}
if (addedBrackets > 1) {
return new ValidationResult(false,
String.format("添加了%d个括号,超过1个", addedBrackets));
}
String originalWithoutBrackets = original.replace("(", "").replace(")", "")
.replace("（", "").replace("）", "")
.replace(" ", "").trim();
String fixedWithoutBrackets = fixed.replace("(", "").replace(")", "")
.replace("（", "").replace("）", "")
.replace(" ", "").trim();
if (!originalWithoutBrackets.equals(fixedWithoutBrackets)) {
return new ValidationResult(false, "修改了题目的其他内容");
}
if (!fixed.contains("( )") && !fixed.contains("()") &&
!fixed.contains("（ ）") && !fixed.contains("（）") &&
!fixed.contains("(　)") && !fixed.contains("（　）")) {
return new ValidationResult(false, "括号格式不正确");
}
return new ValidationResult(true, "验证通过");
}
private int countBrackets(String text) {
int count = 0;
if (text.contains("( )")) {
count += text.split("\\(\\s\\)", -1).length - 1;
}
if (text.contains("()")) {
count += text.split("\\(\\)", -1).length - 1;
}
if (text.contains("（ ）")) {
count += text.split("（\\s）", -1).length - 1;
}
if (text.contains("（）")) {
count += text.split("（）", -1).length - 1;
}
if (text.contains("(　)")) {
count += text.split("\\(　\\)", -1).length - 1;
}
if (text.contains("（　）")) {
count += text.split("（　）", -1).length - 1;
}
return count;
}
private static class ValidationResult {
boolean isValid;
String reason;
ValidationResult(boolean isValid, String reason) {
this.isValid = isValid;
this.reason = reason;
}
}
private void generateExplanation(Question question, Runnable onComplete) {
StringBuilder optionsText = new StringBuilder();
List<String> options = question.getOptions();
if (options != null && !options.isEmpty()) {
for (String option : options) {
optionsText.append(option).append("\n");
}
}
String prompt = aiSettingsManager.getAnswerPrompt()
.replace("{question}", question.getQuestionText())
.replace("{options}", optionsText.toString())
.replace("{answer}", question.getFormattedAnswer());
callAI(prompt, new AICallback() {
@Override
public void onSuccess(String response) {
com.examapp.data.AICacheManager.getInstance(AIProcessingService.this)
.cacheResponse(question.getQuestionText(),
question.getFormattedAnswer(),
response.trim());
onComplete.run();
}
@Override
public void onError(String error) {
onComplete.run();
}
});
}
private void callAI(String prompt, AICallback callback) {
String url = aiSettingsManager.getFullApiUrl();
String apiKey = aiSettingsManager.getApiKey();
String model = aiSettingsManager.getModel();
try {
JSONObject requestJson = new JSONObject();
requestJson.put("model", model);
requestJson.put("temperature", 0.7);
requestJson.put("top_p", 0.9);
JSONArray messages = new JSONArray();
JSONObject message = new JSONObject();
message.put("role", "user");
message.put("content", prompt);
messages.put(message);
requestJson.put("messages", messages);
RequestBody body = RequestBody.create(
requestJson.toString(),
MediaType.parse("application/json; charset=utf-8")
);
Request request = new Request.Builder()
.url(url)
.addHeader("Authorization", "Bearer " + apiKey)
.addHeader("Content-Type", "application/json")
.post(body)
.build();
client.newCall(request).enqueue(new Callback() {
@Override
public void onFailure(Call call, IOException e) {
mainHandler.post(() -> callback.onError(e.getMessage()));
}
@Override
public void onResponse(Call call, Response response) throws IOException {
try {
if (!response.isSuccessful()) {
mainHandler.post(() -> callback.onError("API错误: " + response.code()));
return;
}
String responseBody = response.body().string();
JSONObject jsonResponse = new JSONObject(responseBody);
JSONArray choices = jsonResponse.getJSONArray("choices");
if (choices.length() > 0) {
JSONObject firstChoice = choices.getJSONObject(0);
JSONObject messageObj = firstChoice.getJSONObject("message");
String content = messageObj.getString("content");
mainHandler.post(() -> callback.onSuccess(content));
} else {
mainHandler.post(() -> callback.onError("AI返回了空响应"));
}
} catch (Exception e) {
mainHandler.post(() -> callback.onError("解析响应失败: " + e.getMessage()));
}
}
});
} catch (Exception e) {
mainHandler.post(() -> callback.onError("构建请求失败: " + e.getMessage()));
}
}
private void onQuestionProcessed(boolean success) {
if (success) {
successCount.incrementAndGet();
}
int successfulCount = successCount.get();
int total = questionsNeedingFix.get();
String message = String.format("处理中... %d/%d",
successfulCount, total);
updateNotification(message, successfulCount, total);
if (successfulCount >= total) {
onProcessingComplete();
}
}
private void onProcessingComplete() {
String message = String.format("处理完成! 成功修复:%d道题目",
successCount.get());
updateNotification(message, questionsNeedingFix.get(), questionsNeedingFix.get());
mainHandler.postDelayed(() -> stopSelf(), 3000);
}
private void createNotificationChannel() {
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
NotificationChannel channel = new NotificationChannel(
CHANNEL_ID,
"AI处理服务",
NotificationManager.IMPORTANCE_LOW
);
channel.setDescription("后台处理题目修复和解析生成");
NotificationManager manager = getSystemService(NotificationManager.class);
manager.createNotificationChannel(channel);
}
}
private Notification createNotification(String message, int progress, int max) {
Intent intent = new Intent(this, MainActivity.class);
PendingIntent pendingIntent = PendingIntent.getActivity(
this, 0, intent,
Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
PendingIntent.FLAG_IMMUTABLE : 0
);
NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
.setContentTitle("AI题库处理")
.setContentText(message)
.setSmallIcon(R.drawable.ic_ai_assistant)
.setContentIntent(pendingIntent)
.setOngoing(true)
.setPriority(NotificationCompat.PRIORITY_LOW);
if (max > 0) {
builder.setProgress(max, progress, false);
}
return builder.build();
}
private void updateNotification(String message, int progress, int max) {
NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
manager.notify(NOTIFICATION_ID, createNotification(message, progress, max));
}
@Override
public IBinder onBind(Intent intent) {
return null;
}
@Override
public void onDestroy() {
super.onDestroy();
if (executorService != null && !executorService.isShutdown()) {
executorService.shutdownNow();
try {
if (!executorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
android.util.Log.w("AIProcessingService", "线程池未能在2秒内完全关闭");
}
} catch (InterruptedException e) {
android.util.Log.e("AIProcessingService", "等待线程池关闭时被中断", e);
executorService.shutdownNow();
Thread.currentThread().interrupt();
}
}
if (client != null) {
client.dispatcher().cancelAll();
}
android.util.Log.i("AIProcessingService", "AI处理服务已停止");
}
private interface AICallback {
void onSuccess(String response);
void onError(String error);
}
}