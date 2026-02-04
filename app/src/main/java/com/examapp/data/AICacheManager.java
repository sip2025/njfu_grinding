package com.examapp.data;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
public class AICacheManager {
private static AICacheManager instance;
private SharedPreferences preferences;
private Gson gson;
private static final String PREF_NAME = "ai_cache";
private static final String KEY_CACHE_MAP = "cache_map";
private Map<String, String> cacheMap;
private AICacheManager(Context context) {
preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
gson = new Gson();
loadCache();
}
public static synchronized AICacheManager getInstance(Context context) {
if (instance == null) {
instance = new AICacheManager(context.getApplicationContext());
}
return instance;
}
private void loadCache() {
String json = preferences.getString(KEY_CACHE_MAP, "{}");
Type type = new TypeToken<Map<String, String>>(){}.getType();
cacheMap = gson.fromJson(json, type);
if (cacheMap == null) {
cacheMap = new HashMap<>();
}
}
private void saveCache() {
String json = gson.toJson(cacheMap);
preferences.edit().putString(KEY_CACHE_MAP, json).apply();
}
private String generateKey(String questionText, String answer) {
return String.valueOf((questionText + answer).hashCode());
}
public String getCachedResponse(String questionText, String answer) {
String key = generateKey(questionText, answer);
return cacheMap.get(key);
}
public void cacheResponse(String questionText, String answer, String response) {
String key = generateKey(questionText, answer);
cacheMap.put(key, response);
saveCache();
}
public boolean hasCachedResponse(String questionText, String answer) {
String key = generateKey(questionText, answer);
return cacheMap.containsKey(key);
}
public void clearCache() {
cacheMap.clear();
saveCache();
}
public int getCacheSize() {
return cacheMap.size();
}
}