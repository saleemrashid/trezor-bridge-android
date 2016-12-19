package com.saleemrashid.trezor.bridge;

import android.content.ComponentName;
import android.graphics.BitmapFactory;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final Uri WALLET_URI = Uri.parse("https://wallet.trezor.io/");

    private CustomTabsSession mSession;
    private CustomTabsServiceConnection mConnection;

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
    protected void onResume() {
        super.onResume();

        if (getIntent().getAction() != null && getIntent().getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            Log.v(TAG, "USB device attached");

            final Intent intent = new Intent(this, DaemonService.class);
            intent.putExtra(UsbManager.EXTRA_DEVICE, getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE));
            startService(intent);
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "Activity destroyed");

        unbindCustomTabs();

        super.onDestroy();
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
