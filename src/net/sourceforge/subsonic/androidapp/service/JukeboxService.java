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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class JukeboxService {

    private final BlockingQueue<Callable<?>> tasks = new LinkedBlockingDeque<Callable<?>>();
    private final DownloadServiceImpl downloadService;

    public JukeboxService(DownloadServiceImpl downloadService) {
        this.downloadService = downloadService;
    }

    public void updatePlaylist() {
        List<String> ids = new ArrayList<String>();
        for (DownloadFile file : downloadService.getDownloads()) {
            ids.add(file.getSong().getId());
        }
        tasks.add(new UpdatePlaylistTask(ids));
    }

    private class UpdatePlaylistTask implements Callable<Void> {

        private final List<String> ids;

        public UpdatePlaylistTask(List<String> ids) {
            this.ids = ids;
        }

        @Override
        public Void call() throws Exception {
            MusicService service = MusicServiceFactory.getMusicService(downloadService);
            service.updateJukeboxPlaylist(ids);
            return null;
        }
    }
}
