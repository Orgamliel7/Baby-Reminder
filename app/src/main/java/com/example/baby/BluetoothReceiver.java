package com.example.baby;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BluetoothReceiver extends BroadcastReceiver {

    private static final String PREF_NAME = "BabyReminderPrefs";
    private static final String KEY_BABY_IN_CAR = "babyInCar";
    private static final String TARGET_DEVICE_NAME = "HyundaiBT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (device != null) {
            String deviceName = device.getName();

            // Only process for our target car Bluetooth
            if (deviceName != null && deviceName.equals(TARGET_DEVICE_NAME)) {
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    // Car connected - show dialog in MainActivity
                    Intent dialogIntent = new Intent(context, MainActivity.class);
                    dialogIntent.putExtra("showBabyDialog", true);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(dialogIntent);

                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
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
}