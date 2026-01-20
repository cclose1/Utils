/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.filehandler;

import static com.sun.org.apache.xerces.internal.impl.io.UTF16Reader.DEFAULT_BUFFER_SIZE;
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
    private static String              classWorkingDir   = "";
    private        ZipFile             zip               = null;
    private        Archive             rar               = null;
    private        java.io.File        std               = null;
    private        FileTransfer.Filter filter            = new FileTransfer().getFilter();
    private        boolean             expandZip         = true;
    private        boolean             expandDirectories = false;
    private        ModifyFile          modifyFile        = null;
    private        SourceType          sourceType;
    protected      String              workingDirectory  = "";
    /*
     * Relative file paths are converted to full path name by prefixing with the current working directory which
     * the directory from which the code was executed from. This class allowes the working directory to be explicitly
     * set at the class level or the instance level.
     */
    private static void checkWorkingDirectory(String name) throws IOException {
        java.io.File fname = new java.io.File(name);
        
        if (!fname.exists())      throw new IOException("Working directory " + name + " does not exist");    
        if (!fname.isAbsolute())  throw new IOException("Working directory " + name + " is not absolute");
        if (!fname.isDirectory()) throw new IOException("Working directory " + name + " is not a directory");         
    }
    public static void setClassWorkingDirectory(String name) throws IOException {       
        checkWorkingDirectory(name);
        
        classWorkingDir = name;
    }
    protected static java.io.File getClassJavaFile(String file, String workingDirectory) {
        java.io.File f = new java.io.File(file);
        /*
         * If file is not absolute and a workingDirectory is set, convert to an absolute
         * path in the working directory.
         */
        if (!f.isAbsolute() && !"".equals(classWorkingDir)) {
            f = new java.io.File(workingDirectory, file);
        }
        return f;        
    }
    public static java.io.File getClassJavaFile(String file) {
        return getClassJavaFile(file, classWorkingDir);        
    }
    public void setWorkingDirectory(String name) throws IOException {       
        checkWorkingDirectory(name);
        
        workingDirectory = name;
    }
    public java.io.File getJavaFile(String file) {
        return "".equals(workingDirectory)? getClassJavaFile(file) : getClassJavaFile(file, workingDirectory);
    }
    public java.io.File getJavaFile(String path, String name) {
        String file = path + "\\" + name;
        return "".equals(workingDirectory)? getClassJavaFile(file) : getClassJavaFile(file, workingDirectory);
    }
    public FileReader() {
        
    }        
    public FileReader(ModifyFile modFile) {
        super();
        modifyFile = modFile;
    }
    public interface ModifyFile {
        boolean updateName(FileReader.File owner, FileName name);
    }
    public static void inputStreamToFile(InputStream inputStream, String file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int    read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);

            }
        }
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
        SourceType sourceTyp1e;
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
                    for (int i = 0; i < file.listFiles().length; i++) {
                        loadStats(file.listFiles()[i]);
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
        private java.io.File stdFile     = null;
        private ZipEntry     zipFile     = null;
        private FileHeader   rarFile     = null;
        private InputStream  inputStream = null;
        private String       root        = null;
        private FileName     fullName    = null;
        
        protected File(ZipEntry file) {
            zipFile    = file;
            fullName   = new FileName(file);
            sourceType = SourceType.Zip;
            
            if (modifyFile != null)  modifyFile.updateName(this, fullName);
        }
        protected File(String root, java.io.File file) {
            this.root     = root;
            this.stdFile  = file;
            
            fullName   = new FileName(file);
            sourceType = SourceType.Std;
        }
        protected File(FileHeader file) {
            rarFile    = file;
            fullName   = new FileName(rarFile.getFileNameString());
            sourceType = SourceType.Rar;
        }
        public File(String name) {
            this(null, new java.io.File(name));            
        }
        public boolean isFileSystem() {
            return stdFile != null;
        }
        public java.io.File getJavaFile() {
            return stdFile;
        }        
        public java.io.File getJavaFilex() {
            return FileReader.this.getJavaFile(fullName.getFilePath());           
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
    public ArrayList<File>getFiles(String directory, boolean typeFromExtension) throws IOException {
        ArrayList<File> files = new ArrayList<>();
        FileName        fSource;
        File            file;
        
        directory = getJavaFile(directory).getAbsolutePath();
                
        if (typeFromExtension) openSource(directory);
       
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
                   
                   if ((fSource.path.equalsIgnoreCase(directory) || expandDirectories) && filter.accept(file.fullName)) {
                       files.add(new File(h));
                   }
               }
               System.out.println(h.getFileNameString() + " w " + h.getFileNameW());
            }
        } else
            loadFiles(files, directory, new java.io.File(directory));
        
        return files;
    }    
    public ArrayList<File>getFiles(String directory) throws IOException {
        return getFiles(directory, false);
    }
    public ArrayList<File>getFiles(java.io.File directory) throws IOException {
        return getFiles(directory.getCanonicalPath());
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
