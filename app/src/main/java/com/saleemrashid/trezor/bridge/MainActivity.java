package com.saleemrashid.trezor.bridge;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.saleemrashid.trezor.bridge.daemon.DaemonService;
import com.saleemrashid.trezor.bridge.helpers.CustomTabsHelper;

import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final Uri WALLET_URI = Uri.parse("https://wallet.trezor.io/");

    private CustomTabsSession mSession = null;
    private CustomTabsServiceConnection mConnection = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Activity created");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FloatingActionButton openWalletButton = (FloatingActionButton) findViewById(R.id.open_wallet_button);
        openWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWallet();
            }
        });

        bindCustomTabs();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "Activity destroyed");

        unbindCustomTabs();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "Activity resumed");

        final Intent intent = getIntent();
        if (intent != null) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                Log.v(TAG, "Received ACTION_USB_DEVICE_ATTACHED");

                /* Forward intent to daemon */
                final Intent daemonIntent = new Intent(intent);
                daemonIntent.setClass(this, DaemonService.class);

                startService(daemonIntent);
            }
        }

        super.onResume();
    }

    /**
     * Bind Custom Tabs and warm up URI.
     */
    private void bindCustomTabs() {
        if (mConnection == null) {
            Log.v(TAG, "Binding to Custom Tabs Service");

            mConnection = new CustomTabsServiceConnection() {
                @Override
                public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                    Log.v(TAG, "Connected to Custom Tabs Service");

                    client.warmup(0);

                    mSession = client.newSession(null);
                    mSession.mayLaunchUrl(WALLET_URI, null, null);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.v(TAG, "Disconnected from Custom Tabs Service");

                    mSession = null;
                }
            };

            CustomTabsClient.bindCustomTabsService(this, CustomTabsHelper.getPackageNameToUse(this), mConnection);
        } else {
            Log.w(TAG, "Cannot bind to Custom Tabs Service, already bound");
        }
    }

    private void unbindCustomTabs() {
        if (mConnection != null) {
            Log.v(TAG, "Unbinding from Custom Tabs Service");

            unbindService(mConnection);

            mSession = null;
            mConnection = null;
        } else {
            Log.w(TAG, "Cannot unbind from Custom Tabs Service, not bound");
        }
    }

    /**
     * Open wallet URI with Custom Tabs.
     */
    private void openWallet() {
        Log.v(TAG, "Opening wallet using Custom Tabs");

        final CustomTabsIntent intent = new CustomTabsIntent.Builder(mSession)
                .setToolbarColor(ContextCompat.getColor(this, R.color.primary))
                .setShowTitle(true)
                .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_close_button))
                .build();

        intent.launchUrl(this, WALLET_URI);
    }
}
