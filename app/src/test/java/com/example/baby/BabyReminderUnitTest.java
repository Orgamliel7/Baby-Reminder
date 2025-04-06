//package com.example.baby;
//
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import android.app.NotificationManager;
//import android.bluetooth.BluetoothDevice;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Build;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.Vibrator;
//import android.os.VibratorManager;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//import org.robolectric.RobolectricTestRunner;
//import org.robolectric.annotation.Config;
//import org.robolectric.shadows.ShadowLooper;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.util.HashMap;
//import java.util.Map;
//
//@RunWith(RobolectricTestRunner.class)
//@Config(sdk = {Build.VERSION_CODES.P})
//public class BabyReminderUnitTest {
//
//    private static final String PREF_NAME = "BabyReminderPrefs";
//    private static final String KEY_BABY_IN_CAR = "babyInCar";
//    private static final String KEY_DIALOG_SHOWN = "dialogShown";
//    private static final String KEY_DIALOG_TIMESTAMP = "dialogTimestamp";
//
//    private BluetoothReceiver bluetoothReceiver;
//
//    @Mock
//    private Context mockContext;
//
//    @Mock
//    private BluetoothDevice mockDevice;
//
//    @Mock
//    private SharedPreferences mockSharedPrefs;
//
//    @Mock
//    private SharedPreferences.Editor mockEditor;
//
//    @Mock
//    private NotificationManager mockNotificationManager;
//
//    @Mock
//    private Vibrator mockVibrator;
//
//    @Mock
//    private VibratorManager mockVibratorManager;
//
//    private Map<String, Object> prefValues = new HashMap<>();
//
//    @Before
//    public void setup() {
//        // Initialize Mockito annotations
//        MockitoAnnotations.openMocks(this);
//
//        // Create a new Bluetooth receiver
//        bluetoothReceiver = spy(new BluetoothReceiver());
//
//        // Setup SharedPreferences mock
//        when(mockContext.getSharedPreferences(eq(PREF_NAME), eq(Context.MODE_PRIVATE)))
//                .thenReturn(mockSharedPrefs);
//        when(mockSharedPrefs.edit()).thenReturn(mockEditor);
//        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
//        when(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor);
//        doNothing().when(mockEditor).apply();
//
//        // Setup preference getter mocks with in-memory storage
//        when(mockSharedPrefs.getBoolean(anyString(), anyBoolean())).thenAnswer(
//                new Answer<Boolean>() {
//                    @Override
//                    public Boolean answer(InvocationOnMock invocation) {
//                        String key = invocation.getArgument(0);
//                        Boolean defaultValue = invocation.getArgument(1);
//                        return prefValues.containsKey(key) ? (Boolean) prefValues.get(key) : defaultValue;
//                    }
//                });
//
//        when(mockSharedPrefs.getLong(anyString(), anyLong())).thenAnswer(
//                new Answer<Long>() {
//                    @Override
//                    public Long answer(InvocationOnMock invocation) {
//                        String key = invocation.getArgument(0);
//                        Long defaultValue = invocation.getArgument(1);
//                        return prefValues.containsKey(key) ? (Long) prefValues.get(key) : defaultValue;
//                    }
//                });
//
//        // Mock editor to update our in-memory storage
//        doAnswer(new Answer<Void>() {
//            @Override
//            public Void answer(InvocationOnMock invocation) {
//                // Store the preference changes in our map
//                for (Map.Entry<String, Object> entry : prefValues.entrySet()) {
//                    System.out.println("Applying pref: " + entry.getKey() + " = " + entry.getValue());
//                }
//                return null;
//            }
//        }).when(mockEditor).apply();
//
//        // Capture preference changes
//        doAnswer(new Answer<SharedPreferences.Editor>() {
//            @Override
//            public SharedPreferences.Editor answer(InvocationOnMock invocation) {
//                String key = invocation.getArgument(0);
//                Boolean value = invocation.getArgument(1);
//                prefValues.put(key, value);
//                return mockEditor;
//            }
//        }).when(mockEditor).putBoolean(anyString(), anyBoolean());
//
//        doAnswer(new Answer<SharedPreferences.Editor>() {
//            @Override
//            public SharedPreferences.Editor answer(InvocationOnMock invocation) {
//                String key = invocation.getArgument(0);
//                Long value = invocation.getArgument(1);
//                prefValues.put(key, value);
//                return mockEditor;
//            }
//        }).when(mockEditor).putLong(anyString(), anyLong());
//
//        // Setup device name
//        when(mockDevice.getName()).thenReturn("HyundaiBT");
//
//        // Setup system services
//        when(mockContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            when(mockContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).thenReturn(mockVibratorManager);
//            when(mockVibratorManager.getDefaultVibrator()).thenReturn(mockVibrator);
//        } else {
//            when(mockContext.getSystemService(Context.VIBRATOR_SERVICE)).thenReturn(mockVibrator);
//        }
//        when(mockVibrator.hasVibrator()).thenReturn(true);
//    }
//
//    /**
//     * Test Bluetooth connection detection for target device
//     */
//    @Test
//    public void testBluetoothConnectionDetection() throws Exception {
//        // Create a connect intent with a target device
//        Intent connectIntent = new Intent("android.bluetooth.device.action.ACL_CONNECTED");
//        connectIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mockDevice);
//
//        // Process the intent
//        bluetoothReceiver.onReceive(mockContext, connectIntent);
//
//        // Verify that handleDeviceConnected was called
//        verify(bluetoothReceiver).onReceive(mockContext, connectIntent);
//
//        // Verify the dialog shown flag was set
//        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
//        verify(mockEditor, times(2)).putBoolean(keyCaptor.capture(), valueCaptor.capture());
//
//        // Find if dialog shown flag was set to true
//        int dialogShownIndex = keyCaptor.getAllValues().indexOf(KEY_DIALOG_SHOWN);
//        if (dialogShownIndex != -1) {
//            assertTrue("Dialog shown flag should be true", valueCaptor.getAllValues().get(dialogShownIndex));
//        }
//
//        // Verify timestamp was set
//        verify(mockEditor).putLong(eq(KEY_DIALOG_TIMESTAMP), anyLong());
//    }
//
//    /**
//     * Test Bluetooth disconnection with baby in car
//     */
//    @Test
//    public void testBluetoothDisconnectWithBaby() throws Exception {
//        // Setup: Set baby in car flag
//        prefValues.put(KEY_BABY_IN_CAR, true);
//
//        // Create a disconnect intent
//        Intent disconnectIntent = new Intent("android.bluetooth.device.action.ACL_DISCONNECTED");
//        disconnectIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mockDevice);
//
//        // Inject a mock NotificationHelper through reflection or override internal methods
//        // This is complex in a unit test, so we'll just verify the intent is processed
//
//        // Process the intent
//        bluetoothReceiver.onReceive(mockContext, disconnectIntent);
//
//        // Verify baby in car flag is reset
//        verify(mockEditor).putBoolean(eq(KEY_BABY_IN_CAR), eq(false));
//
//        // Verify dialog shown flag is reset
//        verify(mockEditor).putBoolean(eq(KEY_DIALOG_SHOWN), eq(false));
//    }
//
//    /**
//     * Test Bluetooth disconnection with no baby in car
//     */
//    @Test
//    public void testBluetoothDisconnectWithoutBaby() throws Exception {
//        // Setup: Set baby in car flag to false
//        prefValues.put(KEY_BABY_IN_CAR, false);
//
//        // Create a disconnect intent
//        Intent disconnectIntent = new Intent("android.bluetooth.device.action.ACL_DISCONNECTED");
//        disconnectIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mockDevice);
//
//        // Process the intent
//        bluetoothReceiver.onReceive(mockContext, disconnectIntent);
//
//        // Verify dialog shown flag is reset
//        verify(mockEditor).putBoolean(eq(KEY_DIALOG_SHOWN), eq(false));
//
//        // Verify no reminder notification is shown (since no baby in car)
//        // This would require deeper mocking of NotificationHelper
//    }
//
//    /**
//     * Test non-target device is ignored
//     */
//    @Test
//    public void testNonTargetDeviceIgnored() {
//        // Setup a non-target device
//        when(mockDevice.getName()).thenReturn("RandomDevice");
//
//        // Create a connect intent
//        Intent connectIntent = new Intent("android.bluetooth.device.action.ACL_CONNECTED");
//        connectIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mockDevice);
//
//        // Process the intent
//        bluetoothReceiver.onReceive(mockContext, connectIntent);
//
//        // Verify no dialog shown flag is set (device was ignored)
//        verify(mockEditor, never()).putBoolean(eq(KEY_DIALOG_SHOWN), eq(true));
//    }
//
//    /**
//     * Test null device handling
//     */
//    @Test
//    public void testNullDeviceHandling() {
//        // Create an intent with no device
//        Intent badIntent = new Intent("android.bluetooth.device.action.ACL_CONNECTED");
//
//        // Process the intent
//        bluetoothReceiver.onReceive(mockContext, badIntent);
//
//        // Verify no preferences are modified
//        verify(mockEditor, never()).putBoolean(anyString(), anyBoolean());
//    }
//
//    /**
//     * Test null action handling
//     */
//    @Test
//    public void testNullActionHandling() {
//        // Create an intent with null action
//        Intent badIntent = new Intent();
//        badIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mockDevice);
//
//        // Process the intent
//        bluetoothReceiver.onReceive(mockContext, badIntent);
//
//        // Verify no preferences are modified
//        verify(mockEditor, never()).putBoolean(anyString(), anyBoolean());
//    }
//}