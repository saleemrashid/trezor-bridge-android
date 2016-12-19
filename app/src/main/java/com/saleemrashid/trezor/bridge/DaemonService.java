package com.saleemrashid.trezor.bridge;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class DaemonService extends Service {
    private static final String TAG = DaemonService.class.getSimpleName();

    public static final Uri SSL_CERTIFICATE_URI = Uri.parse("https://wallet.trezor.io/data/bridge/cert/server.crt");
    public static final Uri SSL_PRIVATE_KEY_URI = Uri.parse("https://wallet.trezor.io/data/bridge/cert/server.key");

    private DownloadManager mDownloadManager;

    /* DownloadManager IDs */
    private long mCertificateDownloadId;
    private long mPrivateKeyDownloadId;

    private BroadcastReceiver mReceiver = null;

    private final Map<String, UsbDevice> mDevices = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Service created");

        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        mCertificateDownloadId = startDownload(SSL_CERTIFICATE_URI, R.string.ssl_certificate_title);
        mPrivateKeyDownloadId = startDownload(SSL_PRIVATE_KEY_URI, R.string.ssl_private_key_title);

        startForeground();
        registerDetachReceiver();
        startServer();
    }

    private long startDownload(final Uri uri, int title) {
        Log.i(TAG, "Starting download: " + uri);

        final DownloadManager.Request request = new DownloadManager.Request(uri)
                .setTitle(getResources().getText(title))
                .setVisibleInDownloadsUi(false);

        return mDownloadManager.enqueue(request);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Service called (startId is " + startId + ")");
        super.onStartCommand(intent, flags, startId);

        if (!UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            Log.e(TAG, "Action is not ACTION_USB_DEVICE_ATTACHED");

            stopIfNoDevices();

            return START_NOT_STICKY;
        }

        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (device == null) {
            Log.e(TAG, "Intent missing EXTRA_DEVICE");

            stopIfNoDevices();

            return START_NOT_STICKY;
        }

        registerDevice(device);

        return START_STICKY;
    }

    private void registerDevice(final UsbDevice device) {
        /* If in doubt, always assume `device` is non-stale and `oldDevice` is stale */
        final UsbDevice oldDevice = mDevices.put(device.getDeviceName(), device);

        /* TODO: These edge cases should never happen, error reporting should be implemented */
        if (oldDevice == null) {
            Log.i(TAG, "Registered device: " + device.getDeviceName());
        } else if (device.equals(oldDevice)) {
            Log.w(TAG, "Device was already registered: " + device.getDeviceName());
        } else {
            /* Overwriting the device is correct as stale devices will cause errors */
            Log.w(TAG, "Different device of identical name had not been unregistered: " + device.getDeviceName());
        }

    }

    private boolean isDeviceRegistered(final UsbDevice device) {
        return mDevices.containsKey(device.getDeviceName());
    }

    private void unregisterDevice(final UsbDevice device) {
        /* If in doubt, always assume `device` is non-stale and `oldDevice` is stale */
        final UsbDevice oldDevice = mDevices.remove(device.getDeviceName());

        /* TODO: These edge cases should never happen, error reporting should be implemented */
        if (device.equals(oldDevice)) {
            Log.i(TAG, "Unregistered device: " + device.getDeviceName());
        } else if (oldDevice == null) {
            Log.w(TAG, "Device was not registered: " + device.getDeviceName());
        } else {
            /* Unregistering the device is correct as stale devices will cause errors */
            Log.w(TAG, "Different device had been registered, unregistered anyway: " + device.getDeviceName());
        }
    }

    /* TODO: What the hell is this name? */
    private boolean stopIfNoDevices() {
        if (mDevices.size() == 0) {
            Log.i(TAG, "No devices are registered, stopping");
            stopSelf();

            return true;
        }

        Log.v(TAG, mDevices.size() + " device(s) are registered, not stopping");
        return false;
    }

    @Override
    public void onDestroy() {
        unregisterDetachReceiver();

        Log.v(TAG, "Service destroyed");
        super.onDestroy();
    }

    private void registerDetachReceiver() {
        if (mReceiver == null) {
            Log.v(TAG, "Registering BroadcastReceiver");

            final IntentFilter filter = new IntentFilter();

            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                        Log.i(TAG, "Received ACTION_USB_DEVICE_DETACHED");

                        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (device == null) {
                            Log.e(TAG, "Intent missing EXTRA_DEVICE");

                            return;
                        }

                        if (isDeviceRegistered(device)) {
                            unregisterDevice(device);

                            stopIfNoDevices();
                        } else {
                            Log.i(TAG, "Device was not registered");
                        }
                    } else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                        if (downloadId != mCertificateDownloadId && downloadId != mPrivateKeyDownloadId) {
                            return;
                        }

                        Log.i(TAG, "Download completed");

                        /* TODO: Consume the downloads */
                    }
                }
            };

            registerReceiver(mReceiver, filter);
        } else {
            Log.w(TAG, "Cannot register BroadcastReceiver, already registered");
        }
    }

    private void unregisterDetachReceiver() {
        if (mReceiver != null) {
            Log.v(TAG, "Unregistering BroadcastReceiver");

            unregisterReceiver(mReceiver);

            mReceiver = null;
        } else {
            Log.w(TAG, "Cannot unregister BroadcastReceiver, not registered");
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
