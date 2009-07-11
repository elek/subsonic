/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.*;
import net.sourceforge.subsonic.android.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author Sindre Mehus
 */
public class DownloadService extends Service {

    private static final String TAG = DownloadService.class.getSimpleName();
    private final IBinder binder = new DownloadBinder();

    private final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(10);

    public DownloadService() {
        Log.i(TAG, "Constructor");
        new DownloadThread().start();
    }

    public void download(String url) {
        showNotification();
        Toast.makeText(this, "Added " + url + " to download queue.", Toast.LENGTH_SHORT).show();

        queue.add(url);
        Log.i(TAG, "Download queue size: " + queue.size());
    }

    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        String text = "Download queue: " + (queue.size() + 1);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(android.R.drawable.ic_media_play, text,
                                                     System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        // TODO
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, DownloadService.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "Subsonic is downloading", text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.NOTIFICATION_ID_DOWNLOAD_QUEUE, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: " + intent);
        return binder;
    }

    @Override
    public void onDestroy() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.NOTIFICATION_ID_DOWNLOAD_QUEUE);
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    private class DownloadThread extends Thread {
        @Override
        public void run() {
            while (true) {
                String url = null;
                try {
                    url = queue.take();
                    downloadToFile(url);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to download " + url);
                }
            }
        }

        private void downloadToFile(String url) throws Exception {
            Log.i(TAG, "Starting to download " + url);
            File file = File.createTempFile("subsonic", null);
            InputStream in = null;
            FileOutputStream out = null;
            try {
                in = new URL(url).openStream();
                out = new FileOutputStream(file);
                long n = Util.copy(in, out);
                Log.i(TAG, "Downloaded " + n + " bytes to " + file);

                // TODO: Allowed in this thread?
                Toast.makeText(DownloadService.this, "Finished downloading " + url + ".", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Failed to download stream.", e);
            } finally {
                Util.close(in);
                Util.close(out);
            }
        }
    }
}
