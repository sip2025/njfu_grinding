package com.examapp.data;
import android.content.Context;
import android.content.SharedPreferences;
import com.examapp.model.Question;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class MockExamCacheManager {
private static MockExamCacheManager instance;
private SharedPreferences preferences;
private Gson gson;
private static final String PREF_NAME = "mock_exam_cache";
private static final String KEY_HAS_CACHE = "has_cache";
private static final String KEY_SUBJECT_ID = "subject_id";
private static final String KEY_SUBJECT_NAME = "subject_name";
private static final String KEY_QUESTIONS = "questions";
private static final String KEY_ANSWERS = "answers";
private static final String KEY_CURRENT_POSITION = "current_position";
private static final String KEY_TIMESTAMP = "timestamp";
private MockExamCacheManager(Context context) {
preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
gson = new Gson();
}
public static synchronized MockExamCacheManager getInstance(Context context) {
if (instance == null) {
instance = new MockExamCacheManager(context.getApplicationContext());
}
return instance;
}
public boolean hasCachedExam(String subjectId) {
if (!preferences.getBoolean(KEY_HAS_CACHE, false)) {
return false;
}
String cachedSubjectId = preferences.getString(KEY_SUBJECT_ID, null);
return subjectId != null && subjectId.equals(cachedSubjectId);
}
public void saveExamState(String subjectId, String subjectName,
List<Question> questions, Map<Integer, String> answers,
int currentPosition) {
SharedPreferences.Editor editor = preferences.edit();
editor.putBoolean(KEY_HAS_CACHE, true);
editor.putString(KEY_SUBJECT_ID, subjectId);
editor.putString(KEY_SUBJECT_NAME, subjectName);
editor.putString(KEY_QUESTIONS, gson.toJson(questions));
editor.putString(KEY_ANSWERS, gson.toJson(answers));
editor.putInt(KEY_CURRENT_POSITION, currentPosition);
editor.putLong(KEY_TIMESTAMP, System.currentTimeMillis());
editor.apply();
}
public String getCachedSubjectName() {
return preferences.getString(KEY_SUBJECT_NAME, null);
}
public List<Question> getCachedQuestions() {
String json = preferences.getString(KEY_QUESTIONS, null);
if (json == null) {
return new ArrayList<>();
}
Type type = new TypeToken<List<Question>>(){}.getType();
List<Question> questions = gson.fromJson(json, type);
return questions != null ? questions : new ArrayList<>();
}
public Map<Integer, String> getCachedAnswers() {
String json = preferences.getString(KEY_ANSWERS, null);
if (json == null) {
return new HashMap<>();
}
Type type = new TypeToken<Map<Integer, String>>(){}.getType();
Map<Integer, String> answers = gson.fromJson(json, type);
return answers != null ? answers : new HashMap<>();
}
public int getCachedPosition() {
return preferences.getInt(KEY_CURRENT_POSITION, 0);
}
public long getCachedTimestamp() {
return preferences.getLong(KEY_TIMESTAMP, 0);
}
public int getAnsweredCount() {
return getCachedAnswers().size();
}
public int getTotalCount() {
return getCachedQuestions().size();
}
public void clearCache() {
SharedPreferences.Editor editor = preferences.edit();
editor.clear();
editor.apply();
}
public String getFormattedCacheTime() {
long timestamp = getCachedTimestamp();
if (timestamp == 0) {
return "未知时间";
}
java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault());
return sdf.format(new java.util.Date(timestamp));
}
}