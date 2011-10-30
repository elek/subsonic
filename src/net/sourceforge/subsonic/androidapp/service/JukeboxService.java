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
package net.sourceforge.subsonic.androidapp.service;

import android.util.Log;
import net.sourceforge.subsonic.androidapp.domain.JukeboxStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class JukeboxService {

    private static final String TAG = JukeboxService.class.getSimpleName();
    private static final long STATUS_UPDATE_INTERVAL_SECONDS = 4L; // TODO

    private final TaskQueue tasks = new TaskQueue();
    private final DownloadServiceImpl downloadService;
    private final AtomicInteger positionSeconds = new AtomicInteger();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong sequenceNumber = new AtomicLong();
    private ScheduledFuture<?> statusUpdateFuture;


    // TODO: Create shutdown method?
    // TODO: Gain control
    // TODO: Excessive cpu usage?
    // TODO: Landscape mode
    // TODO: Shuffle play
    // TODO: Change gui for toggling?
    // TODO: Progress support.
    // TODO: Read regular status from server. Create "status" REST action.
    // TODO: Schedule status task right after other tasks.
    // TODO: Test shuffle.
    // TODO: Disable repeat.
    // TODO: Priority queue
    // TODO: Clean-up queue when adding tasks.
    // TODO: Estimate progress.
    // TODO: Persist RC state.
    // TODO: Minimize status updates.
    // TODO: Stop status updates when disabling jukebox.
    


    public JukeboxService(DownloadServiceImpl downloadService) {
        this.downloadService = downloadService;
        new Thread() {
            @Override
            public void run() {
                processTasks();
            }
        }.start();
    }

    private synchronized void startStatusUpdate() {
        stopStatusUpdate();
        Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        };
        statusUpdateFuture = executorService.scheduleWithFixedDelay(updateTask, 0L, STATUS_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void stopStatusUpdate() {
        if (statusUpdateFuture != null) {
            statusUpdateFuture.cancel(false);
            statusUpdateFuture = null;
        }
    }

    private void updateStatus() {
        try {
            long seqNo = sequenceNumber.incrementAndGet();
            JukeboxStatus status = getMusicService().getJukeboxStatus(downloadService, null);

            // Only update status if no other commands have been issued in the meantime.
            if (seqNo == sequenceNumber.get()) {
                positionSeconds.set(status.getPositionSeconds() == null ? 0 : status.getPositionSeconds());
            }
        } catch (Throwable x) {
            Log.e(TAG, "Failed to update jukebox status: " + x, x);
        }
    }

    private void processTasks() {
        while (true) {
            try {
                tasks.take().execute();
            } catch (Throwable x) {
                Log.e(TAG, "Failed to process jukebox task: " + x, x);
            }
        }
    }

    public void updatePlaylist() {
        List<String> ids = new ArrayList<String>();
        for (DownloadFile file : downloadService.getDownloads()) {
            ids.add(file.getSong().getId());
        }
        tasks.add(new SetPlaylist(ids));
    }

    public void skip(final int index, final int offsetSeconds) {
        startStatusUpdate();
        positionSeconds.set(offsetSeconds);
        tasks.add(new Skip(index, offsetSeconds));
    }

    public void stop() {
        stopStatusUpdate();
        tasks.add(new Stop());
    }

    public void start() {
        startStatusUpdate();
        tasks.add(new Start());
    }

    private MusicService getMusicService() {
        return MusicServiceFactory.getMusicService(downloadService);
    }

    public int getPositionSeconds() {
        return positionSeconds.get();
    }

    private static class TaskQueue extends LinkedBlockingQueue<JukeboxTask> {
        @Override
        public boolean add(JukeboxTask jukeboxTask) {
            boolean b = super.add(jukeboxTask);
            Log.d(TAG, "Task queue: " + toString());
            return b;
        }
    }

    private abstract class JukeboxTask {

        JukeboxTask() {
            sequenceNumber.incrementAndGet();
        }

        abstract void execute() throws Exception;

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    private class SetPlaylist extends JukeboxTask {

        private final List<String> ids;
        SetPlaylist(List<String> ids) {
            this.ids = ids;
        }

        @Override
        public void execute() throws Exception {
            getMusicService().updateJukeboxPlaylist(ids, downloadService, null);
            Log.d(TAG, "Updated jukebox with " + ids.size() + " songs.");
        }
    }

    private class Skip extends JukeboxTask {
        private final int index;
        private final int offsetSeconds;

        public Skip(int index, int offsetSeconds) {
            this.index = index;
            this.offsetSeconds = offsetSeconds;
        }

        @Override
        public void execute() throws Exception {
            getMusicService().skipJukebox(index, offsetSeconds, downloadService, null);
        }
    }

    private class Stop extends JukeboxTask {
        @Override
        public void execute() throws Exception {
            getMusicService().stopJukebox(downloadService, null);
        }
    }

    private class Start extends JukeboxTask {
        @Override
        public void execute() throws Exception {
            getMusicService().startJukebox(downloadService, null);
        }
    }

}
