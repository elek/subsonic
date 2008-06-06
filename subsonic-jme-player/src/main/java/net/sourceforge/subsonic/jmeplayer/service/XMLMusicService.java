package net.sourceforge.subsonic.jmeplayer.service;

import net.sourceforge.subsonic.jmeplayer.domain.Index;
import net.sourceforge.subsonic.jmeplayer.domain.MusicDirectory;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/**
 * @author Sindre Mehus
 */
public class XMLMusicService implements MusicService {

    private final MusicServiceDataSource dataSource;
    private final IndexParser indexParser;
    private final MusicDirectoryParser musicDirectoryParser;
    private final Vector readers = new Vector(10);

    public XMLMusicService(MusicServiceDataSource dataSource) {
        this.dataSource = dataSource;
        indexParser = new IndexParser();
        musicDirectoryParser = new MusicDirectoryParser();
    }

    public Index[] getIndexes() throws Exception {
        Reader reader = dataSource.getIndexesReader();
        addReader(reader);
        try {
            return indexParser.parse(reader);
        } finally {
            closeReader(reader);
        }
    }

    public MusicDirectory getMusicDirectory(String path) throws Exception {
        Reader reader = dataSource.getMusicDirectoryReader(path);
        addReader(reader);
        try {
            return musicDirectoryParser.parse(reader);
        } finally {
            closeReader(reader);
        }
    }

    private synchronized void addReader(Reader reader) {
        readers.addElement(reader);
    }

    private synchronized void closeReader(Reader reader) {
        try {
            reader.close();
        } catch (IOException x) {
            x.printStackTrace();
        }
        readers.removeElement(reader);
    }

    public synchronized void interrupt() {
        while (!readers.isEmpty()) {
            Reader reader = (Reader) readers.elementAt(readers.size() - 1);
            closeReader(reader);
        }
    }
}
