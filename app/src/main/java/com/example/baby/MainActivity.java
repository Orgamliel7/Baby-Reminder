package com.example.baby;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREF_NAME = "BabyReminderPrefs";
    private static final String KEY_BABY_IN_CAR = "babyInCar";
    private static final String KEY_DIALOG_SHOWN = "dialogShown";
    private static final String KEY_DIALOG_TIMESTAMP = "dialogTimestamp";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private AlertDialog babyDialog = null;
    private boolean pendingShowDialog = false;

    private void setupNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationHelper.createNotificationChannel(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                Log.e("CRASH", "Uncaught exception: " + e.getMessage(), e);
                SharedPreferences prefs = getSharedPreferences("CrashLogs", MODE_PRIVATE);
                prefs.edit().putString("lastCrash", e.toString() + "\n" + Log.getStackTraceString(e)).apply();
            }
        });
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            requestPermissions();
            setupNotifications();
            processIntent(getIntent());
            updateStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        try {
            super.onNewIntent(intent);
            setIntent(intent); // Store the new intent
            processIntent(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error in onNewIntent: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        try {
            super.onResume();
            // Update the UI status
            updateStatus();

            // Check if we should show the dialog
            if (pendingShowDialog) {
                showBabyDialog();
                pendingShowDialog = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onPause() {
        try {
            super.onPause();
            // Dismiss dialog to prevent memory leaks
            dismissBabyDialog();
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            // Ensure dialog is dismissed
            dismissBabyDialog();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }

    private void dismissBabyDialog() {
        try {
            if (babyDialog != null && babyDialog.isShowing()) {
                babyDialog.dismiss();
                babyDialog = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing dialog: " + e.getMessage(), e);
        }
    }

    private void processIntent(Intent intent) {
        try {
            if (intent == null) {
                Log.d(TAG, "Intent is null, nothing to process");
                return;
            }

            Log.d(TAG, "Processing intent: " + intent.toString());

            // Check if notification was acknowledged
            if (intent.hasExtra("acknowledgeReminder") && intent.getBooleanExtra("acknowledgeReminder", false)) {
                handleNotificationAcknowledgment();
                intent.removeExtra("acknowledgeReminder"); // Prevent reprocessing
            }

            // Check if we need to show the baby dialog
            if (intent.hasExtra("showBabyDialog") && intent.getBooleanExtra("showBabyDialog", false)) {
                Log.d(TAG, "showBabyDialog flag is true, setting pending flag.");
                pendingShowDialog = true;
                intent.removeExtra("showBabyDialog"); // Prevent reprocessing
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing intent: " + e.getMessage(), e);
        }
    }
    private void handleNotificationAcknowledgment() {
        try {
            // User has tapped on the notification
            NotificationHelper.setReminderAcknowledged(this, true);

            // Cancel any ongoing notifications
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.cancelAll();
            } else {
                Log.e(TAG, "NotificationManager is null");
            }

            // Show confirmation to user
            Toast.makeText(this, "Baby reminder acknowledged", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Baby reminder acknowledged by user");
        } catch (Exception e) {
            Log.e(TAG, "Error handling notification acknowledgment: " + e.getMessage(), e);
        }
    }

    private void showBabyDialog() {
        try {
            if (isFinishing() || isDestroyed()) {
                Log.w(TAG, "Activity is finishing or destroyed, skipping dialog display.");
                pendingShowDialog = false; // Reset the flag
                return;
            }

            // Run on UI thread to ensure proper context
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Check again in case conditions changed
                        if (isFinishing() || isDestroyed()) {
                            Log.w(TAG, "Activity became invalid during UI thread execution.");
                            pendingShowDialog = false;
                            return;
                        }

                        Log.d(TAG, "Showing baby dialog");

                        // Set dialog shown flag and timestamp
                        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                        editor.putBoolean(KEY_DIALOG_SHOWN, true);
                        editor.putLong(KEY_DIALOG_TIMESTAMP, System.currentTimeMillis());
                        editor.apply();

                        // Cancel all previous notifications
                        NotificationManager notificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (notificationManager != null) {
                            notificationManager.cancelAll();
                        } else {
                            Log.e(TAG, "NotificationManager is null");
                        }

                        // Dismiss any existing dialog to prevent stacking
                        dismissBabyDialog();

                        // Create and show the dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Baby Check")
                                .setMessage("Is there a baby in the car?")
                                .setCancelable(false)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putBoolean(KEY_BABY_IN_CAR, true);
                                            editor.putBoolean(KEY_DIALOG_SHOWN, false); // Reset dialog flag
                                            editor.apply();

                                            Toast.makeText(MainActivity.this, "Baby presence confirmed", Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "User confirmed baby is in car");
                                            updateStatus();
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error in Yes button click: " + e.getMessage(), e);
                                            pendingShowDialog = false;
                                        }
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putBoolean(KEY_BABY_IN_CAR, false);
                                            editor.putBoolean(KEY_DIALOG_SHOWN, false); // Reset dialog flag
                                            editor.apply();

                                            Log.d(TAG, "User confirmed no baby in car");
                                            updateStatus();
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error in No button click: " + e.getMessage(), e);
                                        }
                                    }
                                });

                        babyDialog = builder.create();
                        babyDialog.show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing dialog on UI thread: " + e.getMessage(), e);
                        pendingShowDialog = false;
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up dialog: " + e.getMessage(), e);
            pendingShowDialog = false;
        }
    }


    private void updateStatus() {
        try {
            TextView statusText = findViewById(R.id.statusText);
            if (statusText == null) {
                Log.e(TAG, "Status TextView not found");
                return;
            }

            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            boolean babyInCar = prefs.getBoolean(KEY_BABY_IN_CAR, false);

            if (babyInCar) {
                statusText.setText("Status: Baby is in the car");
                Log.d(TAG, "Status updated: Baby is in car");
            } else {
                statusText.setText("Status: No baby in the car");
                Log.d(TAG, "Status updated: No baby in car");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating status: " + e.getMessage(), e);
        }
    }

    private void requestPermissions() {
        try {
            String[] permissions;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12 (S) and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // For Android 13 (Tiramisu) and above - needs POST_NOTIFICATIONS permission
                    permissions = new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.POST_NOTIFICATIONS,
                            Manifest.permission.VIBRATE
                    };
                } else {
                    // For Android 12 (S)
                    permissions = new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.VIBRATE
                    };
                }
            } else {
                // For Android 11 and below
                permissions = new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.VIBRATE
                };
            }

            // Check if we need to request permissions
            boolean needsPermission = false;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    needsPermission = true;
                    break;
                }
            }

            if (needsPermission) {
                Log.d(TAG, "Requesting permissions");
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            } else {
                Log.d(TAG, "All permissions already granted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting permissions: " + e.getMessage(), e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == PERMISSION_REQUEST_CODE) {
                boolean allPermissionsGranted = true;

                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }

                if (allPermissionsGranted) {
                    Log.d(TAG, "All permissions granted");
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w(TAG, "Some permissions were denied");
                    Toast.makeText(this,
                            "Some permissions were denied. The app may not function properly.",
                            Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in permission result handling: " + e.getMessage(), e);
        }
    }
}