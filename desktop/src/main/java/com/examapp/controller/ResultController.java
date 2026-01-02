package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.QuestionManager;
import com.examapp.model.ExamHistoryEntry;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ResultController {

    @FXML private Label subjectNameLabel;
    @FXML private Label scoreLabel;
    @FXML private Label detailsLabel;
    @FXML private Label accuracyLabel;
    @FXML private Button backButton;
    @FXML private Button reviewWrongButton;
    @FXML private Button reviewAllButton;

    private ExamHistoryEntry historyEntry;

    public void setHistoryEntry(ExamHistoryEntry historyEntry) {
        this.historyEntry = historyEntry;
        displayResults();
    }

    private void displayResults() {
        if (historyEntry == null) return;

        subjectNameLabel.setText(historyEntry.getSubjectName() + " - 考试结果");
        scoreLabel.setText(String.format("%d / %d", historyEntry.getScore(), historyEntry.getMaxScore()));
        
        detailsLabel.setText(String.format("答对: %d | 答错: %d | 未答: %d",
                historyEntry.getCorrectCount(),
                historyEntry.getWrongCount(),
                historyEntry.getTotalQuestions() - historyEntry.getAnsweredQuestions()));
        
        accuracyLabel.setText(String.format("正确率: %.1f%%", historyEntry.getScoreRate()));
   }

   @FXML
    private void handleBack() {
        try {
            Main.switchScene("/fxml/main.fxml", "主菜单");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   @FXML
   private void handleReviewWrong() {
       if (historyEntry == null) return;

       Subject subject = QuestionManager.getInstance().getSubjectById(historyEntry.getSubjectId());
       if (subject == null) return;

       QuestionManager qm = QuestionManager.getInstance();
       List<Question> wrongQuestions = historyEntry.getQuestionRecords().stream()
           .filter(record -> !record.isCorrect())
           .map(record -> {
               Question fullQuestion = qm.getQuestionById(record.getQuestionId());
               if (fullQuestion != null) {
                   Question q = new Question();
                   q.setId(fullQuestion.getId());
                   q.setQuestionText(fullQuestion.getQuestionText());
                   q.setAnswer(fullQuestion.getAnswer());
                   q.setOptions(fullQuestion.getOptions());
                   q.setType(fullQuestion.getType());
                   q.setExplanation(fullQuestion.getExplanation());
                   q.setUserAnswer(record.getUserAnswer());
                   return q;
               }
               return null;
           })
           .filter(q -> q != null)
           .collect(Collectors.toList());

       try {
           Main.switchSceneWithData("/fxml/review.fxml", "错题回顾 - " + historyEntry.getSubjectName(), controller -> {
               if (controller instanceof ReviewController) {
                   ((ReviewController) controller).setReviewData(subject, wrongQuestions, true, "result");
               }
           });
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

   @FXML
   private void handleReviewAll() {
       if (historyEntry == null) return;

       // Find the subject object
       Subject subject = QuestionManager.getInstance().getSubjectById(historyEntry.getSubjectId());
       if (subject == null) {
           return;
       }

       // Get all questions from the exam history and restore full Question objects
       QuestionManager qm = QuestionManager.getInstance();
       List<Question> allQuestions = historyEntry.getQuestionRecords().stream()
           .map(record -> {
               // Get the full Question object from QuestionManager
               Question fullQuestion = qm.getQuestionById(record.getQuestionId());
               if (fullQuestion != null) {
                   // Create a copy to avoid modifying the original
                   Question q = new Question();
                   q.setId(fullQuestion.getId());
                   q.setQuestionText(fullQuestion.getQuestionText());
                   q.setAnswer(fullQuestion.getAnswer());
                   q.setOptions(fullQuestion.getOptions());
                   q.setType(fullQuestion.getType());
                   q.setExplanation(fullQuestion.getExplanation());
                   q.setUserAnswer(record.getUserAnswer());
                   return q;
               }
               return null;
           })
           .filter(q -> q != null)
           .collect(Collectors.toList());

       try {
           Main.switchSceneWithData("/fxml/review.fxml", "试卷回顾 - " + historyEntry.getSubjectName(), controller -> {
               if (controller instanceof ReviewController) {
                   ((ReviewController) controller).setReviewData(subject, allQuestions, true, "result");
               }
           });
       } catch (IOException e) {
           e.printStackTrace();
       }
   }
}