package com.examapp.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.examapp.R;
import com.examapp.AISettingsActivity;
import com.examapp.data.AISettingsManager;

public class GestureGuideHelper {

    private static final String PREFS_NAME = "GestureGuide";
    private static final String KEY_GUIDE_COMPLETED = "gesture_guide_completed";
    private static final String KEY_CURRENT_STEP = "current_step";
    private static final String KEY_GUIDE_IN_PROGRESS = "guide_in_progress";

    private final Activity activity;
    private final SharedPreferences prefs;
    private View guideOverlay;
    private AISettingsManager aiSettingsManager;

    private int currentStep = 0;
    private static final int STEP_RIGHT_SWIPE = 0;
    private static final int STEP_LEFT_SWIPE = 1;
    private static final int STEP_MENU = 2;
    private static final int STEP_AI = 3;
    private static final int STEP_PULL_UP = 4;

    public GestureGuideHelper(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.aiSettingsManager = AISettingsManager.getInstance(activity);
    }

    public boolean shouldShowGuide() {
        return !prefs.getBoolean(KEY_GUIDE_COMPLETED, false);
    }

    // 🔧 Get current step from SharedPreferences
    private int getCurrentStep() {
        return prefs.getInt(KEY_CURRENT_STEP, STEP_RIGHT_SWIPE);
    }

    // 🔧 Save current step to SharedPreferences
    private void saveCurrentStep(int step) {
        prefs.edit()
            .putInt(KEY_CURRENT_STEP, step)
            .putBoolean(KEY_GUIDE_IN_PROGRESS, true)
            .apply();
    }

    public void showGuide() {
        if (!shouldShowGuide()) {
            return;
        }
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        LayoutInflater inflater = LayoutInflater.from(activity);
        guideOverlay = inflater.inflate(R.layout.gesture_guide_overlay, rootView, false);
        rootView.addView(guideOverlay);

        // 🔧 Restore previous progress if guide was in progress
        boolean isInProgress = prefs.getBoolean(KEY_GUIDE_IN_PROGRESS, false);
        if (isInProgress) {
            currentStep = getCurrentStep();
        } else {
            currentStep = STEP_RIGHT_SWIPE;
        }
        showStep(currentStep);

        guideOverlay.setVisibility(View.VISIBLE);

        // Prevent interaction with underlying activity
        guideOverlay.setClickable(true);
        guideOverlay.setFocusable(true);
    }

    private void showStep(int step) {
        if (guideOverlay == null) return;

        ImageView animationView = guideOverlay.findViewById(R.id.guide_animation);
        TextView titleView = guideOverlay.findViewById(R.id.guide_title);
        TextView descView = guideOverlay.findViewById(R.id.guide_description);
        Button nextButton = guideOverlay.findViewById(R.id.guide_next_button);
        Button actionButton = guideOverlay.findViewById(R.id.guide_action_button);

        actionButton.setVisibility(View.GONE);
        nextButton.setText("下一步");

        int animationRes = 0;

        switch (step) {
            case STEP_RIGHT_SWIPE:
                titleView.setText("手势操作：向右滑动");
                descView.setText("在屏幕上向右滑动，可以查看上一道题目。");
                animationRes = R.drawable.anim_swipe_right;
                break;
            case STEP_LEFT_SWIPE:
                titleView.setText("手势操作：向左滑动");
                descView.setText("在屏幕上向左滑动，可以查看下一道题目。");
                animationRes = R.drawable.anim_swipe_left;
                break;
            case STEP_MENU:
                titleView.setText("快速导航");
                descView.setText("点击左上角的菜单按钮，或从屏幕左边缘向右滑，打开题目列表。");
                animationRes = R.drawable.anim_drawer;
                break;
            case STEP_AI:
                titleView.setText("AI 答疑助手");
                if (aiSettingsManager.isConfigured()) {
                    descView.setText("点击右下角的 AI 按钮，获取题目解析和答疑。");
                    actionButton.setVisibility(View.VISIBLE);
                    actionButton.setText("试一试");
                    actionButton.setOnClickListener(v -> {
                        // Ideally trigger AI dialog, but for guide flow just proceed
                        hideGuide(); // Let user actually click the real button
                        // Or better, finish guide and point to button.
                        // For this requirements: "If configured, guide user to click AI button"
                        // Since this is an overlay blocking input, we simulate or instruct.
                        // We will mark guide as partly done or just proceed to next step?
                        // "If user has configured... guide user to click AI button, view analysis, then look at other questions."
                        // Let's assume Next goes to next step, Action allows interaction?
                        // The requirement says "guide user to click that button".
                        // We'll proceed to next step for now to keep flow linear as requested "Right->Left->Menu->AI->PullUp".
                        // Wait, requirement says "If no key -> jump to settings. If returned -> continue guide."
                        nextStep();
                    });
                } else {
                    descView.setText("您尚未配置 AI API Key。点击下方按钮前往设置。");
                    actionButton.setVisibility(View.VISIBLE);
                    actionButton.setText("去配置");
                    actionButton.setOnClickListener(v -> {
                        Intent intent = new Intent(activity, AISettingsActivity.class);
                        activity.startActivity(intent);
                        // We keep the guide open? Or close and resume?
                        // It's better to close and expect user to come back.
                        // But standard behavior is Activity pauses.
                        // When resuming, we need to check if configured.
                    });
                }
                animationRes = R.drawable.anim_click;
                break;
            case STEP_PULL_UP:
                titleView.setText("手势操作：上滑检索");
                descView.setText("在题目底部继续向上滑动，自动检索相似题目。");
                animationRes = R.drawable.anim_swipe_up;
                nextButton.setText("完成");
                break;
        }

        playGuideAnimation(animationView, animationRes);
        nextButton.setOnClickListener(v -> nextStep());
    }

    private void playGuideAnimation(ImageView animationView, int drawableRes) {
        if (animationView == null || drawableRes == 0) return;
        animationView.setImageResource(drawableRes);
        animationView.post(() -> {
            Drawable drawable = animationView.getDrawable();
            if (drawable instanceof Animatable) {
                ((Animatable) drawable).start();
            }
        });
    }

    private void nextStep() {
        if (currentStep == STEP_AI && !aiSettingsManager.isConfigured()) {
             // If user clicks Next without configuring AI, we skip or force?
             // Requirement: "If not configured, jump to settings".
             // If they click Next, maybe they want to skip.
             // Let's check requirement: "If user didn't configure... skip API config."
        }

        currentStep++;
        saveCurrentStep(currentStep);  // 🔧 Save progress after each step
        
        if (currentStep > STEP_PULL_UP) {
            completeGuide();
        } else {
            showStep(currentStep);
        }
    }

    public void onResume() {
        // Check if we are in AI step and user just returned from Settings
        if (guideOverlay != null && guideOverlay.getVisibility() == View.VISIBLE && currentStep == STEP_AI) {
            showStep(STEP_AI); // Refresh UI state based on new config
        }
    }

    private void completeGuide() {
        // 🔧 Mark guide as completed and clear progress state
        prefs.edit()
            .putBoolean(KEY_GUIDE_COMPLETED, true)
            .putBoolean(KEY_GUIDE_IN_PROGRESS, false)
            .remove(KEY_CURRENT_STEP)
            .apply();
        hideGuide();
    }

    private void hideGuide() {
        if (guideOverlay == null) return;
        
        // 🔧 Check if Activity is still valid before animating
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            // Activity is destroyed, remove immediately without animation
            ViewGroup parent = (ViewGroup) guideOverlay.getParent();
            if (parent != null) {
                try {
                    parent.removeView(guideOverlay);
                } catch (IllegalStateException e) {
                    // Activity already destroyed, ignore
                }
            }
            guideOverlay = null;
            return;
        }
        
        // 🔧 Cancel any ongoing animations
        guideOverlay.animate().cancel();
        
        // Animate fade out
        guideOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    if (guideOverlay == null) return;
                    
                    // 🔧 Verify Activity is still valid when callback executes
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        ViewGroup parent = (ViewGroup) guideOverlay.getParent();
                        if (parent != null) {
                            try {
                                parent.removeView(guideOverlay);
                            } catch (IllegalStateException e) {
                                // Activity state changed during animation, ignore
                            }
                        }
                    }
                    guideOverlay = null;
                })
                .start();
    }

    // 🔧 Called when Activity is being destroyed to prevent memory leaks and crashes
    public void onDestroy() {
        if (guideOverlay != null) {
            // Cancel any ongoing animations
            guideOverlay.animate().cancel();
            
            // Remove overlay immediately
            ViewGroup parent = (ViewGroup) guideOverlay.getParent();
            if (parent != null) {
                try {
                    parent.removeView(guideOverlay);
                } catch (Exception e) {
                    // Ignore any exceptions during cleanup
                }
            }
            guideOverlay = null;
        }
    }

    // 🔧 Check if guide is currently showing (for Activity state restoration)
    public boolean isShowing() {
        return guideOverlay != null && guideOverlay.getVisibility() == View.VISIBLE;
    }

    public void resetGuide() {
        prefs.edit()
            .putBoolean(KEY_GUIDE_COMPLETED, false)
            .putBoolean(KEY_GUIDE_IN_PROGRESS, false)
            .remove(KEY_CURRENT_STEP)
            .apply();
    }
}
