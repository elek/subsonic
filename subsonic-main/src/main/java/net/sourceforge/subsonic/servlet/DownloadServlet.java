package net.sourceforge.subsonic.servlet;

import net.sourceforge.subsonic.*;
import net.sourceforge.subsonic.domain.*;
import net.sourceforge.subsonic.service.*;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

import javax.servlet.http.*;
import java.io.*;
import java.util.zip.*;

/**
 * A servlet used for downloading files to a remote client. If the requested path refers to a file, the
 * given file is downloaded.  If the requested path refers to a directory, the entire directory (including
 * sub-directories) are downloaded as an uncompressed zip-file.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.7 $ $Date: 2006/01/08 17:29:14 $
 */
public class DownloadServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(DownloadServlet.class);

    /**
     * Handles the given HTTP request.
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @throws IOException If an I/O error occurs.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DownloadStatus status = null;
        try {
            status = new DownloadStatus();
            status.setPlayer(ServiceFactory.getPlayerService().getPlayer(request, response, false, false));
            ServiceFactory.getStatusService().addDownloadStatus(status);

            String path = request.getParameter("path");
            String playlistName = request.getParameter("playlist");
            String playerId = request.getParameter("player");

            if (path != null) {
                File file = new File(path);
                if (!ServiceFactory.getSecurityService().isReadAllowed(file)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                if (file.isFile()) {
                    downloadFile(response, status, file);
                } else {
                    downloadDirectory(response, status, file);
                }

            } else if (playlistName != null) {
                Playlist playlist = new Playlist();
                ServiceFactory.getPlaylistService().loadPlaylist(playlist, playlistName);
                downloadPlaylist(request, response, status, playlist);

            } else if (playerId != null) {
                Player player = ServiceFactory.getPlayerService().getPlayerById(playerId);
                Playlist playlist = player.getPlaylist();
                playlist.setName("Playlist");
                downloadPlaylist(request, response, status, playlist);
            }


        } finally {
            if (status != null) {
                ServiceFactory.getStatusService().removeDownloadStatus(status);
            }
        }
    }

    /**
     * Downloads a single file.
     * @param response The HTTP response.
     * @param status The download status.
     * @param file The file to download.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadFile(HttpServletResponse response, DownloadStatus status, File file) throws IOException {
        LOG.info("Starting to download '" + file + "' to " + status.getPlayer());
        status.setFile(new MusicFile(file));

        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
        response.setContentLength((int) file.length());

        copyFileToStream(file, response.getOutputStream(), status);
        LOG.info("Downloaded '" + file + "' to " + status.getPlayer());
    }

    /**
     * Downloads all files in a directory (including sub-directories). The files are packed together in an
     * uncompressed zip-file.
     * @param response The HTTP response.
     * @param status The download status.
     * @param file The file to download.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadDirectory(HttpServletResponse response, DownloadStatus status, File file) throws IOException {
        String zipFileName = file.getName() + ".zip";
        LOG.info("Starting to download '" + zipFileName + "' to " + status.getPlayer());
        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename=" + zipFileName);

        ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
        out.setMethod(ZipOutputStream.STORED);  // No compression.

        zip(out, file.getParentFile(), file, status);
        out.close();
        LOG.info("Downloaded '" + zipFileName + "' to " + status.getPlayer());
    }

    /**
     * Downloads all files in a playlist.  The files are packed together in an
     * uncompressed zip-file.
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param status The download status.
     * @param playlist The playlist to download.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadPlaylist(HttpServletRequest request, HttpServletResponse response, DownloadStatus status, Playlist playlist) throws IOException {
        String zipFileName = playlist.getName().replaceAll("(\\.m3u)|(\\.pls)", "") + ".zip";
        LOG.info("Starting to download '" + zipFileName + "' to " + status.getPlayer());
        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename=" + zipFileName);

        ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
        out.setMethod(ZipOutputStream.STORED);  // No compression.

        MusicFile[] musicFiles = playlist.getFiles();
        for (MusicFile musicFile : musicFiles) {
            zip(out, musicFile.getParent().getFile(), musicFile.getFile(), status);
        }

        out.close();
        LOG.info("Downloaded '" + zipFileName + "' to " + status.getPlayer());
    }

    /**
     * Utility method for writing the content of a given file to a given output stream.
     * @param file The file to copy.
     * @param out The output stream to write to.
     * @param status The download status.
     * @throws IOException If an I/O error occurs.
     */
    private void copyFileToStream(File file, OutputStream out, DownloadStatus status) throws IOException {
        LOG.info("Downloading " + file + " to " + status.getPlayer());

        InputStream in = new BufferedInputStream(new FileInputStream(file), 8192);
        try {
            byte[] buf = new byte[8192];
            while (true) {
                int n = in.read(buf);
                if (n == -1) {
                    break;
                }
                out.write(buf, 0, n);
                status.setBytesStreamed(status.getBytesStreamed() + n);
            }
        } finally {
            in.close();
        }
    }

    /**
     * Writes a file or a directory structure to a zip output stream. File entries in the zip file are relative
     * to the given root.
     * @param out The zip output stream.
     * @param root The root of the directory structure.  Used to create path information in the zip file.
     * @param file The file or directory to zip.
     * @param status The download status.
     * @throws IOException If an I/O error occurs.
     */
    private void zip(ZipOutputStream out, File root, File file, DownloadStatus status) throws IOException {
        String zipName = file.getCanonicalPath().substring(root.getCanonicalPath().length() + 1);

        if (file.isFile()) {
            status.setFile(new MusicFile(file));

            ZipEntry zipEntry = new ZipEntry(zipName);
            zipEntry.setSize(file.length());
            zipEntry.setCompressedSize(file.length());
            zipEntry.setCrc(computeCrc(file));

            out.putNextEntry(zipEntry);
            copyFileToStream(file, out, status);
            out.closeEntry();

        } else {
            ZipEntry zipEntry = new ZipEntry(zipName + '/');
            zipEntry.setSize(0);
            zipEntry.setCompressedSize(0);
            zipEntry.setCrc(0);

            out.putNextEntry(zipEntry);
            out.closeEntry();

            File[] children = file.listFiles();
            for (File child : children) {
                zip(out, root, child, status);
            }
        }
    }

    /**
     * Computes the CRC checksum for the given file.
     * @param file The file to compute checksum for.
     * @return A CRC32 checksum.
     * @throws IOException If an I/O error occurs.
     */
    private long computeCrc(File file) throws IOException {
        CRC32 crc = new CRC32();
        InputStream in = new FileInputStream(file);

        try {

            byte[] buf = new byte[8192];
            int n = in.read(buf);
            while (n != -1) {
                crc.update(buf, 0, n);
                n = in.read(buf);
            }

        } finally {
            in.close();
        }

        return crc.getValue();
    }
}