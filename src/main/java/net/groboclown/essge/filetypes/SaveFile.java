package net.groboclown.essge.filetypes;

import net.groboclown.essge.UnpackedSaveGame;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The main save file.
 */
public class SaveFile {
    private final File file;
    private final static int BUFF_SIZE = 4096;

    public SaveFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return this.file;
    }

    public SaveGameInfo readSaveGameInfo() throws IOException, ParserConfigurationException, SAXException {
        try (ZipFile zip = new ZipFile(this.file)) {
            final ZipEntry entry = zip.getEntry("saveinfo.xml");
            if (entry == null) {
                return null;
            }
            return SaveGameInfo.parseXml(zip.getInputStream(entry));
        }
    }

    public Opened open(boolean persist) throws IOException {
        return new Opened(this.file, persist);
    }

    public static List<SaveFile> readFromSaveDir(final File saveDir) {
        List<SaveFile> ret = new ArrayList<>();
        File[] contents = saveDir.listFiles();
        if (contents == null) {
            return ret;
        }
        for (File f: contents) {
            if (f.isFile() && f.getName().endsWith(".savegame")) {
                ret.add(new SaveFile(f));
            }
        }
        return ret;
    }

    public static class Opened implements Closeable {
        private final File src;
        private File tmpdir;
        private List<File> contents;
        private boolean persist;
        private UnpackedSaveGame game;

        public Opened(File src, boolean persist) throws IOException {
            this.src = src;
            this.tmpdir = Files.createTempDirectory("eternity-ssge").toFile();
            this.persist = persist;
            this.contents = new ArrayList<>();
            try (ZipFile f = new ZipFile(this.src)) {
                for (Enumeration<? extends ZipEntry> e = f.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry = e.nextElement();
                    if (entry.isDirectory()) {
                        rmDir(this.tmpdir);
                        throw new IOException("bad save file");
                    }
                    File o = new File(this.tmpdir, entry.getName());
                    this.contents.add(o);
                    try (FileOutputStream w = new FileOutputStream(o)) {
                        InputStream in = f.getInputStream(entry);
                        copyStreams(in, w);
                    }
                }
            }
            this.game = new UnpackedSaveGame(this.tmpdir);
        }

        public void setPersistent(boolean persist) {
            ensureOpen();
            this.persist = persist;
        }

        public UnpackedSaveGame getSave() {
            ensureOpen();
            return this.game;
        }

        /**
         * Save the contents over the original save file, right now.
         */
        public void persist() throws IOException {
            ensureOpen();
            // Get the list of files to archive before the backup is made.
            final File alt = new File(this.tmpdir, "save.new");
            FileOutputStream out = new FileOutputStream(alt);
            ZipOutputStream zip = new ZipOutputStream(out);
            for (File f: this.contents) {
                ZipEntry e = new ZipEntry(f.getName());
                zip.putNextEntry(e);
                FileInputStream in = new FileInputStream(f);
                copyStreams(in, zip);
                zip.closeEntry();
            }
            zip.close();

            // To be super safe, move the original zip file into the temp directory.
            File backup = new File(this.tmpdir, "save.backup");
            moveFile(this.src, backup);
            moveFile(alt, this.src);

            // Ignore backup delete error.
            backup.delete();
        }

        private void ensureOpen() {
            if (this.tmpdir == null) {
                throw new RuntimeException("already closed");
            }
        }

        @Override
        public void close() throws IOException {
            if (this.persist && this.tmpdir != null) {
                persist();
            }
            rmDir(this.tmpdir);
            this.tmpdir = null;
        }
    }


    private static void rmDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }
            for (File f: files) {
                if (f.isFile()) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }

    private static void copyStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buff = new byte[BUFF_SIZE];
        int len;
        while ((len = in.read(buff, 0, BUFF_SIZE)) > 0) {
            out.write(buff, 0, len);
        }
    }

    private static void moveFile(File src, File dest) throws IOException {
        if (dest.exists()) {
            if (!dest.delete()) {
                throw new IOException("failed to remove destination");
            }
        }
        if (!src.renameTo(dest)) {
            // On some systems, when crossing hardware drives, it can cause a failure.
            try (FileInputStream in = new FileInputStream(src)) {
                try (FileOutputStream out = new FileOutputStream(dest)) {
                    copyStreams(in, out);
                }
            }
            if (!src.delete()) {
                throw new IOException("failed to remove source after copy");
            }
        }
    }
}
