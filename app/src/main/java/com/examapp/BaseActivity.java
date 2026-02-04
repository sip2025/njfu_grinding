package com.examapp;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.examapp.util.BackgroundApplier;
import com.examapp.data.SettingsManager;
public abstract class BaseActivity extends AppCompatActivity {
@Override
protected void onCreate(Bundle savedInstanceState) {
SettingsManager settingsManager = SettingsManager.getInstance(this);
AppCompatDelegate.setDefaultNightMode(settingsManager.getThemeMode());
super.onCreate(savedInstanceState);
}
@Override
protected void onResume() {
super.onResume();
BackgroundApplier.apply(this);
}
}