package com.saleemrashid.trezor.bridge.daemon;

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

import com.saleemrashid.trezor.bridge.BuildConfig;
import com.saleemrashid.trezor.bridge.MainActivity;
import com.saleemrashid.trezor.bridge.R;
import com.saleemrashid.trezor.bridge.helpers.DownloadHelper;
import com.saleemrashid.trezor.bridge.helpers.SSLHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.Response;

public class DaemonService extends Service {
    private static final String TAG = DaemonService.class.getSimpleName();

    public static final String HOST = BuildConfig.SERVER_HOST;
    public static final int PORT = BuildConfig.SERVER_PORT;

    public static final Uri SSL_CERTIFICATE_URI = Uri.parse(BuildConfig.SSL_CERTIFICATE_URL);
    public static final Uri SSL_PRIVATE_KEY_URI = Uri.parse(BuildConfig.SSL_PRIVATE_KEY_URL);

    private BroadcastReceiver mReceiver = null;

    private final Map<String, UsbDevice> mDevices = new HashMap<>();
    private final NanoHTTPD mServer = new DaemonHTTPD(HOST, PORT, mDevices);
    private SSLServerSocketFactory mSSLSocketFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Service created");

        DownloadHelper.download(new DownloadHelper.Callback() {
            @Override
            public void onSuccess(Map<Uri, Response> responses) {
                final Reader certificateReader = new InputStreamReader(responses.get(SSL_CERTIFICATE_URI).body().byteStream());
                final Reader privkeyReader = new InputStreamReader(responses.get(SSL_PRIVATE_KEY_URI).body().byteStream());

                mSSLSocketFactory = SSLHelper.createFactory(certificateReader, privkeyReader);

                if (mSSLSocketFactory == null) {
                    stopSelf();

                    return;
                }

                startServer();
            }

            @Override
            public void onFailure(@Nullable Uri uri, IOException e) {
                Log.e(TAG, "Download failed for " + uri, e);

                stopSelf();

            }
        }, SSL_CERTIFICATE_URI, SSL_PRIVATE_KEY_URI);

        startForeground();
        registerReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Service called (startId is " + startId + ")");
        super.onStartCommand(intent, flags, startId);

        if (intent == null || !UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            Log.e(TAG, "Intent is missing or action is not ACTION_USB_DEVICE_ATTACHED");

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
        stopServer();
        unregisterReceiver();

        Log.v(TAG, "Service destroyed");
        super.onDestroy();
    }

    private void registerReceiver() {
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
                    }
                }
            };

            registerReceiver(mReceiver, filter);
        } else {
            Log.w(TAG, "Cannot register BroadcastReceiver, already registered");
        }
    }

    private void unregisterReceiver() {
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

    private void startServer() {
        if (mServer.isAlive()) {
            Log.w(TAG, "Server had already been started");

            return;
        }

        try {
            mServer.makeSecure(mSSLSocketFactory, null);

            /* Shouldn't run as daemon thread */
            Log.v(TAG, "Starting HTTP server");
            mServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e) {
            Log.e(TAG, "Could not start server", e);
        }
    }

    private void stopServer() {
        if (!mServer.isAlive()) {
            Log.w(TAG, "Server was not alive");

            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Stopping HTTP server");

                /* Closing the socket causes android.os.NetworkOnMainThreadException */
                mServer.stop();
            }
        }).start();
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        Log.w(TAG, "Service binding not implemented");

        return null;
    }
}
