package com.saleemrashid.trezor.bridge.helpers;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Helper for downloading a list of files and failing unless all are successfully downloaded.
 */
public class DownloadHelper {
    private static final String TAG = DownloadHelper.class.getSimpleName();

    /* Prevent instantiation */
    private DownloadHelper() {}

    public static void download(final Callback callback, final Request... requests) {
        Log.v(TAG, "Starting downloads");

        final Handler handler = new Handler();
        final OkHttpClient client = callback.build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Map<Uri, Response> responses = new HashMap<>();

                for (final Request request : requests) {
                    final Uri uri = Uri.parse(request.url().toString());

                    Log.v(TAG, "Downloading " + uri);

                    final Response response;
                    try {
                        response = client.newCall(request).execute();
                    } catch (final IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.w(TAG, "Could not download " + uri);

                                callback.onFailure(uri, e);
                            }
                        });

                        return;
                    }

                    Log.i(TAG, "Downloaded " + uri);
                    responses.put(uri, response);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Downloads complete");

                        callback.onSuccess(responses);
                    }
                });
            }
        }).start();
    }

    public static void download(final Callback callback, final Uri... uris) {
        final Request[] requests = new Request[uris.length];

        for (int i = 0; i < uris.length; i++) {
            requests[i] = new Request.Builder()
                    .url(uris[i].toString())
                    .build();
        }

        download(callback, requests);
    }

    public static abstract class Callback {
        public abstract void onSuccess(final Map<Uri, Response> responses);

        public abstract void onFailure(@Nullable final Uri uri, final IOException e);

        public OkHttpClient build() {
            return new OkHttpClient();
        }
    }
}
