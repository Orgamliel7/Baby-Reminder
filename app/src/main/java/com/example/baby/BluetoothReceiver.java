package com.example.baby;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.Arrays;
import java.util.List;
import android.Manifest;


public class BluetoothReceiver extends BroadcastReceiver {

    private static final String TAG = "BluetoothReceiver";
    private static final String PREF_NAME = "BabyReminderPrefs";
    private static final String KEY_BABY_IN_CAR = "babyInCar";
    private static final String KEY_DIALOG_SHOWN = "dialogShown";
    private static final String KEY_DIALOG_TIMESTAMP = "dialogTimestamp";

    // Time constants (in milliseconds)
    private static final long INITIAL_REMINDER_DELAY = 10000; // 10 seconds
    private static final long VIBRATION_DURATION = 3000; // 3 seconds
    private static final long VIBRATION_PAUSE = 3000; // 3 seconds
    private static final long REMINDER_CAMPAIGN_DURATION = 30000; // 30 seconds (half a minute)
    private static final int FOLLOW_UP_ALERTS = 3; // Number of additional alerts to send

    // List of target Bluetooth device names
    private static final List<String> TARGET_DEVICE_NAMES = Arrays.asList(
            "HyundaiBT",
            "CAR MULTIMEDIA",
            "JBL Flip 4"
    );

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "Intent action was null");
                return;
            }

            Log.d(TAG, "Bluetooth action received: " + action);

            // Get the BluetoothDevice from the intent with robust null check
            BluetoothDevice device = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                } else {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting Bluetooth device: " + e.getMessage());
            }

            if (device == null) {
                Log.e(TAG, "BluetoothDevice was null in onReceive");
                return;
            }

            // Safely get device name
            String deviceName = null;
            try {
                deviceName = getDeviceName(context, device);
            } catch (Exception e) {
                Log.e(TAG, "Error getting device name: " + e.getMessage());
                return;
            }

            Log.d(TAG, "Device name: " + deviceName);

            // Only proceed if we have a valid device name to check
            if (deviceName != null && TARGET_DEVICE_NAMES.contains(deviceName)) {
                if ("android.bluetooth.device.action.ACL_CONNECTED".equals(action)) {
                    Log.d(TAG, "Connected to target device: " + deviceName);
                    handleDeviceConnected(context);
                } else if ("android.bluetooth.device.action.ACL_DISCONNECTED".equals(action)) {
                    Log.d(TAG, "Disconnected from target device: " + deviceName);
                    handleDeviceDisconnected(context);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onReceive: " + e.getMessage(), e);
        }
    }

    // Handle device connected event
    private void handleDeviceConnected(final Context context) {
        try {
            // Send immediate connection alert with vibration
            NotificationHelper.showConnectionAlert(context);
            vibratePhone(context, 2000); // 2 seconds of gentle vibration

            // Set dialog shown timestamp for follow-up reminders
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_DIALOG_SHOWN, true);
            editor.putLong(KEY_DIALOG_TIMESTAMP, System.currentTimeMillis());
            editor.apply();

            // Schedule follow-up reminders if no response
            scheduleDialogReminders(context);
        } catch (Exception e) {
            Log.e(TAG, "Error in handleDeviceConnected: " + e.getMessage(), e);
        }
    }

    // Handle device disconnected event
    private void handleDeviceDisconnected(Context context) {
        try {
            // Car disconnected - check if baby was in car
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            boolean babyInCar = prefs.getBoolean(KEY_BABY_IN_CAR, false);

            if (babyInCar) {
                // Send initial reminder notification
                NotificationHelper.showBabyReminder(context);

                // Start the escalating notification sequence
                startEscalatingReminders(context);

                // Reset the flag
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_BABY_IN_CAR, false);
                editor.apply();
            }

            // Always reset dialog flags when disconnecting
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_DIALOG_SHOWN, false);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error in handleDeviceDisconnected: " + e.getMessage(), e);
        }
    }

    // Schedule reminders if the user doesn't respond to the baby dialog
    private void scheduleDialogReminders(final Context context) {
        try {
            final Handler handler = new Handler(Looper.getMainLooper());

            // Send two follow-up reminders after 10 seconds
            handler.postDelayed(() -> {
                try {
                    SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    boolean dialogShown = prefs.getBoolean(KEY_DIALOG_SHOWN, false);
                    long dialogTimestamp = prefs.getLong(KEY_DIALOG_TIMESTAMP, 0);
                    long timeElapsed = System.currentTimeMillis() - dialogTimestamp;

                    // Only send reminders if dialog was shown and it's been around 10 seconds
                    if (dialogShown && timeElapsed >= 9000 && timeElapsed <= 15000) {
                        Log.d(TAG, "Sending dialog follow-up reminders");

                        // Send first reminder
                        NotificationHelper.showDialogReminder(context, 1);

                        // Send second reminder 3 seconds later
                        handler.postDelayed(() -> {
                            try {
                                NotificationHelper.showDialogReminder(context, 2);
                            } catch (Exception e) {
                                Log.e(TAG, "Error showing second dialog reminder: " + e.getMessage(), e);
                            }
                        }, 3000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in scheduleDialogReminders delayed task: " + e.getMessage(), e);
                }
            }, INITIAL_REMINDER_DELAY);
        } catch (Exception e) {
            Log.e(TAG, "Error in scheduleDialogReminders: " + e.getMessage(), e);
        }
    }

    // Start the sequence of escalating reminders if user doesn't respond
    private void startEscalatingReminders(final Context context) {
        try {
            final Handler handler = new Handler(Looper.getMainLooper());

            // After 10 seconds with no response, start the pattern of vibrations
            handler.postDelayed(() -> {
                try {
                    // Check if the notification was acknowledged
                    boolean wasAcknowledged = NotificationHelper.wasReminderAcknowledged(context);

                    if (!wasAcknowledged) {
                        // Start the pattern of vibrations
                        startVibrationPattern(context);

                        // Send additional notifications
                        sendFollowUpNotifications(context, handler);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in startEscalatingReminders delayed task: " + e.getMessage(), e);
                }
            }, INITIAL_REMINDER_DELAY);
        } catch (Exception e) {
            Log.e(TAG, "Error in startEscalatingReminders: " + e.getMessage(), e);
        }
    }

    // Start a pattern of vibrations (3 seconds on, 3 seconds off) for 30 seconds
    private void startVibrationPattern(final Context context) {
        try {
            final Handler handler = new Handler(Looper.getMainLooper());
            final long endTime = System.currentTimeMillis() + REMINDER_CAMPAIGN_DURATION;

            final Runnable vibrationRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (System.currentTimeMillis() < endTime) {
                            // Vibrate for 3 seconds
                            vibratePhone(context, VIBRATION_DURATION);

                            // Schedule next vibration after pause
                            handler.postDelayed(this, VIBRATION_DURATION + VIBRATION_PAUSE);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in vibrationRunnable: " + e.getMessage(), e);
                    }
                }
            };

            // Start the vibration pattern
            handler.post(vibrationRunnable);
        } catch (Exception e) {
            Log.e(TAG, "Error in startVibrationPattern: " + e.getMessage(), e);
        }
    }

    // Helper method to vibrate the phone
    private void vibratePhone(Context context, long duration) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing VIBRATE permission");
                return;
            }
            Vibrator vibrator = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager =
                        (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    vibrator = vibratorManager.getDefaultVibrator();
                }
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // For Android 8.0 (Oreo) and above
                    VibrationEffect effect;
                    if (duration >= 5000) {
                        // For longer vibrations, use a gentle pattern
                        long[] pattern = {0, 500, 200, 500, 200};
                        effect = VibrationEffect.createWaveform(pattern, 0);
                        vibrator.vibrate(effect);
                    } else {
                        // For shorter alerts, use a continuous vibration
                        effect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE);
                        vibrator.vibrate(effect);
                    }
                } else {
                    // For Android 7.1 and below
                    vibrator.vibrate(duration);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in vibratePhone: " + e.getMessage(), e);
        }
    }

    // Send 3 follow-up notifications
    private void sendFollowUpNotifications(final Context context, final Handler handler) {
        try {
            for (int i = 0; i < FOLLOW_UP_ALERTS; i++) {
                final int notificationNumber = i + 1;
                // Space out the notifications during the vibration campaign
                long delay = INITIAL_REMINDER_DELAY + (REMINDER_CAMPAIGN_DURATION / (FOLLOW_UP_ALERTS + 1)) * (i + 1);

                handler.postDelayed(() -> {
                    try {
                        NotificationHelper.showFollowUpReminder(context, notificationNumber);
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing follow-up reminder: " + e.getMessage(), e);
                    }
                }, delay);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in sendFollowUpNotifications: " + e.getMessage(), e);
        }
    }

    // Safely get device name with permission check
    private String getDeviceName(Context context, BluetoothDevice device) {
        String deviceName = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12 (S) and above
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                    deviceName = device.getName();
                } else {
                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission");
                    // We don't have permission, return a default name that won't match any target
                    return null;
                }
            } else {
                // For Android 11 and below
                deviceName = device.getName();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when accessing Bluetooth device name", e);
        } catch (Exception e) {
            Log.e(TAG, "Error getting device name: " + e.getMessage(), e);
        }

        return deviceName;
    }
}