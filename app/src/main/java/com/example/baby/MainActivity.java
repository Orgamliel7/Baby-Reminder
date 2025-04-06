// \baby\app\src\main\java\com\example\baby\MainActivity.java
// This is the main activity that handles permissions, shows the current status,
// and displays the 'Baby Check' dialog when requested.
package com.example.baby;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent; // Add this import
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

// Fix these imports
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NAME = "BabyReminderPrefs";
    private static final String KEY_BABY_IN_CAR = "babyInCar";
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create notification channel
        NotificationHelper.createNotificationChannel(this);

        // Request necessary permissions
        requestPermissions();

        // Check if we need to show the baby dialog
        if (getIntent() != null && getIntent().getBooleanExtra("showBabyDialog", false)) {
            showBabyDialog();
        }

        // Show status in UI
        updateStatus();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null && intent.getBooleanExtra("showBabyDialog", false)) {
            showBabyDialog();
        }
    }

    private void showBabyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Baby Check")
                .setMessage("Is there a baby in the car?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Save the state
                    SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(KEY_BABY_IN_CAR, true);
                    editor.apply();

                    updateStatus();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Clear the state
                    SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(KEY_BABY_IN_CAR, false);
                    editor.apply();

                    updateStatus();
                })
                .setCancelable(false)
                .show();
    }

    private void updateStatus() {
        TextView statusText = findViewById(R.id.statusText);
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean babyInCar = prefs.getBoolean(KEY_BABY_IN_CAR, false);

        if (babyInCar) {
            statusText.setText("Status: Baby is in the car");
        } else {
            statusText.setText("Status: No baby in the car");
        }
    }

    private void requestPermissions() {
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            };
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                break;
            }
        }
    }
}