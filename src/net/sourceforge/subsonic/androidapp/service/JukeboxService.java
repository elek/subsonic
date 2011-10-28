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
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class JukeboxService {

    private static final String TAG = JukeboxService.class.getSimpleName();

    private final BlockingQueue<Callable<?>> tasks = new LinkedBlockingQueue<Callable<?>>();
    private final DownloadServiceImpl downloadService;

    // TODO: Create shutdown method?
    // TODO: Call getLicense earlier
    // TODO: Gain control
    // TODO: Excessive cpu usage?
    // TODO: Landscape mode

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
                tasks.take().call();
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

    public void skip(final int index) {
        tasks.add(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                getMusicService().skipJukebox(index, downloadService, null);
                return null;
            }
        });
    }

    private MusicService getMusicService() {
        return MusicServiceFactory.getMusicService(downloadService);
    }

    private class UpdatePlaylistTask implements Callable<Void> {

        private final List<String> ids;

        public UpdatePlaylistTask(List<String> ids) {
            this.ids = ids;
        }

        @Override
        public Void call() throws Exception {
            getMusicService().updateJukeboxPlaylist(ids, downloadService, null);
            Log.d(TAG, "Updated jukebox with " + ids.size() + " songs.");
            return null;
        }

    }
}
