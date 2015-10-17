/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.filehandler;

import org.cbc.utils.system.Timer;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.ArrayList;

/**
 *
 * @author cclose
 */
public class FileTransfer {
    public class TransferDetails {
        private File      source;
        private boolean   copied;
        private long      size;
        private Date      copyStart;
        private Date      timestamp;
        private Exception error;
        private double time;

        public File getSource() {
            return source;
        }
        public boolean isCopied() {
            return copied;
        }
        public long getSize() {
            return size;
        }
        public Date getCopyStart() {
            return copyStart;
        }
        public Date getTimestamp() {
            return timestamp;
        }
        public Exception getError() {
            return error;
        }
        public double getTime() {
            return time;
        }
    }
    public class CopyResult {
        private File      source;
        private File      destination;	  
        private Date      copyStart;
        private Exception error     = null;
        private int       found     = 0;
        private int       copied    = 0;
        private int       errors    = 0;
        private double    findTime  = 0;
        private double    copyTime  = 0;
        private ArrayList<TransferDetails> details = new ArrayList<TransferDetails>();

        public File getSource() {
            return source;
        }
        public File getDestination() {
            return destination;
        }
        public Date getCopyStart() {
            return copyStart;
        }
        public Exception getError() {
            return error;
        }
        public int getFound() {
            return found;
        }
        public int getCopied() {
            return copied;
        }
        public int getErrors() {
            return errors;
        }
        public long getCopySize() {
            long size = 0;
            
            for (TransferDetails t : details) {
                if (t.copied) size += t.getSize();
            }
            return size;
        }
        public long getTotalSize() {
            long size = 0;
            
            for (TransferDetails t : details) {
                size += t.getSize();
            }
            return size;
        }
        public double getFindTime() {
            return findTime;
        }
        public double getCopyTime() {
            return copyTime;
        }
        public ArrayList<TransferDetails> getDetails() {
            return details;
        }
    }
    public class Filter implements FileFilter {
        private Date              since      = null;
        private ArrayList<String> extensions = null;
        private String            match      = null;
        private boolean           filesOnly  = true;

        public boolean accept(String name, long lastModified, boolean isDirectory) {
            if (since != null && lastModified < since.getTime()) return false;
            if (filesOnly && isDirectory) return false;
            if (extensions != null) {
                int i = name.lastIndexOf('.');

                if (i != 0) {
                    String extension = name.substring(i + 1);

                    if (!extensions.contains(extension)) return false;
                }
            }
            if (!isDirectory && match != null && !name.matches(match)) return false;

            return true;

        }
        public boolean accept(File pathname) {
            return accept(pathname.getName(), pathname.lastModified(), pathname.isDirectory());
        }
        public void setSince(Date since) {
            this.since = since;
        }
        public void setExtensions(ArrayList<String> extensions) {
            this.extensions = extensions;
        }
        public void setMatch(String match) {
            this.match = match;
        }
        public void setFilesOnly(boolean yes) {
            this.filesOnly = yes;
        }
    }
    public static void copyFile(File source, File dest) throws IOException {
        FileInputStream fi = new FileInputStream(source);
        FileChannel fic = fi.getChannel();
        
        MappedByteBuffer mbuf = fic.map(FileChannel.MapMode.READ_ONLY, 0, source.length());
        fic.close();
        fi.close();
        FileOutputStream fo = new FileOutputStream(dest);
        FileChannel foc = fo.getChannel();
        foc.write(mbuf);
        foc.close();
        fo.close();
    }
    public static void copyFileAndDate(File source, File dest) throws IOException {
        copyFile(source, dest);
        dest.setLastModified(source.lastModified());
    }
    public static void moveFile(File file, File directory) throws IOException {
        if (!file.renameTo(new File(directory + File.separator + file.getName())))
            throw new IOException("Can't move " + file.toString() + " to " + directory.toString());
    }
    public boolean isSame(File a, File b) {
        // When setting the lastModified time, there seems to be some rounding takes place. Consider times equal if they are within
        // 5 seconds of each other.

        return a.getName().equals(b.getName())                      &&
               Math.abs(b.lastModified() - a.lastModified()) < 5000 &&
               a.length() == b.length();

    }
    private static void makeDirectory(File directory, File target) throws IOException {
        if (!directory.exists()) {
            File parent = directory.getParentFile();

            if (parent != null)
                makeDirectory(parent, target);
            else
                throw new IOException("Make directory for " + target + " unable to find parent " + directory);

            directory.mkdir();
        }
    }
    public static void makeDirectory(File directory) throws IOException {
        makeDirectory(directory, directory);
    }
    public static void makeDirectory(String directory) throws IOException {
        makeDirectory(new File(directory));
    }
    public static File[] getFiles(File source, FileFilter filter) throws IOException {
        File list[] = source.listFiles(filter);
        
        if (!source.isDirectory())
            throw new IOException(source.getCanonicalPath() + " is not a directory");
        
        if (list != null) {
            Arrays.sort(
                    list,
                    new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                        }
                    });
        }
        return list;
    }
    public Filter getFilter() {
        return new Filter();
    }
    public static File[] getFiles(File source, Date since, ArrayList<String> extensions, boolean includeDirectories) throws IOException {
        Filter filter = new FileTransfer().getFilter();

        filter.setSince(since);
        filter.setFilesOnly(!includeDirectories);
        filter.setExtensions(extensions);

        return getFiles(source, filter);
    }
    public static File[] getFiles(File source, Date since, ArrayList<String> extensions) throws IOException {
        return getFiles(source, since, extensions, false);
    }
    public static File[] getFiles(File source, String regex, boolean includeDirectories) throws IOException {
        Filter filter = new FileTransfer().getFilter();

        filter.setMatch(regex);
        filter.setFilesOnly(!includeDirectories);

        return getFiles(source, filter);
    }
    public CopyResult copyFolder(File source, File dest, Date since, ArrayList<String> extensions, boolean onlyChanged) {
        Filter     filter = new Filter();
        Timer      timer  = new Timer();
        CopyResult result = new CopyResult();

        filter.setSince(since);
        filter.setExtensions(extensions);
        timer.setAutoReset(false);
        result.copyStart   = new Date();
        File[] files       = source.listFiles(filter);
        result.source      = source;
        result.destination = dest;
        result.findTime    = timer.getElapsed();

        if (!source.isDirectory()) {
            result.errors = 1;
            result.error  = new IOException("Source " + source + " is not a directory");
            return result;
        }
        try {
            makeDirectory(dest);
        } catch (IOException ex) {
            result.errors = 1;
            result.error  = ex;
            return result;
        }
        for (File file : files) {
            Timer           copyTimer = new Timer();
            TransferDetails copy      = new TransferDetails();

            copyTimer.reset();
            copy.source   = file;
            copy.copied   = false;
            copy.copyStart = new Date();
            result.found++;

            File destFile = new File(dest.getAbsolutePath() + File.separator + file.getName());

            if (!onlyChanged || !isSame(copy.source, destFile)) {
                try {
                    copyFileAndDate(copy.getSource(), destFile);
                    copy.copied = true;
                    result.copied++;
                } catch (IOException ex) {
                    result.errors++;
                    copy.error = ex;
                }
            }
            copy.time      = copyTimer.getElapsed();
            copy.size      = copy.getSource().length();
            copy.timestamp = new Date(copy.getSource().lastModified());
            result.getDetails().add(copy);
        }
        result.copyTime = timer.getElapsed(3);
        return result;
    }
    public void copyFolder(File source, File dest) throws IOException {
        copyFolder(source, dest, null, null, false);
    }
}
