package com.example.baby;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "BabyReminderChannel";
    private static final String CHANNEL_NAME = "Baby Reminder Notifications";
    private static final int NOTIFICATION_ID = 1001;
    private static final int CONNECTION_NOTIFICATION_ID = 1002;
    private static final int DIALOG_REMINDER_BASE_ID = 2000;
    private static final String PREF_NAME = "BabyReminderPrefs";
    private static final String KEY_REMINDER_ACKNOWLEDGED = "reminderAcknowledged";


    // Fix in NotificationHelper.java - Improve notification channel creation
    public static void createNotificationChannel(Context context) {
        try {
            // Create the notification channel for Android 8.0 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager == null) {
                    Log.e(TAG, "NotificationManager is null");
                    return;
                }

                // Check if channel already exists
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (existingChannel != null) {
                    Log.d(TAG, "Notification channel already exists");
                    return;
                }

                try {
                    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();

                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription("Notifications for baby reminder app");
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                    channel.setSound(defaultSoundUri, audioAttributes);
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created");
                } catch (Exception e) {
                    Log.e(TAG, "Error configuring notification channel: " + e.getMessage());

                    // Create a simpler channel as fallback
                    try {
                        NotificationChannel simpleChannel = new NotificationChannel(
                                CHANNEL_ID,
                                CHANNEL_NAME,
                                NotificationManager.IMPORTANCE_HIGH);
                        notificationManager.createNotificationChannel(simpleChannel);
                        Log.d(TAG, "Simple notification channel created as fallback");
                    } catch (Exception ex) {
                        Log.e(TAG, "Critical error creating fallback channel: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel: " + e.getMessage(), e);
        }
    }

    public static void showBabyReminder(Context context) {
        try {
            // Set the notification as not acknowledged
            setReminderAcknowledged(context, false);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }

            // Create intent for notification tap action
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("acknowledgeReminder", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, getPendingIntentFlags());

            // Set up the notification
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)  // Using system icon
                    .setContentTitle("Baby Reminder!")
                    .setContentText("Don't forget your baby in the car!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setSound(defaultSoundUri)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            // Show notification
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            Log.d(TAG, "Baby reminder notification shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing baby reminder: " + e.getMessage(), e);
        }
    }

    public static void showConnectionAlert(Context context) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }

            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("showBabyDialog", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, getPendingIntentFlags());

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)  // Using system icon
                    .setContentTitle("Car Connected")
                    .setContentText("Your car's Bluetooth has connected")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSound(defaultSoundUri)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            notificationManager.notify(CONNECTION_NOTIFICATION_ID, notificationBuilder.build());
            Log.d(TAG, "Connection alert notification shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing connection alert: " + e.getMessage(), e);
        }
    }

    public static void showDialogReminder(Context context, int reminderNumber) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }

            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("showBabyDialog", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 100 + reminderNumber, intent, getPendingIntentFlags());

            Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alertSound == null) {
                alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Car Check Reminder")
                    .setContentText("Please confirm if there is a baby in the car")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setSound(alertSound)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            notificationManager.notify(DIALOG_REMINDER_BASE_ID + reminderNumber, notificationBuilder.build());
            Log.d(TAG, "Dialog reminder " + reminderNumber + " notification shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog reminder: " + e.getMessage(), e);
        }
    }

    public static void showFollowUpReminder(Context context, int reminderNumber) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }

            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("acknowledgeReminder", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, reminderNumber, intent, getPendingIntentFlags());

            Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alertSound == null) {
                alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)  // Using system icon
                    .setContentTitle("URGENT: Baby Reminder!")
                    .setContentText("Please check your car! Don't forget your baby!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setSound(alertSound)
                    .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            // Use a different ID for each follow-up notification
            notificationManager.notify(NOTIFICATION_ID + 100 + reminderNumber, notificationBuilder.build());
            Log.d(TAG, "Follow-up reminder " + reminderNumber + " notification shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing follow-up reminder: " + e.getMessage(), e);
        }
    }

    public static boolean wasReminderAcknowledged(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_REMINDER_ACKNOWLEDGED, false);
        } catch (Exception e) {
            Log.e(TAG, "Error checking if reminder was acknowledged: " + e.getMessage(), e);
            return false;
        }
    }

    public static void setReminderAcknowledged(Context context, boolean acknowledged) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_REMINDER_ACKNOWLEDGED, acknowledged);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting reminder acknowledged: " + e.getMessage(), e);
        }
    }

    private static int getPendingIntentFlags() {
        // Handle the PendingIntent flag difference for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }
}