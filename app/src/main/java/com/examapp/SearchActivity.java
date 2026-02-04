package com.examapp;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.examapp.adapter.QuestionAdapter;
import com.examapp.adapter.SimilarQuestionsAdapter;
import com.examapp.data.QuestionManager;
import com.examapp.data.AISettingsManager;
import com.examapp.data.AICacheManager;
import com.examapp.service.AIService;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import io.noties.markwon.Markwon;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class SearchActivity extends BaseActivity implements QuestionAdapter.OnQuestionClickListener {
private QuestionManager questionManager;
private AISettingsManager aiSettingsManager;
private AICacheManager aiCacheManager;
private AIService aiService;
private Markwon markwon;
private EditText searchInput;
private RecyclerView searchResultsRecyclerView;
private LinearLayout emptyStateLayout;
private QuestionAdapter questionAdapter;
private List<Question> allQuestions;
private List<Question> filteredQuestions;
private String subjectId;
private boolean isLoadingSimilarQuestions = false;
@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
setContentView(R.layout.activity_search);
questionManager = QuestionManager.getInstance(this);
aiSettingsManager = AISettingsManager.getInstance(this);
aiCacheManager = AICacheManager.getInstance(this);
aiService = AIService.getInstance(this);
markwon = Markwon.create(this);
subjectId = getIntent().getStringExtra(StudyModeActivity.EXTRA_SUBJECT_ID);
initializeUI();
loadAllQuestions();
setupSearch();
}
private void initializeUI() {
Toolbar toolbar = findViewById(R.id.toolbar);
setSupportActionBar(toolbar);
getSupportActionBar().setDisplayHomeAsUpEnabled(true);
searchInput = findViewById(R.id.search_input);
searchResultsRecyclerView = findViewById(R.id.search_results_recycler_view);
searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
emptyStateLayout = findViewById(R.id.empty_state_layout);
}
private void loadAllQuestions() {
allQuestions = new ArrayList<>();
if (subjectId != null) {
Subject subject = questionManager.getSubject(subjectId);
if (subject != null && subject.getQuestions() != null) {
allQuestions.addAll(subject.getQuestions());
}
} else {
Map<String, Subject> subjects = questionManager.getAllSubjects();
for (Subject subject : subjects.values()) {
if (subject.getQuestions() != null) {
allQuestions.addAll(subject.getQuestions());
}
}
}
}
private void setupSearch() {
searchInput.addTextChangedListener(new TextWatcher() {
@Override
public void beforeTextChanged(CharSequence s, int start, int count, int after) {
}
@Override
public void onTextChanged(CharSequence s, int start, int before, int count) {
performSearch(s.toString());
}
@Override
public void afterTextChanged(Editable s) {
}
});
}
private void performSearch(String keyword) {
filteredQuestions = new ArrayList<>();
if (keyword.isEmpty()) {
showEmptyState();
return;
}
String lowerKeyword = keyword.toLowerCase();
for (Question question : allQuestions) {
if (question.getQuestionText().toLowerCase().contains(lowerKeyword)) {
filteredQuestions.add(question);
continue;
}
if (question.getOptions() != null) {
for (String option : question.getOptions()) {
if (option != null && option.toLowerCase().contains(lowerKeyword)) {
filteredQuestions.add(question);
break;
}
}
}
}
if (filteredQuestions.isEmpty()) {
showEmptyState();
} else {
searchResultsRecyclerView.setVisibility(View.VISIBLE);
emptyStateLayout.setVisibility(View.GONE);
questionAdapter = new QuestionAdapter(filteredQuestions, this);
searchResultsRecyclerView.setAdapter(questionAdapter);
}
}
private void showEmptyState() {
searchResultsRecyclerView.setVisibility(View.GONE);
emptyStateLayout.setVisibility(View.VISIBLE);
}
@Override
public void onQuestionClick(Question question) {
AlertDialog.Builder builder = new AlertDialog.Builder(this);
LayoutInflater inflater = this.getLayoutInflater();
View dialogView = inflater.inflate(R.layout.dialog_question_details, null);
builder.setView(dialogView);
TextView questionText = dialogView.findViewById(R.id.dialog_question_text);
TextView optionsText = dialogView.findViewById(R.id.dialog_options_text);
TextView userAnswerText = dialogView.findViewById(R.id.dialog_user_answer_text);
TextView correctAnswerText = dialogView.findViewById(R.id.dialog_correct_answer_text);
Button starButton = dialogView.findViewById(R.id.dialog_star_button);
Button aiButton = dialogView.findViewById(R.id.dialog_ai_button);
Button similarButton = dialogView.findViewById(R.id.dialog_similar_button);
questionText.setText(question.getQuestionText());
StringBuilder options = new StringBuilder();
if (question.getOptions() != null && !question.getOptions().isEmpty()) {
for (String option : question.getOptions()) {
options.append(option).append("\n");
}
}
optionsText.setText(options.toString());
userAnswerText.setVisibility(View.GONE);
correctAnswerText.setText("正确答案: " + question.getAnswer());
updateStarButton(starButton, question);
starButton.setOnClickListener(v -> {
question.setWrong(!question.isWrong());
questionManager.updateQuestionStarStatus(question);
updateStarButton(starButton, question);
});
if (aiSettingsManager.isConfigured()) {
    aiButton.setVisibility(View.VISIBLE);
    aiButton.setOnClickListener(v -> showAIDialog(question));
} else {
    aiButton.setVisibility(View.GONE);
}

similarButton.setOnClickListener(v -> showSimilarQuestionsDialog(question));

builder.setTitle("题目详情")
.setPositiveButton("关闭", null);
AlertDialog dialog = builder.create();
dialog.show();
}
private void updateStarButton(Button starButton, Question question) {
if (question.isWrong()) {
starButton.setText("取消星标");
} else {
starButton.setText("星标");
}
}
private void showAIDialog(Question question) {
showAIDialog(question, false);
}
private void showAIDialog(Question question, boolean forceRefresh) {
BottomSheetDialog dialog = new BottomSheetDialog(this);
View dialogView = getLayoutInflater().inflate(R.layout.dialog_ai_answer, null);
dialog.setContentView(dialogView);
ProgressBar progressBar = dialogView.findViewById(R.id.ai_progress_bar);
TextView thinkingText = dialogView.findViewById(R.id.ai_thinking_text);
TextView answerText = dialogView.findViewById(R.id.ai_answer_text);
TextView errorText = dialogView.findViewById(R.id.ai_error_text);
ImageView modelIcon = dialogView.findViewById(R.id.ai_model_icon);
TextView modelName = dialogView.findViewById(R.id.ai_model_name);
ImageButton refreshButton = dialogView.findViewById(R.id.ai_refresh_button);
Button closeButton = dialogView.findViewById(R.id.ai_close_button);
String model = aiSettingsManager.getModel();
modelName.setText(model != null && !model.isEmpty() ? model : "AI助手");
modelIcon.setImageResource(getModelIconResource(model));
refreshButton.setOnClickListener(v -> {
loadAIResponse(question, progressBar, thinkingText, answerText, errorText, true);
});
closeButton.setOnClickListener(v -> dialog.dismiss());
loadAIResponse(question, progressBar, thinkingText, answerText, errorText, forceRefresh);
dialog.show();
}
private void loadAIResponse(Question question, ProgressBar progressBar,
TextView thinkingText, TextView answerText,
TextView errorText, boolean forceRefresh) {
progressBar.setVisibility(View.VISIBLE);
thinkingText.setVisibility(View.VISIBLE);
answerText.setVisibility(View.GONE);
errorText.setVisibility(View.GONE);
aiService.askQuestion(question, new AIService.AICallback() {
@Override
public void onSuccess(String response) {
progressBar.setVisibility(View.GONE);
thinkingText.setVisibility(View.GONE);
answerText.setVisibility(View.VISIBLE);
markwon.setMarkdown(answerText, response);
}
@Override
public void onError(String error) {
progressBar.setVisibility(View.GONE);
thinkingText.setVisibility(View.GONE);
errorText.setVisibility(View.VISIBLE);
errorText.setText(getString(R.string.ai_error) + ": " + error);
}
}, forceRefresh);
}
private void showSimilarQuestionsDialog(Question currentQuestion) {
    if (isLoadingSimilarQuestions) return;
    
    String questionSubjectId = findSubjectIdForQuestion(currentQuestion);
    if (questionSubjectId == null) {
        Toast.makeText(this, "无法找到题目所属科目", Toast.LENGTH_SHORT).show();
        return;
    }

    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View view = getLayoutInflater().inflate(R.layout.layout_similar_questions_bottom_sheet, null);
    dialog.setContentView(view);

    ProgressBar progressBar = view.findViewById(R.id.progressBar);
    TextView tvNoSimilar = view.findViewById(R.id.tvNoSimilar);
    RecyclerView rvSimilar = view.findViewById(R.id.rvSimilarQuestions);

    progressBar.setVisibility(View.VISIBLE);
    tvNoSimilar.setVisibility(View.GONE);
    rvSimilar.setVisibility(View.GONE);
    dialog.show();

    isLoadingSimilarQuestions = true;

    new Thread(() -> {
        List<Question> similarQuestions = questionManager.findSimilarQuestions(questionSubjectId, currentQuestion);
        runOnUiThread(() -> {
            isLoadingSimilarQuestions = false;
            progressBar.setVisibility(View.GONE);

            if (similarQuestions.isEmpty()) {
                tvNoSimilar.setVisibility(View.VISIBLE);
            } else {
                rvSimilar.setVisibility(View.VISIBLE);
                rvSimilar.setLayoutManager(new LinearLayoutManager(SearchActivity.this));
                SimilarQuestionsAdapter adapter = new SimilarQuestionsAdapter(similarQuestions);
                rvSimilar.setAdapter(adapter);
            }
        });
    }).start();

    dialog.setOnDismissListener(d -> isLoadingSimilarQuestions = false);
}

private String findSubjectIdForQuestion(Question questionToFind) {
    if (subjectId != null) {
        return subjectId;
    }
    Map<String, Subject> subjects = questionManager.getAllSubjects();
    for (Map.Entry<String, Subject> entry : subjects.entrySet()) {
        if (entry.getValue().getQuestions() != null && entry.getValue().getQuestions().contains(questionToFind)) {
            return entry.getKey();
        }
    }
    return null;
}

private int getModelIconResource(String model) {
if (model == null || model.isEmpty()) {
return R.drawable.ic_ai_assistant;
}
String lowerModel = model.toLowerCase();
if (lowerModel.contains("gpt") || lowerModel.contains("openai")) {
return R.drawable.openai;
} else if (lowerModel.contains("gemini")) {
return R.drawable.gemini_color;
} else if (lowerModel.contains("claude")) {
return R.drawable.claude_color;
} else if (lowerModel.contains("deepseek")) {
return R.drawable.deepseek_color;
} else if (lowerModel.contains("glm") || lowerModel.contains("chatglm")) {
return R.drawable.chatglm_color;
} else if (lowerModel.contains("qwen")) {
return R.drawable.qwen_color;
} else if (lowerModel.contains("grok")) {
return R.drawable.grok;
} else if (lowerModel.contains("ollama")) {
return R.drawable.ollama;
} else {
return R.drawable.ic_ai_assistant;
}
}
@Override
public boolean onSupportNavigateUp() {
onBackPressed();
return true;
}
}