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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class JukeboxService {

    private static final String TAG = JukeboxService.class.getSimpleName();

    private final BlockingQueue<JukeboxTask> tasks = new LinkedBlockingQueue<JukeboxTask>();
    private final DownloadServiceImpl downloadService;
    private int position;

    // TODO: Create shutdown method?
    // TODO: Gain control
    // TODO: Excessive cpu usage?
    // TODO: Landscape mode
    // TODO: Shuffle play
    // TODO: Change gui for toggling?
    // TODO: Progress support.
    // TODO: Read regular status from server. Create "status" REST action.
    // TODO: Method to remove tasks of certain type from queue?
    // TODO: Schedule status task right after other tasks.

    public JukeboxService(DownloadServiceImpl downloadService) {
        this.downloadService = downloadService;
        new Thread() {
            @Override
            public void run() {
                processTasks();
            }
        }.start();
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
        tasks.add(new UpdatePlaylistTask(ids));
    }

    public void skip(final int index, final int offsetSeconds) {
        tasks.add(new SkipTask(index, offsetSeconds));
    }

    public void stop() {
        tasks.add(new StopTask());
    }

    public void start() {
        tasks.add(new StartTask());
    }

    private MusicService getMusicService() {
        return MusicServiceFactory.getMusicService(downloadService);
    }

    public int getPosition() {
        return position;
    }


    private interface JukeboxTask {
        void execute() throws Exception;
    }

    private class UpdatePlaylistTask implements JukeboxTask {

        private final List<String> ids;
        UpdatePlaylistTask(List<String> ids) {
            this.ids = ids;
        }

        @Override
        public void execute() throws Exception {
            getMusicService().updateJukeboxPlaylist(ids, downloadService, null);
            Log.d(TAG, "Updated jukebox with " + ids.size() + " songs.");
        }
    }

    private class SkipTask implements JukeboxTask {
        private final int index;
        private final int offsetSeconds;

        public SkipTask(int index, int offsetSeconds) {
            this.index = index;
            this.offsetSeconds = offsetSeconds;
        }

        @Override
        public void execute() throws Exception {
            getMusicService().skipJukebox(index, offsetSeconds, downloadService, null);
        }
    }

    private class StopTask implements JukeboxTask {
        @Override
        public void execute() throws Exception {
            getMusicService().stopJukebox(downloadService, null);
        }
    }

    private class StartTask implements JukeboxTask {
        @Override
        public void execute() throws Exception {
            getMusicService().startJukebox(downloadService, null);
        }
    }
}
