package com.saleemrashid.trezor.bridge;

import android.content.ComponentName;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

    /**
     * Bind Custom Tabs and warm up URI.
     */
    private void bindCustomTabs() {
        final CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                Log.i(TAG, "onCustomTabsServiceConnected");

                client.warmup(0);

                mSession = client.newSession(null);
                mSession.mayLaunchUrl(WALLET_URI, null, null);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "onServiceDisconnected");

                mSession = null;
            }
        };

        CustomTabsClient.bindCustomTabsService(this, CustomTabsHelper.getPackageNameToUse(this), connection);
    }

    /**
     * Open wallet URI with Custom Tabs.
     */
    private void openWallet() {
        Log.i(TAG, "openWallet");

        final CustomTabsIntent intent = new CustomTabsIntent.Builder(mSession)
                .setToolbarColor(ContextCompat.getColor(this, R.color.primary))
                .setShowTitle(true)
                .setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left)
                .setExitAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .build();

        intent.launchUrl(this, WALLET_URI);
    }
}
