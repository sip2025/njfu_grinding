package com.examapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import com.examapp.util.BackgroundApplier;
import com.google.android.material.card.MaterialCardView;

public class StudyModeActivity extends BaseActivity {

    public static final String EXTRA_SUBJECT_ID = "com.examapp.SUBJECT_ID";
    public static final String EXTRA_SUBJECT_NAME = "com.examapp.SUBJECT_NAME";

    private String subjectId;
    private String subjectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_mode);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        subjectId = getIntent().getStringExtra(EXTRA_SUBJECT_ID);
        subjectName = getIntent().getStringExtra(EXTRA_SUBJECT_NAME);
        setTitle(subjectName);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        BackgroundApplier.apply(this);
        setupClickListeners();
    }

    private void setupClickListeners() {
        MaterialCardView studyModeCard = findViewById(R.id.study_mode_card);
        MaterialCardView sequentialPracticeCard = findViewById(R.id.sequential_practice_card);
        MaterialCardView randomPracticeCard = findViewById(R.id.random_practice_card);
        MaterialCardView endlessModeCard = findViewById(R.id.endless_mode_card);
        MaterialCardView wrongReviewCard = findViewById(R.id.wrong_review_card);
        MaterialCardView mockExamCard = findViewById(R.id.mock_exam_card);
        MaterialCardView wrongQuestionsCard = findViewById(R.id.wrong_questions_card);
        MaterialCardView searchCard = findViewById(R.id.search_card);
        MaterialCardView examHistoryCard = findViewById(R.id.exam_history_card);
        MaterialCardView wrongAnalysisCard = findViewById(R.id.wrong_analysis_card);

        if (studyModeCard != null) studyModeCard.setOnClickListener(v -> startStudyMode());
        if (sequentialPracticeCard != null) sequentialPracticeCard.setOnClickListener(v -> startPractice(false));
        if (randomPracticeCard != null) randomPracticeCard.setOnClickListener(v -> startPractice(true));
        if (endlessModeCard != null) endlessModeCard.setOnClickListener(v -> startEndlessMode());
        if (wrongReviewCard != null) wrongReviewCard.setOnClickListener(v -> startWrongReview());
        if (mockExamCard != null) mockExamCard.setOnClickListener(v -> startMockExam());
        if (wrongQuestionsCard != null) wrongQuestionsCard.setOnClickListener(v -> startWrongQuestions());
        if (searchCard != null) searchCard.setOnClickListener(v -> startSearch());
        if (examHistoryCard != null) examHistoryCard.setOnClickListener(v -> startHistory());
        if (wrongAnalysisCard != null) wrongAnalysisCard.setOnClickListener(v -> startWrongAnalysis());
    }

    private void startStudyMode() {
        Intent intent = new Intent(this, PracticeActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        intent.putExtra(PracticeActivity.EXTRA_MODE, PracticeActivity.MODE_REVIEW);
        startActivity(intent);
    }

    private void startPractice(boolean isRandom) {
        Intent intent = new Intent(this, PracticeActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        intent.putExtra(PracticeActivity.EXTRA_MODE, isRandom ? PracticeActivity.MODE_RANDOM : PracticeActivity.MODE_SEQUENTIAL);
        startActivity(intent);
    }

    private void startEndlessMode() {
        Intent intent = new Intent(this, EndlessModeActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        startActivity(intent);
    }

    private void startWrongQuestions() {
        Intent intent = new Intent(this, WrongQuestionsActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        startActivity(intent);
    }

    private void startWrongReview() {
        Intent intent = new Intent(this, PracticeActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        intent.putExtra(PracticeActivity.EXTRA_MODE, PracticeActivity.MODE_WRONG_REVIEW);
        startActivity(intent);
    }

    private void startSearch() {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        startActivity(intent);
    }

    private void startHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        startActivity(intent);
    }

    private void startMockExam() {
        Intent intent = new Intent(this, MockExamActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        startActivity(intent);
    }

    private void startWrongAnalysis() {
        Intent intent = new Intent(this, WrongAnalysisActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_study_mode, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_switch_subject) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
