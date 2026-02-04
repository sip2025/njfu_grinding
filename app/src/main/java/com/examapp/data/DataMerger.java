package com.examapp.data;
import com.examapp.model.ExamHistoryEntry;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.model.SyncData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class DataMerger {
private Gson gson = new Gson();
public Map<String, Subject> mergeSubjects(String localJson, String remoteJson) {
Type type = new TypeToken<Map<String, Subject>>() {}.getType();
Map<String, Subject> localSubjects = gson.fromJson(localJson, type);
Map<String, Subject> remoteSubjects = gson.fromJson(remoteJson, type);
if (localSubjects == null) localSubjects = new HashMap<>();
if (remoteSubjects == null) remoteSubjects = new HashMap<>();
Map<String, Subject> mergedSubjects = new HashMap<>(localSubjects);
for (Map.Entry<String, Subject> remoteEntry : remoteSubjects.entrySet()) {
String key = remoteEntry.getKey();
Subject remoteSubject = remoteEntry.getValue();
Subject localSubject = mergedSubjects.get(key);
if (localSubject == null) {
mergedSubjects.put(key, remoteSubject);
} else {
Subject mergedSubject = mergeSingleSubject(localSubject, remoteSubject);
mergedSubjects.put(key, mergedSubject);
}
}
return mergedSubjects;
}
private Subject mergeSingleSubject(Subject local, Subject remote) {
Subject merged = new Subject(local.getId(), local.getDisplayName());
merged.setEndlessBestStreak(Math.max(local.getEndlessBestStreak(), remote.getEndlessBestStreak()));
merged.setSequentialLastPosition(Math.max(local.getSequentialLastPosition(), remote.getSequentialLastPosition()));
merged.setReviewLastPosition(Math.max(local.getReviewLastPosition(), remote.getReviewLastPosition()));
Map<String, Question> localQuestionsMap = local.getQuestions().stream()
.collect(Collectors.toMap(Question::getId, Function.identity()));
List<Question> mergedQuestions = new ArrayList<>();
for (Question remoteQuestion : remote.getQuestions()) {
Question localQuestion = localQuestionsMap.get(remoteQuestion.getId());
if (localQuestion != null) {
Question mergedQuestion = new Question(localQuestion);
mergedQuestion.setWrongAnswerCount(localQuestion.getWrongAnswerCount() + remoteQuestion.getWrongAnswerCount());
mergedQuestion.setWrong(localQuestion.isWrong() || remoteQuestion.isWrong());
if (localQuestion.getAnswerState() == Question.AnswerState.UNANSWERED) {
mergedQuestion.setAnswerState(remoteQuestion.getAnswerState());
mergedQuestion.setUserAnswer(remoteQuestion.getUserAnswer());
}
mergedQuestions.add(mergedQuestion);
localQuestionsMap.remove(remoteQuestion.getId());
} else {
mergedQuestions.add(remoteQuestion);
}
}
mergedQuestions.addAll(localQuestionsMap.values());
merged.setQuestions(mergedQuestions);
merged.recalculateStats();
merged.setLastModified(System.currentTimeMillis());
return merged;
}
public List<ExamHistoryEntry> mergeExamHistory(String localJson, String remoteJson) {
Type type = new TypeToken<List<ExamHistoryEntry>>() {}.getType();
List<ExamHistoryEntry> localHistory = gson.fromJson(localJson, type);
List<ExamHistoryEntry> remoteHistory = gson.fromJson(remoteJson, type);
if (localHistory == null) localHistory = new ArrayList<>();
if (remoteHistory == null) remoteHistory = new ArrayList<>();
List<ExamHistoryEntry> mergedHistory = new ArrayList<>(localHistory);
mergedHistory.addAll(remoteHistory);
Set<String> uniqueKeys = new HashSet<>();
List<ExamHistoryEntry> distinctHistory = new ArrayList<>();
for (ExamHistoryEntry entry : mergedHistory) {
String key = entry.getTimestamp() + "-" + entry.getDeviceSource();
if (uniqueKeys.add(key)) {
distinctHistory.add(entry);
}
}
distinctHistory.sort((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));
return distinctHistory;
}
public SyncData merge(SyncData localData, SyncData remoteData) {
List<Subject> mergedSubjects = remoteData.getSubjects();
List<ExamHistoryEntry> mergedHistory = remoteData.getExamHistory();
return new SyncData(mergedSubjects, mergedHistory);
}
}