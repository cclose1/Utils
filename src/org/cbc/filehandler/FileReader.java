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
import java.io.FileOutputStream;
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
    public  enum   SourceType {Zip, Rar, Std};
    private static Logger              log               = new Logger();
    private        ZipFile             zip               = null;
    private        Archive             rar               = null;
    private        java.io.File        std               = null;
    private        FileTransfer.Filter filter            = new FileTransfer().getFilter();
    private        boolean             expandZip         = true;
    private        boolean             expandDirectories = false;
    private        ModifyFile          modifyFile        = null;
    private        SourceType          sourceType;

    /*
     * This sets the working directory for org.cbc.filehandler.File. The following absolute method could be
     * directly called, but have been added as a convenience, allowing the user of this class to avoid using
     * the org.cbc.filehandler.File class.
     */
    static public void setWorkingDirectory(String name) throws IOException {
         org.cbc.filehandler.File.setWorkingDirectory(name);
    }
    public java.io.File absoluteFileFor(String name) {
        return org.cbc.filehandler.File.absoluteFileFor(name);
    }
    public java.io.File absoluteFileFor(String path, String name) {
        return org.cbc.filehandler.File.absoluteFileFor(path, name);
    }
    public java.io.File absoluteFileFor(java.io.File file) {
        return org.cbc.filehandler.File.absoluteFileFor(file);
    }
    public String absolutePathFor(String name) {
        return org.cbc.filehandler.File.absolutePathFor(name);
    }
    public String absolutePathFor(String path, String name) {
        return org.cbc.filehandler.File.absolutePathFor(path, name);
    }
    public String absolutePathFor(java.io.File file) {
        return org.cbc.filehandler.File.absolutePathFor(file);
    }
    public FileReader() {
        
    }        
    public FileReader(ModifyFile modFile) {
        super();
        modifyFile = modFile;
    }
    public interface ModifyFile {
        String changeName(String path, String name);
    }    
    public static void inputStreamToFile(InputStream inputStream, java.io.File file) throws IOException {        
        try (FileOutputStream outputStream = new FileOutputStream(org.cbc.filehandler.File.absolutePathFor(file), false)) {
            int    read;
            byte[] bytes = new byte[4096];
            
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }    
    public static void inputStreamToFile(InputStream inputStream, String file) throws IOException {
        inputStreamToFile(inputStream, new java.io.File(file));
    }   
    public static void inputStreamToFile(InputStream inputStream, String path, String name) throws IOException {
        inputStreamToFile(inputStream, new java.io.File(path, name));
    }
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
    public class FileName {
        String     path = "";
        String     name = "";
        long       time        = 0;
        boolean    isDirectory = false;
        
        FileName(String path, String name) {
            this.path = path;
            this.name = name;
        }
        FileName(String fullname) {
            String fullName = fullname.replace('/', java.io.File.separatorChar);
            int    i        = fullName.lastIndexOf(java.io.File.separatorChar);

            if (i != -1) {
                path = fullName.substring(0, i);
                name = fullName.substring(i + 1);
                
            }
        }
        FileName(ZipEntry file) {
            this(file.getName());
            this.isDirectory = file.isDirectory();
            this.time        = file.getTime();
        }
        FileName(FileHeader file) {
            this(file.getFileNameString());
            this.isDirectory = file.isDirectory();
            this.time        = file.getMTime().getTime();
        }
        FileName(java.io.File file) {
            this(file.getParent(), file.getName());
            this.isDirectory = file.isDirectory();
            this.time        = file.lastModified();
        }
        public String getPath() {
            return path;
        }
        public String getName() {
            return name;
        }
        public String getFilePath() {
            if (path == null) return name;
            
            return path + java.io.File.separator + name;
        }
        public boolean isDirectory() {
            return isDirectory;
        }
        public void setPath(String path) {
            this.path = path;
        }
        public void setName(String name) {
            this.name = name;
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
                    for (java.io.File listFile : file.listFiles()) {
                        loadStats(listFile);
                    }
                }
                depth--;                    
            }
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
        private java.io.File stdFile     = null;        ;

        private ZipEntry     zipFile     = null;
        private FileHeader   rarFile     = null;
        private InputStream  inputStream = null;
        private FileName     fullName    = null;
        
        protected File(ZipEntry file) {
            zipFile    = file;
            fullName   = new FileName(file);
            sourceType = SourceType.Zip;
                        
            if (modifyFile != null) fullName.name = modifyFile.changeName(fullName.getPath(), fullName.getName());
        }
        protected File(java.io.File file) {
            this.stdFile = file;
            
            fullName   = new FileName(file);
            sourceType = SourceType.Std;
        }
        protected File(FileHeader file) {
            rarFile    = file;
            fullName   = new FileName(rarFile.getFileNameString());
            sourceType = SourceType.Rar;
        }
        public File(String name) {
            this(new org.cbc.filehandler.File(name));            
        }
        public boolean isFileSystem() {
            return stdFile != null;
        }
        public java.io.File getStdFile() {
            return stdFile;
        }         
        public FileName getFullName() {
            return fullName;
        }
        public String getName() {
            return fullName.name;
        }
        public String getPath() {
            return fullName.path;
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
        public SourceType getSourceType() {
            return sourceType;
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
    public void setStdSource(String fileName) {
            std = new java.io.File(fileName);
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
    private void loadFiles(ArrayList<File> files, org.cbc.filehandler.File directory) throws IOException {
        java.io.File[] list = FileTransfer.getFiles(directory, filter);

        if (list != null) {
            for (java.io.File f : list) {
                org.cbc.filehandler.File fl = new org.cbc.filehandler.File(f);
                
                if (f.isDirectory() && expandDirectories)
                    loadFiles(files, fl);
                else
                    files.add(new File(fl));
            }
        }
    }
    private void openSource(String file) throws IOException {
        String fl = file.toLowerCase();
        
        zip = null;
        rar = null;
        std = null;
        
        if (fl.endsWith(".zip")) {
            setZipSource(file);
            expandZip = true;
            
        } 
        else if (fl.endsWith(".rar"))
            setRarSource(file);
        else 
            setStdSource(file);
    }
    public ArrayList<File>getFiles(String source, boolean typeFromExtension) throws IOException {
        ArrayList<File> files = new ArrayList<>();
        FileName        fSource;
        File            file;
        
        source = absolutePathFor(source);
                
        if (typeFromExtension) openSource(source);
       
        if (zip != null) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
               ZipEntry ze = e.nextElement();
               fSource     = new FileName(ze);
       
               if (!fSource.isDirectory()) {
                   file = new File(ze);
                           
                   if (filter.accept(file.fullName)) {
                       files.add(file);
                   }
               }
            }
        } else if (rar != null) {
            for (FileHeader h : rar.getFileHeaders()) {
                fSource = new FileName(h);

               if (!fSource.isDirectory()) {
                   file =new File(h);
                   
                   if ((fSource.path.equalsIgnoreCase(source) || expandDirectories) && filter.accept(file.fullName)) {
                       files.add(new File(h));
                   }
               }
               System.out.println(h.getFileNameString() + " w " + h.getFileNameW());
            }
        } else
            loadFiles(files, new org.cbc.filehandler.File(source));
        
        return files;
    }    
    public void close() throws IOException {
        if (zip != null) {
            zip.close();
            zip = null;
        }
        if (rar != null) {
            rar.close();
            rar = null;
        }
        if (std != null) std = null;
    }
}
