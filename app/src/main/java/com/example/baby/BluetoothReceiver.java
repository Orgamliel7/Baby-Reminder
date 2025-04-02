// \baby\app\src\main\java\com\example\baby\BluetoothReceiver.java
package com.example.baby;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.Arrays;
import java.util.List;

public class BluetoothReceiver extends BroadcastReceiver {

    private static final String TAG = "BluetoothReceiver";
    private static final String PREF_NAME = "BabyReminderPrefs";
    private static final String KEY_BABY_IN_CAR = "babyInCar";

    // List of target Bluetooth device names
    private static final List<String> TARGET_DEVICE_NAMES = Arrays.asList(
            "HyundaiBT",
            "CAR MULTIMEDIA",
            "JBL Flip 4"
    );

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (device != null) {
            String deviceName = getDeviceName(context, device);

            // Check if the device name is in our list of target devices
            if (deviceName != null && TARGET_DEVICE_NAMES.contains(deviceName)) {
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    Log.d(TAG, "Connected to target device: " + deviceName);
                    // Car connected - show dialog in MainActivity
                    Intent dialogIntent = new Intent(context, MainActivity.class);
                    dialogIntent.putExtra("showBabyDialog", true);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(dialogIntent);

                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    Log.d(TAG, "Disconnected from target device: " + deviceName);
                    // Car disconnected - check if baby was in car
                    SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    boolean babyInCar = prefs.getBoolean(KEY_BABY_IN_CAR, false);

                    if (babyInCar) {
                        // Send reminder notification
                        NotificationHelper.showBabyReminder(context);

                        // Reset the flag
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(KEY_BABY_IN_CAR, false);
                        editor.apply();
                    }
                }
            }
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
        }

        return deviceName;
    }
}