/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.filehandler;

import org.cbc.utils.system.Logger;
import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 * @author CClose changed again 19-Oct-15
 */
public class FileReader {
    private static Logger              log               = new Logger();
    private        ZipFile             zip               = null;
    private        Archive             rar               = null;
    private        FileTransfer.Filter filter            = new FileTransfer().getFilter();
    private        boolean             expandZip         = true;
    private        boolean             expandDirectories = false;

    /**
     * @return the expandZip
     */
    public boolean isExpandZip() {
        return expandZip;
    }

    /**
     * @param expandZip the expandZip to set
     */
    public void setExpandZip(boolean expandZip) {
        this.expandZip = expandZip;
    }
    private class JavaFileName {
        String path = "";
        String name = "";

        JavaFileName(String fullname) {
            String fullName = fullname.replace('/', java.io.File.separatorChar);
            int    i        = fullName.lastIndexOf(java.io.File.separatorChar);

            if (i != -1) {
                path = fullName.substring(0, i);
                name = fullName.substring(i + 1);
            }
        }
    }
    public static class DirectoryStats {
        private int  directories;
        private int  files;
        private int  depth;
        private int  maxDepth;
        private long totalFileSize;
        private long totalDirectorySize;
        
        private void loadStats(java.io.File file) {
            if (file.isFile()) {
                files         += 1;
                totalFileSize += file.length();
            } else {         
                totalDirectorySize += file.length();
                directories        += 1;
                depth              += 1;
                
                if (depth > maxDepth) maxDepth = depth;
                
                if (file.listFiles() == null) {
                    /*
                     * Not sure what this implies. For now do nothing.
                     */
                    log.comment("loadStats file " + file.getAbsolutePath() + " returned a null file list");
                } else {
                    for (int i = 0; i < file.listFiles().length; i++) {
                        loadStats(file.listFiles()[i]);
                    }
                }
            depth--;                    
            }
            return;
        }
        private DirectoryStats(java.io.File file) {
            loadStats(file);
        }
        /**
         * @return the directories
         */
        public int getDirectories() {
            return directories;
        }
        /**
         * @return the files
         */
        public int getFiles() {
            return files;
        }
        /**
         * @return the depth
         */
        public int getDepth() {
            return maxDepth;
        }
        /**
         * @return the totalFileSize
         */
        public long getTotalFileSize() {
            return totalFileSize;
        }
        /**
         * @return the totalDirectorySize
         */
        public long getTotalDirectorySize() {
            return totalDirectorySize;
        }
    }
    static public DirectoryStats getDirectoryStats(java.io.File directory) {
        return new DirectoryStats(directory);
    }
    public class File {
        private File() {
        }
        private java.io.File stdFile     = null;
        private ZipEntry     zipFile     = null;
        private FileHeader   rarFile     = null;
        private InputStream  inputStream = null;
        private String       root        = null;

        protected File(ZipEntry file) {
            zipFile = file;
        }
        protected File(String root, java.io.File file) {
            this.root    = root;
            this.stdFile = file;
        }
        protected File(FileHeader file) {
            rarFile = file;
        }
        public boolean isFileSystem() {
            return stdFile != null;
        }
        public java.io.File getFile() {
            return stdFile;
        }
        public String getName() {
            if (stdFile != null) return stdFile.getName();
            if (zipFile != null) return new JavaFileName(zipFile.getName()).name;
            if (rarFile != null) return new JavaFileName(rarFile.getFileNameString()).name;
            
            return null;
        }
        public String getRoot() {
            return root;
        }
        public String getRelativeName() throws IOException {
            if (root == null) return getName();
            
            return stdFile.getCanonicalPath().substring(root.length());
        }
        public InputStream open() throws IOException {
            if (stdFile != null) {
                if (expandZip && stdFile.getName().toLowerCase().endsWith(".zip")) {
                    ZipFile z = new ZipFile(stdFile);

                    if (z.size() != 1) throw new IOException("Zip file " + stdFile.getName() + " has " + z.size() + "entries");

                    inputStream = z.getInputStream(z.entries().nextElement());
                } else
                    inputStream = new FileInputStream(stdFile);
            } else if (zipFile != null) {
                inputStream = zip.getInputStream(zipFile);
            } else if (rarFile != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                try {
                    rar.extractFile(rarFile, out);
                    inputStream = new ByteArrayInputStream(out.toByteArray());
                } catch (RarException ex) {
                    throw new IOException("Unable to convert rar header " + rarFile.getFileNameString() + " to input stream-" + ex.getMessage());
                }
            } else {
                throw new IOException("No file available to open");
            }
            return inputStream;
        }
        public void close() throws IOException {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        }
    }
    public void setZipSource(String fileName) throws IOException {
        zip = new ZipFile(fileName);
    }
    public void setRarSource(String fileName) throws IOException {
        try {
            rar = new Archive(new java.io.File(fileName));
        } catch (RarException ex) {
            throw new IOException("Unable to open rar file " + fileName + '-' + ex.getMessage());

        }
    }
    public void setFilter(String regex) {
        filter.setMatch(regex);
    }
    public void setExpandDirectories(boolean yes) {
        filter.setFilesOnly(!yes);
        expandDirectories = yes;
    }
    public void setSince(Date timestamp) {
        filter.setSince(timestamp);
    }

    public void setSince(int days) {
        GregorianCalendar cal = new GregorianCalendar();

        cal.setTime(new Date());
        cal.add(GregorianCalendar.DAY_OF_MONTH, -days);
        setSince(cal.getTime());
    }
    private void loadFiles(ArrayList<File> files, String root, java.io.File directory) throws IOException {
        java.io.File[] list = FileTransfer.getFiles(directory, filter);

        if (list != null) {
            for (java.io.File f : list) {
                if (f.isDirectory() && expandDirectories)
                    loadFiles(files, root, f);
                else
                    files.add(new File(root, f));
            }
        }
    }
    public ArrayList<File>getFiles(String directory) throws IOException {
        ArrayList<File> files = new ArrayList<File>();
        
        if (zip != null) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
               ZipEntry ze = e.nextElement();
        
               if (!ze.isDirectory()) {
                   JavaFileName id = new JavaFileName(ze.getName());

                   if ((id.path.equalsIgnoreCase(directory) || expandDirectories) && filter.accept(ze.getName(), ze.getTime(), ze.isDirectory())) {
                       files.add(new File(ze));
                   }
               }
            }
        } else if (rar != null) {
            for (FileHeader h : rar.getFileHeaders()) {

               if (!h.isDirectory()) {
                   JavaFileName id = new JavaFileName(h.getFileNameString());

                   if ((id.path.equalsIgnoreCase(directory) || expandDirectories) && filter.accept(h.getFileNameString(), h.getATime().getTime(), h.isDirectory())) {
                       files.add(new File(h));
                   }
               }
               System.out.println(h.getFileNameString() + " w " + h.getFileNameW());
            }
        } else
            loadFiles(files, directory, new java.io.File(directory));
        
        return files;
    }
    public ArrayList<File> getFiles(java.io.File directory) throws IOException {
        return getFiles(directory.getCanonicalPath());
    }
    

    public void close() throws IOException {
        if (zip != null) {
            zip.close();
            zip = null;
        }
    }
}
