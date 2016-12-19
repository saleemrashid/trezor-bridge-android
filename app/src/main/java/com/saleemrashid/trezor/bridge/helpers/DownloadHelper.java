package com.saleemrashid.trezor.bridge.helpers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Helper for downloading a list of files and failing unless all are successfully downloaded.
 */
public class DownloadHelper {
    private static final String TAG = DownloadHelper.class.getSimpleName();

    private final Callback mCallback;
    private final List<Uri> mUris = new ArrayList<>();

    private Context mContext;
    private DownloadManager mDownloadManager;
    private final HandlerThread mHandlerThread;

    private long[] mIds;
    private final HashSet<Long> mIdsRemaining = new HashSet<>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (!mIdsRemaining.remove(id)) {
                return;
            }

            final Uri uri;
            final int status;

            Cursor cursor = mDownloadManager.query(new DownloadManager.Query().setFilterById(id));
            try {
                cursor.moveToFirst();

                uri = Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI)));
                status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            } finally {
                cursor.close();
            }

            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                Log.e(TAG, "Downloading " + uri + " failed, status is " + status);

                mDownloadManager.remove(mIds);

                callbackToUser(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onFailure(uri, status);
                    }
                });

                return;
            }

            Log.i(TAG, "Downloading " + uri + " completed");

            if (mIdsRemaining.isEmpty()) {
                Log.i(TAG, "No more downloads remaining");

                final Map<Uri, FileInputStream> streams = new HashMap<>();

                cursor = mDownloadManager.query(new DownloadManager.Query().setFilterById(mIds));

                try {
                    int columnId = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                    int columnUri = cursor.getColumnIndex(DownloadManager.COLUMN_URI);

                    while (cursor.moveToNext()) {
                        final Uri otherUri = Uri.parse(cursor.getString(columnUri));

                        final ParcelFileDescriptor fd;
                        try {
                            fd = mDownloadManager.openDownloadedFile(cursor.getInt(columnId));
                        } catch (FileNotFoundException e) {
                            callbackToUser(new Runnable() {
                                @Override
                                public void run() {
                                    /* DownloadManager never uses (status = 0) */
                                    mCallback.onFailure(null, 0);
                                }
                            });

                            return;
                        }

                        streams.put(otherUri, new FileInputStream(fd.getFileDescriptor()));
                    }

                    callbackToUser(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onComplete(streams);
                        }
                    });
                } finally {
                    cursor.close();
                }
            }
        }
    };

    private void callbackToUser(final Runnable runnable) {
        new Handler(mContext.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mContext.unregisterReceiver(mReceiver);
                mHandlerThread.quit();

                runnable.run();
            }
        });
    }

    public DownloadHelper(final Callback callback) {
        mCallback = callback;

        mHandlerThread = new HandlerThread(DownloadHelper.class.getSimpleName());
    }

    public void download(@NonNull final Context context) {
        Log.i(TAG, "Starting downloads");

        if (mContext != null) {
            throw new IllegalStateException("Method cannot be called multiple times");
        }

        mContext = context;

        if (mUris.isEmpty()) {
            Log.w(TAG, "No URIs provided");

            /* Follow normal usage from an external point of view but don't waste time */
            mCallback.onComplete(Collections.<Uri, FileInputStream>emptyMap());

            return;
        }

        mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        mHandlerThread.start();

        final IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        context.registerReceiver(mReceiver, filter, null, new Handler(mHandlerThread.getLooper()));

        int i = 0;
        mIds = new long[mUris.size()];

        for (final Uri uri : mUris) {
            long id = mDownloadManager.enqueue(mCallback.createRequest(uri));

            mIds[i++] = id;
            mIdsRemaining.add(id);
        }
    }

    public DownloadHelper add(final Uri... uris) {
        if (mContext != null) {
            throw new IllegalStateException("Method cannot be called after starting download");
        }

        mUris.addAll(Arrays.asList(uris));

        return this;
    }

    public static abstract class Callback {
        public abstract void onComplete(final Map<Uri, FileInputStream> streams);

        public abstract void onFailure(@Nullable final Uri uri, int status);

        public DownloadManager.Request createRequest(final Uri uri) {
            return new DownloadManager.Request(uri)
                    .setVisibleInDownloadsUi(false);
        }
    }
}
