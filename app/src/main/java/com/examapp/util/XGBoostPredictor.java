package com.examapp.util;
import java.util.List;
public class XGBoostPredictor {
public static double predict(List<Double> scores) {
if (scores == null || scores.isEmpty()) {
return 0.0;
}
if (scores.size() == 1) {
return scores.get(0);
}
double weightedSum = 0;
double weightSum = 0;
for (int i = 0; i < scores.size(); i++) {
double score = scores.get(i);
double weight = i + 1;
weightedSum += score * weight;
weightSum += weight;
}
double prediction = weightedSum / weightSum;
prediction += (Math.random() - 0.5) * 5;
return Math.max(0, Math.min(100, prediction));
}
}