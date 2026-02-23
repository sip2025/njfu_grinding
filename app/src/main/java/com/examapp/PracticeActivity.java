package com.examapp;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.examapp.data.QuestionManager;
import com.examapp.data.AISettingsManager;
import com.examapp.data.AICacheManager;
import com.examapp.service.AIService;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.util.DraggableFABHelper;
import com.examapp.util.GestureGuideHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.airbnb.lottie.LottieAnimationView;
import io.noties.markwon.Markwon;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.examapp.adapter.SubjectExpandableAdapter;
import com.google.android.material.navigation.NavigationView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.examapp.adapter.SimilarQuestionsAdapter;

public class PracticeActivity extends BaseActivity implements GestureDetector.OnGestureListener {

    public static final String EXTRA_MODE = "practice_mode";
    public static final String MODE_SEQUENTIAL = "mode_sequential";
    public static final String MODE_RANDOM = "mode_random";
    public static final String MODE_REVIEW = "mode_review";
    public static final String MODE_WRONG_REVIEW = "mode_wrong_review";

    private QuestionManager questionManager;
    private AISettingsManager aiSettingsManager;
    private AICacheManager aiCacheManager;
    private AIService aiService;
    private Markwon markwon;

    private Subject subject;
    private List<Question> questions;
    private List<Question> baseQuestions;
    private int currentPosition;
    private List<Integer> questionHistory = new ArrayList<>();
    private String subjectId;

    private boolean isReviewMode;
    private boolean isWrongReviewMode;
    private boolean isRandomOrder;

    private GestureDetectorCompat gestureDetector;
    private boolean isBindingQuestion;

    private FloatingActionButton aiAssistantButton;
    private DraggableFABHelper draggableFABHelper;
    private GestureGuideHelper gestureGuide;
    private LottieAnimationView feedbackAnimation;
    private boolean isFavorited = false;

    private TextView questionNumberView;
    private TextView questionTextView;
    private RadioGroup optionsGroup;
    private Button nextButton;
    private Button previousButton;
    private Button favoriteButton;
    private LinearLayout feedbackLayout;
    private TextView feedbackTextView;
    private DrawerLayout drawerLayout;
    private RecyclerView questionNavRecyclerView;
    private SubjectExpandableAdapter subjectExpandableAdapter;
    private ImageButton typeMenuButton;
    private ScrollView practiceScrollView;
    private Map<String, Boolean> expansionState = new LinkedHashMap<>();
    private LinearLayout randomModeSidebar;
    private View legendView;
    private TextView scrollPercentageText;

    private float drawerGestureStartX;
    private boolean drawerGestureEligible;
    private static final int OPEN_THRESHOLD_PX = 60;
    private String currentFilterKeyword = null;

    private float gestureStartY = 0;
    private float gestureStartScrollPercent = 0;
    private boolean isLoadingSimilarQuestions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        questionManager = QuestionManager.getInstance(this);
        aiSettingsManager = AISettingsManager.getInstance(this);
        aiCacheManager = AICacheManager.getInstance(this);
        aiService = AIService.getInstance(this);
        markwon = Markwon.create(this);

        subjectId = getIntent().getStringExtra(StudyModeActivity.EXTRA_SUBJECT_ID);
        subject = questionManager.getSubject(subjectId);

        String presetMode = getIntent().getStringExtra(EXTRA_MODE);
        if (presetMode == null && getIntent().getBooleanExtra("review_mode", false)) {
            presetMode = MODE_REVIEW;
        }

        gestureDetector = new GestureDetectorCompat(this, this);

        initializeUI();
        initGestureGuide();

        if (presetMode == null) {
            showModeSelectionDialog();
        } else {
            applyMode(presetMode);
            loadQuestions();
            displayCurrentQuestion();
        }
    }

    private void initializeUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        drawerLayout = findViewById(R.id.practice_drawer_layout);
        questionNavRecyclerView = findViewById(R.id.question_nav_recycler_view);
        questionNavRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        randomModeSidebar = findViewById(R.id.random_mode_sidebar);
        legendView = findViewById(R.id.legend_view);
        scrollPercentageText = findViewById(R.id.scroll_percentage_text);
        typeMenuButton = findViewById(R.id.type_menu_button);

        practiceScrollView = findViewById(R.id.practice_scroll_view);
        questionNumberView = findViewById(R.id.question_number);
        questionTextView = findViewById(R.id.question_text);
        optionsGroup = findViewById(R.id.options_group);
        nextButton = findViewById(R.id.next_button);
        previousButton = findViewById(R.id.previous_button);
        favoriteButton = findViewById(R.id.favorite_button);
        feedbackLayout = findViewById(R.id.feedback_layout);
        feedbackTextView = findViewById(R.id.feedback_text);
        feedbackAnimation = findViewById(R.id.feedback_animation);
        aiAssistantButton = findViewById(R.id.ai_assistant_button);

        draggableFABHelper = new DraggableFABHelper();
        draggableFABHelper.makeDraggable(aiAssistantButton, v -> showAIDialog());

        nextButton.setOnClickListener(v -> moveToNextQuestion());
        previousButton.setOnClickListener(v -> moveToPreviousQuestion());
        favoriteButton.setOnClickListener(v -> toggleWrongQuestion());

        findViewById(R.id.btn_single_choice).setOnClickListener(v -> filterQuestionsByType("单选题"));
        findViewById(R.id.btn_multiple_choice).setOnClickListener(v -> filterQuestionsByType("多选题"));
        findViewById(R.id.btn_true_false).setOnClickListener(v -> filterQuestionsByType("判断题"));
        findViewById(R.id.btn_mixed).setOnClickListener(v -> filterQuestionsByType(null));

        typeMenuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        drawerLayout.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    drawerGestureStartX = event.getX();
                    drawerGestureEligible = drawerGestureStartX < v.getWidth() / 2 && !drawerLayout.isDrawerOpen(GravityCompat.START);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (drawerGestureEligible) {
                        float diff = event.getX() - drawerGestureStartX;
                        if (diff > OPEN_THRESHOLD_PX) {
                            drawerLayout.openDrawer(GravityCompat.START);
                            drawerGestureEligible = false;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    drawerGestureEligible = false;
                    break;
            }
            return false;
        });

        practiceScrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    gestureStartY = event.getY();
                    gestureStartScrollPercent = getScrollPercentage();
                    break;
                    
                case MotionEvent.ACTION_UP:
                    float endY = event.getY();
                    float endScrollPercent = getScrollPercentage();
                    
                    // Check if user swiped up from 75% area to 20% area
                    if (gestureStartScrollPercent >= 0.75f && endScrollPercent <= 0.20f &&
                        gestureStartY - endY > 100 && !isLoadingSimilarQuestions) {
                        showSimilarQuestionsDialog();
                        return true;
                    }
                    break;
            }
            return false;
        });

        optionsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (questions != null && !questions.isEmpty()) {
                Question currentQuestion = questions.get(currentPosition);
                boolean isMultipleChoice = "多选题".equals(currentQuestion.getType());
                if (!isBindingQuestion && checkedId != -1 && !isReviewMode && !isMultipleChoice) {
                    evaluateCurrentAnswer();
                }
            }
        });

        updateSidebarButtonStyles(null);
    }

    private void showModeSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择学习模式");
        String[] modes = {"顺序刷题", "随机刷题", "背题模式"};
        builder.setItems(modes, (dialog, which) -> {
            if (which == 1) {
                applyMode(MODE_RANDOM);
            } else if (which == 2) {
                applyMode(MODE_REVIEW);
            } else {
                applyMode(MODE_SEQUENTIAL);
            }
            loadQuestions();
            displayCurrentQuestion();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void applyMode(String mode) {
        isRandomOrder = MODE_RANDOM.equals(mode) || MODE_WRONG_REVIEW.equals(mode);
        isReviewMode = MODE_REVIEW.equals(mode);
        isWrongReviewMode = MODE_WRONG_REVIEW.equals(mode);

        if (MODE_RANDOM.equals(mode)) {
            questionNavRecyclerView.setVisibility(View.GONE);
            randomModeSidebar.setVisibility(View.VISIBLE);
            legendView.setVisibility(View.GONE);
            questionHistory.clear();
            filterQuestionsByType(null);
            updateSidebarButtonStyles(null);
        } else if (MODE_WRONG_REVIEW.equals(mode)) {
            questionNavRecyclerView.setVisibility(View.VISIBLE);
            randomModeSidebar.setVisibility(View.GONE);
            legendView.setVisibility(View.VISIBLE);
        } else {
            questionNavRecyclerView.setVisibility(View.VISIBLE);
            randomModeSidebar.setVisibility(View.GONE);
            legendView.setVisibility(isReviewMode ? View.VISIBLE : View.GONE);
        }
    }

    private void loadQuestions() {
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        boolean shouldReset = MODE_RANDOM.equals(mode) || MODE_WRONG_REVIEW.equals(mode);

        if (MODE_WRONG_REVIEW.equals(mode)) {
            baseQuestions = questionManager.getWrongQuestions(subjectId);
            if (baseQuestions.isEmpty()) {
                Toast.makeText(this, "错题本是空的！", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            questions = questionManager.getClonedQuestions(baseQuestions);
            for (Question q : questions) {
                q.setUserAnswer(null);
                q.setAnswerState(Question.AnswerState.UNANSWERED);
            }
            Collections.shuffle(questions);
            currentPosition = 0;
        } else if (subject != null) {
            baseQuestions = subject.getQuestions() != null ? new ArrayList<>(subject.getQuestions()) : new ArrayList<>();
            questions = questionManager.getClonedQuestions(baseQuestions);

            if (isRandomOrder) {
                for (Question q : questions) {
                    q.setUserAnswer(null);
                    q.setAnswerState(Question.AnswerState.UNANSWERED);
                }
                Collections.shuffle(questions);
                currentPosition = 0;
            } else {
                if (MODE_SEQUENTIAL.equals(mode)) {
                    currentPosition = Math.max(0, subject.getSequentialLastPosition());
                } else if (MODE_REVIEW.equals(mode)) {
                    currentPosition = Math.max(0, subject.getReviewLastPosition());
                } else {
                    currentPosition = 0;
                }
            }
        } else {
            baseQuestions = new ArrayList<>();
            questions = new ArrayList<>();
            currentPosition = 0;
        }
    }

    private void displayCurrentQuestion() {
        if (questions == null || questions.isEmpty()) {
            Toast.makeText(this, "没有题目", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isRandomOrder) {
            if (!questionHistory.contains(currentPosition)) {
                questionHistory.add(currentPosition);
            }
        }

        Question question = questions.get(currentPosition);
        String typeText = question.getType() != null ? question.getType() : "未知";

        if (isRandomOrder) {
            questionNumberView.setText(String.format("【%s】", typeText));
        } else {
            int[] typeInfo = getTypeSpecificQuestionInfo(question);
            questionNumberView.setText(String.format("【%s】 第 %d / %d 题", typeText, typeInfo[0], typeInfo[1]));
        }

        questionTextView.setText(question.getQuestionText());
        optionsGroup.removeAllViews();
        feedbackLayout.setVisibility(LinearLayout.GONE);

        updateFavoriteButtonLabel(question);
        updateFavoriteButton();

        if (isReviewMode) {
            displayReviewMode(question);
        } else {
            displayPracticeMode(question);
        }

        if (isRandomOrder) {
            previousButton.setEnabled(questionHistory.size() > 1);
            nextButton.setEnabled(true);
        } else {
            previousButton.setEnabled(currentPosition > 0);
            nextButton.setEnabled(currentPosition < questions.size() - 1);
        }

        updateQuestionNavigationDrawer();

        // Reset scroll position to top
        practiceScrollView.scrollTo(0, 0);
    }

    private void updateQuestionNavigationDrawer() {
        if (subjectExpandableAdapter == null) {
            List<Question> questionsForNav = isWrongReviewMode ? questions : baseQuestions;
            List<Object> items = groupQuestionsByType(questionsForNav);
            boolean useWrongCountColors = isReviewMode || isWrongReviewMode;
            subjectExpandableAdapter = new SubjectExpandableAdapter(items, new HashMap<>(), currentPosition, position -> {
                currentPosition = position;
                displayCurrentQuestion();
                drawerLayout.closeDrawer(GravityCompat.START);
            }, useWrongCountColors, questionsForNav);

            GridLayoutManager layoutManager = new GridLayoutManager(this, 5);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return subjectExpandableAdapter.getItemViewType(position) == SubjectExpandableAdapter.TYPE_HEADER ? 5 : 1;
                }
            });

            questionNavRecyclerView.setLayoutManager(layoutManager);
            questionNavRecyclerView.setAdapter(subjectExpandableAdapter);

            questionNavRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    updateScrollPercentage();
                }

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        scrollPercentageText.postDelayed(() -> {
                            if (scrollPercentageText != null) {
                                scrollPercentageText.setVisibility(View.GONE);
                            }
                        }, 1500);
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        if (scrollPercentageText != null) {
                            scrollPercentageText.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });

            drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerOpened(View drawerView) {
                    scrollToCurrentQuestion();
                    updateScrollPercentage();
                    if (scrollPercentageText != null) {
                        scrollPercentageText.setVisibility(View.VISIBLE);
                        scrollPercentageText.postDelayed(() -> {
                            if (scrollPercentageText != null) {
                                scrollPercentageText.setVisibility(View.GONE);
                            }
                        }, 2000);
                    }
                }
            });

        } else {
            subjectExpandableAdapter.setCurrentQuestionIndex(currentPosition);
            subjectExpandableAdapter.notifyDataSetChanged();
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                scrollToCurrentQuestion();
            }
        }
    }

    private void scrollToCurrentQuestion() {
        if (subjectExpandableAdapter != null && questionNavRecyclerView != null) {
            int adapterPosition = findAdapterPositionForQuestion(currentPosition);
            if (adapterPosition >= 0) {
                questionNavRecyclerView.scrollToPosition(adapterPosition);
            }
        }
    }

    private void updateScrollPercentage() {
        if (scrollPercentageText == null || questionNavRecyclerView == null) return;
        int offset = questionNavRecyclerView.computeVerticalScrollOffset();
        int range = questionNavRecyclerView.computeVerticalScrollRange() - questionNavRecyclerView.computeVerticalScrollExtent();
        int extent = questionNavRecyclerView.computeVerticalScrollExtent();

        if (range > 0) {
            int percentage = (int) ((offset * 100.0f) / range);
            percentage = Math.max(0, Math.min(100, percentage));
            scrollPercentageText.setText(percentage + "%");

            float scrollRatio = (float) offset / range;
            int recyclerHeight = questionNavRecyclerView.getHeight();
            int textHeight = scrollPercentageText.getHeight();
            if (textHeight == 0) textHeight = 30;
            int maxY = recyclerHeight - textHeight - 16;
            int targetY = (int) (scrollRatio * maxY);
            targetY = Math.max(8, Math.min(targetY, maxY));

            scrollPercentageText.setTranslationY(targetY);
        } else {
            scrollPercentageText.setText("0%");
            scrollPercentageText.setTranslationY(8);
        }
    }

    private int findAdapterPositionForQuestion(int questionIndex) {
        if (subjectExpandableAdapter == null) return -1;
        // This is a simplified lookup, assuming expanded state.
        // A more robust implementation would iterate the adapter items.
        // For now, rely on adapter's internal logic or simple iteration if needed.
        // Since we don't have direct mapping, we iterate through adapter items
        for (int i = 0; i < subjectExpandableAdapter.getItemCount(); i++) {
            if (subjectExpandableAdapter.getItemViewType(i) == SubjectExpandableAdapter.TYPE_QUESTION) {
                // Check if this item matches
            }
        }

        // Fallback:
        List<Question> questionsForNav = isWrongReviewMode ? questions : baseQuestions;
        int targetIndex = isWrongReviewMode ? questionIndex : questionIndex;
        int position = 0;
        String currentType = null;

        for (int i = 0; i <= targetIndex && i < questionsForNav.size(); i++) {
             Question q = questionsForNav.get(i);
             if (!q.getType().equals(currentType)) {
                 position++; // Header
                 currentType = q.getType();
             }
             if (i == targetIndex) {
                 return position;
             }
             position++;
        }
        return position > 0 ? position : 0;
    }

    private List<Object> groupQuestionsByType(List<Question> questions) {
        List<Object> items = new ArrayList<>();
        Map<String, List<Question>> groupedQuestions = new LinkedHashMap<>();
        groupedQuestions.put("单选题", new ArrayList<>());
        groupedQuestions.put("多选题", new ArrayList<>());
        groupedQuestions.put("判断题", new ArrayList<>());

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            q.setIndex(i);
            if ("多选题".equals(q.getType())) {
                groupedQuestions.get("多选题").add(q);
            } else if ("判断题".equals(q.getType())) {
                groupedQuestions.get("判断题").add(q);
            } else {
                groupedQuestions.get("单选题").add(q);
            }
        }

        for (Map.Entry<String, List<Question>> entry : groupedQuestions.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                items.add(entry.getKey());
                int relativeIdCounter = 1;
                for (Question q : entry.getValue()) {
                    q.setRelativeId(relativeIdCounter++);
                }
                items.addAll(entry.getValue());
            }
        }
        return items;
    }

    private boolean isTrueFalseQuestion(Question q) {
        String cat = q.getCategory() != null ? q.getCategory().toLowerCase() : "";
        String type = q.getType() != null ? q.getType().toLowerCase() : cat;
        return type.contains("判断") || type.contains("true") || type.contains("false");
    }

    private void displayPracticeMode(Question question) {
        isBindingQuestion = true;
        optionsGroup.removeAllViews();
        List<String> opts = question.getOptions();
        boolean isMultipleChoice = "多选题".equals(question.getType());

        if ((opts == null || opts.isEmpty()) && isTrueFalseQuestion(question)) {
            opts = new ArrayList<>();
            opts.add("A. " + getString(R.string.option_true));
            opts.add("B. " + getString(R.string.option_false));
        }

        if (opts != null) {
            if (isMultipleChoice) {
                for (String option : opts) {
                    CheckBox cb = new CheckBox(this);
                    cb.setText(option);
                    cb.setTextSize(16);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    lp.setMargins(0, 16, 0, 16);
                    cb.setLayoutParams(lp);
                    optionsGroup.addView(cb);
                }
                Button submitButton = new Button(this);
                submitButton.setText("确认答案");
                submitButton.setOnClickListener(v -> evaluateCurrentAnswer());
                optionsGroup.addView(submitButton);
            } else {
                for (String option : opts) {
                    RadioButton rb = new RadioButton(this);
                    rb.setText(option);
                    rb.setTextSize(16);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    lp.setMargins(0, 16, 0, 16);
                    rb.setLayoutParams(lp);
                    optionsGroup.addView(rb);
                }
            }
        }

        if (question.getUserAnswer() != null) {
            String userAnswer = question.getUserAnswer();
            for (int i = 0; i < optionsGroup.getChildCount(); i++) {
                android.view.View child = optionsGroup.getChildAt(i);
                if (isMultipleChoice && child instanceof CheckBox) {
                    CheckBox cb = (CheckBox) child;
                    String optionLetter = cb.getText().toString().substring(0, 1);
                    if (userAnswer.contains(optionLetter)) {
                        cb.setChecked(true);
                    }
                } else if (!isMultipleChoice && child instanceof RadioButton) {
                    RadioButton rb = (RadioButton) child;
                    if (rb.getText().toString().startsWith(userAnswer)) {
                        rb.setChecked(true);
                    }
                }
            }
        } else {
            if (!isMultipleChoice) {
                optionsGroup.clearCheck();
            }
        }
        isBindingQuestion = false;
    }

    private void displayReviewMode(Question question) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        List<String> opts = question.getOptions();
        if ((opts == null || opts.isEmpty()) && isTrueFalseQuestion(question)) {
            opts = new ArrayList<>();
            opts.add("A. " + getString(R.string.option_true));
            opts.add("B. " + getString(R.string.option_false));
        }

        if (opts != null) {
            String answer = question.getAnswer();
            boolean isMultipleChoice = "多选题".equals(question.getType());
            boolean isTrueFalse = "判断题".equals(question.getType());
            String normalizedAnswer = answer;

            if (isTrueFalse && answer != null) {
                if ("正确".equals(answer) || "对".equals(answer) || "true".equalsIgnoreCase(answer)) {
                    normalizedAnswer = "A";
                } else if ("错误".equals(answer) || "错".equals(answer) || "false".equalsIgnoreCase(answer)) {
                    normalizedAnswer = "B";
                }
            }

            for (int i = 0; i < opts.size(); i++) {
                String option = opts.get(i);
                String letter = String.valueOf((char) ('A' + i));
                TextView tv = new TextView(this);
                tv.setText(option);
                tv.setTextSize(16);
                tv.setPadding(16, 8, 16, 8);

                boolean isCorrectOption = false;
                if (normalizedAnswer != null) {
                    if (isMultipleChoice) {
                        isCorrectOption = normalizedAnswer.contains(letter);
                    } else {
                        isCorrectOption = letter.equals(normalizedAnswer);
                    }
                }

                if (isCorrectOption) {
                    tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    tv.setTextColor(getColor(R.color.success));
                } else {
                    tv.setTextColor(getColor(R.color.black));
                }
                container.addView(tv);
            }
        }

        if (question.getExplanation() != null && !question.getExplanation().isEmpty()) {
            TextView explanationTitle = new TextView(this);
            explanationTitle.setText("解析:");
            explanationTitle.setTextSize(16);
            explanationTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            explanationTitle.setPadding(16, 16, 16, 8);
            container.addView(explanationTitle);

            TextView explanationText = new TextView(this);
            explanationText.setText(question.getExplanation());
            explanationText.setTextSize(14);
            explanationText.setPadding(16, 0, 16, 16);
            container.addView(explanationText);
        }

        optionsGroup.addView(container);
    }

    private void updateFavoriteButtonLabel(Question question) {
        favoriteButton.setText(question.isWrong() ? R.string.unstar : R.string.star);
    }

    private void evaluateCurrentAnswer() {
        Question question = questions.get(currentPosition);
        if (question == null) return;

        String userAnswer = "";
        boolean isMultipleChoice = "多选题".equals(question.getType());

        if (isMultipleChoice) {
            StringBuilder userAnswerBuilder = new StringBuilder();
            for (int i = 0; i < optionsGroup.getChildCount(); i++) {
                android.view.View child = optionsGroup.getChildAt(i);
                if (child instanceof CheckBox && ((CheckBox) child).isChecked()) {
                    String optionText = ((CheckBox) child).getText().toString();
                    userAnswerBuilder.append(optionText.substring(0, 1));
                }
            }
            userAnswer = userAnswerBuilder.toString();
        } else {
            if (optionsGroup.getCheckedRadioButtonId() != -1) {
                RadioButton selected = findViewById(optionsGroup.getCheckedRadioButtonId());
                String selectedText = selected.getText().toString();
                if (!selectedText.isEmpty()) {
                    userAnswer = selectedText.substring(0, 1);
                }
            }
        }

        if (userAnswer.isEmpty()) {
            if (isMultipleChoice) {
                Toast.makeText(this, "请选择答案", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String prevAnswer = question.getUserAnswer();
        question.setUserAnswer(userAnswer);
        boolean isCorrect = question.isAnsweredCorrectly();
        question.setAnswerState(isCorrect ? Question.AnswerState.CORRECT : Question.AnswerState.WRONG);

        int originalIndex = getOriginalQuestionIndex(currentPosition);
        Question originalQuestion = findOriginalQuestion(question);

        if (prevAnswer == null || !prevAnswer.equals(userAnswer)) {
            if (originalIndex >= 0) {
                questionManager.recordAnswer(subjectId, originalIndex, userAnswer, isCorrect);
            }
        }

        if (subjectExpandableAdapter != null) {
            subjectExpandableAdapter.notifyDataSetChanged();
        }

        feedbackLayout.setVisibility(LinearLayout.VISIBLE);
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        boolean isWrongReviewMode = MODE_WRONG_REVIEW.equals(mode);

        if (isCorrect) {
            feedbackTextView.setText("✓ 正确!");
            feedbackTextView.setTextColor(getColor(R.color.success));
            showCorrectAnimation();
            if (originalQuestion != null && originalQuestion.isWrong() && !isWrongReviewMode && originalIndex >= 0) {
                originalQuestion.setWrong(false);
                question.setWrong(false);
                questionManager.removeWrongQuestion(subjectId, originalIndex);
                updateFavoriteButtonLabel(question);
            }
            optionsGroup.postDelayed(this::moveToNextQuestion, 400);
        } else {
            feedbackTextView.setText("✗ 错误! 正确答案是: " + question.getFormattedAnswer());
            feedbackTextView.setTextColor(getColor(R.color.error));
            showWrongAnimation();
            if (originalQuestion != null && !originalQuestion.isWrong() && !isWrongReviewMode && originalIndex >= 0) {
                originalQuestion.setWrong(true);
                question.setWrong(true);
                questionManager.addWrongQuestion(subjectId, originalIndex);
                updateFavoriteButtonLabel(question);
            }
            if (question.getExplanation() != null && !question.getExplanation().isEmpty()) {
                TextView explanationTitle = new TextView(this);
                explanationTitle.setText("解析:");
                explanationTitle.setTextSize(16);
                explanationTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                explanationTitle.setPadding(16, 16, 16, 8);
                container.addView(explanationTitle);

                TextView explanationText = new TextView(this);
                explanationText.setText(question.getExplanation());
                explanationText.setTextSize(14);
                explanationText.setPadding(16, 0, 16, 16);
                container.addView(explanationText);
            }

            optionsGroup.addView(container);
        }
    }

    private void toggleWrongQuestion() {
        if (questions == null || questions.isEmpty()) return;
        Question currentQuestion = questions.get(currentPosition);
        int originalIndex = getOriginalQuestionIndex(currentPosition);

        if (originalIndex < 0) return;

        Question questionToUpdate = findOriginalQuestion(currentQuestion);
        if (questionToUpdate == null) return;

        boolean isCurrentlyWrong = questionToUpdate.isWrong();
        if (isCurrentlyWrong) {
            questionManager.removeWrongQuestion(subjectId, originalIndex);
            Toast.makeText(this, R.string.star_removed, Toast.LENGTH_SHORT).show();
        } else {
            questionManager.addWrongQuestion(subjectId, originalIndex);
            Toast.makeText(this, R.string.star_added, Toast.LENGTH_SHORT).show();
        }

        questionToUpdate.setWrong(!isCurrentlyWrong);
        currentQuestion.setWrong(!isCurrentlyWrong);
        updateFavoriteButtonLabel(currentQuestion);
    }

    private void moveToNextQuestion() {
        if (questions == null || questions.isEmpty()) return;

        if (isRandomOrder) {
            if (questions.size() > 1) {
                int nextPosition = new java.util.Random().nextInt(questions.size());
                while (nextPosition == currentPosition) {
                    nextPosition = new java.util.Random().nextInt(questions.size());
                }
                currentPosition = nextPosition;
            }
            displayCurrentQuestion();
        } else {
            if (currentPosition < questions.size() - 1) {
                currentPosition++;
                saveProgress();
                displayCurrentQuestion();
            }
        }
    }

    private void moveToPreviousQuestion() {
        if (isRandomOrder) {
            if (questionHistory.size() > 1) {
                questionHistory.remove(questionHistory.size() - 1);
                currentPosition = questionHistory.get(questionHistory.size() - 1);
                displayCurrentQuestion();
            }
        } else {
            if (questions == null || questions.isEmpty()) return;
            if (currentPosition > 0) {
                currentPosition--;
                saveProgress();
                displayCurrentQuestion();
            }
        }
    }

    private int getOriginalQuestionIndex(int position) {
        if (subject == null || subject.getQuestions() == null || questions == null || position < 0 || position >= questions.size()) {
            return -1;
        }
        Question question = questions.get(position);
        String questionId = question.getId();
        List<Question> originalQuestions = subject.getQuestions();

        for (int i = 0; i < originalQuestions.size(); i++) {
            Question q = originalQuestions.get(i);
            if (questionId != null && questionId.equals(q.getId())) {
                return i;
            }
        }
        return -1;
    }

    private Question findOriginalQuestion(Question clonedQuestion) {
        if (subject == null || subject.getQuestions() == null || clonedQuestion == null) {
            return null;
        }
        String questionId = clonedQuestion.getId();
        for (Question q : subject.getQuestions()) {
            if (questionId != null && questionId.equals(q.getId())) {
                return q;
            }
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) { return true; }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) { return false; }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 != null && e2 != null) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (diffX > 100) {
                    moveToPreviousQuestion();
                    return true;
                } else if (diffX < -100) {
                    moveToNextQuestion();
                    return true;
                }
            }
        }
        return false;
    }
    
    private float getScrollPercentage() {
        View view = (View) practiceScrollView.getChildAt(practiceScrollView.getChildCount() - 1);
        int scrollY = practiceScrollView.getScrollY();
        int scrollableHeight = view.getHeight() - practiceScrollView.getHeight();
        
        if (scrollableHeight <= 0) return 0f;
        return (float) scrollY / scrollableHeight;
    }

    private void showSimilarQuestionsDialog() {
        if (questions == null || questions.isEmpty()) return;
        Question currentQuestion = questions.get(currentPosition);

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
            List<Question> similarQuestions = questionManager.findSimilarQuestions(subjectId, currentQuestion);
            runOnUiThread(() -> {
                isLoadingSimilarQuestions = false;
                progressBar.setVisibility(View.GONE);

                if (similarQuestions.isEmpty()) {
                    tvNoSimilar.setVisibility(View.VISIBLE);
                } else {
                    rvSimilar.setVisibility(View.VISIBLE);
                    rvSimilar.setLayoutManager(new LinearLayoutManager(PracticeActivity.this));
                    SimilarQuestionsAdapter adapter = new SimilarQuestionsAdapter(similarQuestions);
                    rvSimilar.setAdapter(adapter);
                }
            });
        }).start();

        dialog.setOnDismissListener(d -> isLoadingSimilarQuestions = false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gestureGuide != null) {
            gestureGuide.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 🔧 Clean up GestureGuideHelper to prevent memory leaks and crashes
        if (gestureGuide != null) {
            gestureGuide.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 🔧 Save guide state when configuration changes (e.g., screen rotation)
        if (gestureGuide != null && gestureGuide.isShowing()) {
            outState.putBoolean("guide_showing", true);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 🔧 Restore guide state after configuration change
        if (savedInstanceState != null && savedInstanceState.getBoolean("guide_showing", false)) {
            // Delay to ensure UI is fully initialized
            findViewById(android.R.id.content).postDelayed(() -> {
                if (gestureGuide != null && gestureGuide.shouldShowGuide()) {
                    gestureGuide.showGuide();
                }
            }, 300);
        }
    }

    private void saveProgress() {
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (MODE_SEQUENTIAL.equals(mode) || MODE_REVIEW.equals(mode)) {
            int positionToSave = getOriginalQuestionIndex(currentPosition);
            if (MODE_SEQUENTIAL.equals(mode)) {
                questionManager.updateSequentialProgress(subjectId, positionToSave);
            } else {
                questionManager.updateReviewProgress(subjectId, positionToSave);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private int[] getTypeSpecificQuestionInfo(Question currentQuestion) {
        if (baseQuestions == null || currentQuestion.getType() == null) {
            return new int[]{currentPosition + 1, questions.size()};
        }
        String type = currentQuestion.getType();
        int typeTotal = 0;
        for (Question q : baseQuestions) {
            if (type.equals(q.getType())) {
                typeTotal++;
            }
        }
        int typeCurrentIndex = currentQuestion.getRelativeId();
        if (typeCurrentIndex <= 0) {
            int calculatedIndex = 0;
            for (Question q : baseQuestions) {
                if (type.equals(q.getType())) {
                    calculatedIndex++;
                    if (q.equals(currentQuestion)) {
                        typeCurrentIndex = calculatedIndex;
                        break;
                    }
                }
            }
        }
        return new int[]{typeCurrentIndex, typeTotal};
    }

    private void filterQuestionsByType(String type) {
        if (baseQuestions == null) return;
        currentFilterKeyword = type;
        List<Question> filteredBase = new ArrayList<>();
        if (type == null) {
            filteredBase.addAll(baseQuestions);
        } else {
            for (Question q : baseQuestions) {
                if (type.equals(q.getType())) {
                    filteredBase.add(q);
                }
            }
        }

        if (filteredBase.isEmpty()) {
            Toast.makeText(this, "该类型下没有题目", Toast.LENGTH_SHORT).show();
            return;
        }

        questions = questionManager.getClonedQuestions(filteredBase);
        for (Question q : questions) {
            q.setUserAnswer(null);
            q.setAnswerState(Question.AnswerState.UNANSWERED);
        }
        Collections.shuffle(questions);
        currentPosition = 0;
        questionHistory.clear();
        displayCurrentQuestion();
        updateSidebarButtonStyles(type);
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void updateSidebarButtonStyles(String activeType) {
        Button btnSingle = findViewById(R.id.btn_single_choice);
        Button btnMultiple = findViewById(R.id.btn_multiple_choice);
        Button btnTrueFalse = findViewById(R.id.btn_true_false);
        Button btnMixed = findViewById(R.id.btn_mixed);

        btnSingle.setBackgroundColor(    "单选题".equals(activeType) ? getColor(R.color.primary) : Color.GRAY);
        btnMultiple.setBackgroundColor(  "多选题".equals(activeType) ? getColor(R.color.primary) : Color.GRAY);
        btnTrueFalse.setBackgroundColor( "判断题".equals(activeType) ? getColor(R.color.primary) : Color.GRAY);
        btnMixed.setBackgroundColor(     activeType == null       ? getColor(R.color.primary) : Color.GRAY);
    }

    private void showAIDialog() {
        showAIDialog(false);
    }

    private void showAIDialog(boolean forceRefresh) {
        if (!aiSettingsManager.isConfigured()) {
            Toast.makeText(this, R.string.ai_not_configured, Toast.LENGTH_LONG).show();
            return;
        }
        if (questions == null || questions.isEmpty() || currentPosition >= questions.size()) {
            Toast.makeText(this, "无法获取当前题目", Toast.LENGTH_SHORT).show();
            return;
        }
        Question currentQuestion = questions.get(currentPosition);

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

        boolean hasCached = aiCacheManager.hasCachedResponse(
                currentQuestion.getQuestionText(),
                currentQuestion.getFormattedAnswer()
        );

        refreshButton.setOnClickListener(v -> {
            loadAIResponse(currentQuestion, progressBar, thinkingText, answerText, errorText, true);
        });

        closeButton.setOnClickListener(v -> dialog.dismiss());

        loadAIResponse(currentQuestion, progressBar, thinkingText, answerText, errorText, forceRefresh);
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

    private void initGestureGuide() {
        gestureGuide = new GestureGuideHelper(this);
        findViewById(android.R.id.content).postDelayed(() -> {
            if (gestureGuide.shouldShowGuide()) {
                gestureGuide.showGuide();
            }
        }, 500);
    }

    private void updateFavoriteButton() {
        Question currentQuestion = questions.get(currentPosition);
        // boolean isInWrongSet = questionManager.isInWrongSet(subject.getId(), currentQuestion); // Assuming method exists or use logic
        // But previously it just checked local questions list, let's stick to logic in toggle
        // Actually earlier code used updateFavoriteButton but there is no such method in QuestionManager exposed usually for "isInWrongSet"
        // Wait, earlier read file didn't show `isInWrongSet`. Let's use `currentQuestion.isWrong()` which is set.
        MaterialButton favBtn = (MaterialButton) favoriteButton;
        if (currentQuestion.isWrong()) {
            favBtn.setIcon(getDrawable(R.drawable.ic_star_filled));
        } else {
            favBtn.setIcon(getDrawable(R.drawable.ic_star_outline));
        }
    }

    private void showCorrectAnimation() {
        if (feedbackAnimation != null) {
            feedbackAnimation.setVisibility(View.VISIBLE);
            feedbackAnimation.setAnimation(R.raw.success_animation);
            feedbackAnimation.setRepeatCount(0);
            feedbackAnimation.playAnimation();
            feedbackAnimation.postDelayed(() -> {
                if (feedbackAnimation != null) {
                    feedbackAnimation.setVisibility(View.GONE);
                }
            }, 3000);
        }
    }

    private void showWrongAnimation() {
        if (feedbackAnimation != null) {
            feedbackAnimation.setVisibility(View.VISIBLE);
            feedbackAnimation.setAnimation(R.raw.error_animation);
            feedbackAnimation.setRepeatCount(0);
            feedbackAnimation.playAnimation();
            feedbackAnimation.postDelayed(() -> {
                if (feedbackAnimation != null) {
                    feedbackAnimation.setVisibility(View.GONE);
                }
            }, 3000);
        }
    }
}
