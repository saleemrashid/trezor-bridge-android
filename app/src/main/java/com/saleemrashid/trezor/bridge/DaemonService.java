package com.saleemrashid.trezor.bridge;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class DaemonService extends Service {
    private static final String TAG = DaemonService.class.getSimpleName();

    private BroadcastReceiver mDetachReceiver = null;

    @Override
    public void onCreate() {
        Log.v(TAG, "Service created");

        super.onCreate();

        startForeground();
        registerDetachReceiver();

        /* TODO: Start WorkerThread and HTTP server */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Service called (startId is " + startId + ")");

        super.onStartCommand(intent, flags, startId);

        if (!UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            Log.w(TAG, "Action is not ACTION_USB_DEVICE_ATTACHED");

            return START_NOT_STICKY;
        }

        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (device == null) {
            Log.w(TAG, "Intent missing EXTRA_DEVICE");

            return START_NOT_STICKY;
        }

        /* TODO: Take ownership of the device */

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Service destroyed");

        unregisterDetachReceiver();

        super.onDestroy();
    }

    private void registerDetachReceiver() {
        if (mDetachReceiver == null) {
            Log.v(TAG, "Registering BroadcastReceiver for ACTION_USB_DEVICE_DETACHED");

            mDetachReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i(TAG, "Received ACTION_USB_DEVICE_DETACHED");

                    final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (device == null) {
                        Log.w(TAG, "Intent missing EXTRA_DEVICE");

                        return;
                    }

                    /* TODO: Cross match device on list of devices owned by the daemon */
                    /* TODO: Stop service if device is last device owned by the daemon */
                }
            };

            registerReceiver(mDetachReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        } else {
            Log.w(TAG, "Cannot register for ACTION_USB_DEVICE_DETACHED, already registered");
        }
    }

    private void unregisterDetachReceiver() {
        if (mDetachReceiver != null) {
            Log.v(TAG, "Unregistering BroadcastReceiver for ACTION_USB_DEVICE_DETACHED");

            unregisterReceiver(mDetachReceiver);

            mDetachReceiver = null;
        } else {
            Log.w(TAG, "Cannot unregister for ACTION_USB_DEVICE_DETACHED, not registered");
        }
    }

    private void startForeground() {
        Log.v(TAG, "Starting in foreground");

        final Intent intent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getText(R.string.app_name))
                .setContentText(getResources().getText(R.string.daemon_notification_text))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_stat_trezor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        final Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(1, notification);
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        Log.w(TAG, "Service binding not implemented");

        return null;
    }
}
