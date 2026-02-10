package org.cbc.filehandler;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
/*
 * This extension to java.io.File allows the default directory used to construct absolute path names to
 * be changed.
 */
public class File extends java.io.File{
    private static final long   serialVersionUID = 1L;
    private static       String workingDirectory = null;
    /*
     * name defines the directory to be used as the working directory. A value of null reinstates the one defined by
     *      java.io.File. Otherwise it must be an absolute path name to an existing directory.
     */
    static public void setWorkingDirectory(String name) throws IOException {
        
        if (name != null) {
            java.io.File fname = new java.io.File(name);
            
            if (!fname.exists())      throw new IOException("Working directory " + name + " does not exist");
            if (!fname.isAbsolute())  throw new IOException("Working directory " + name + " is not absolute");
            if (!fname.isDirectory()) throw new IOException("Working directory " + name + " is not a directory");
        }             
        workingDirectory = name;
    }
    /*
     * This can be called with file set to this for the class object. It is important that file methods are not 
     * called that are overriden be the File class extending java.io.class, to avoid an infinite loop.
     */
    static public String absolutePathFor(java.io.File file) { 
        /*
         * The following is done to ensure that the isAbsolute override is not called which will
         * result in a 
        */
        java.io.File fl = new java.io.File(file.getPath());
        
        if (!fl.isAbsolute() && File.workingDirectory != null) {            
            fl = new java.io.File(File.workingDirectory + (file.getParent() != null? java.io.File.separator + file.getParent() : ""), file.getName());
        } else {
            fl = new java.io.File(file.getParent(), file.getName());
        }
        return fl.getAbsolutePath();
    }    
    static public String absolutePathFor(String file) {  
        return absolutePathFor(new java.io.File(file));
    }    
    static public String absolutePathFor(String path, String name) {  
        return absolutePathFor(new java.io.File(path, name));
    }
    static public java.io.File absoluteFileFor(java.io.File file) {  
        return new java.io.File(absolutePathFor(file));
    }
    static public java.io.File absoluteFileFor(String file) {  

        return new java.io.File(absolutePathFor(new java.io.File(file)));
    }
    static public java.io.File absoluteFileFor(String path, String name) {  
        return new java.io.File(absolutePathFor(new java.io.File(path, name)));
    }
    public File(String file) {
            super(file);                    
    }
    public File(java.io.File fl) {
        super(fl.getPath());
    }
    @Override
    public java.io.File getAbsoluteFile() {
        return new java.io.File(File.absolutePathFor(this));
    } 
    @Override
    public String getAbsolutePath() { 
        return File.absolutePathFor(this);
    }
    @Override
    public String getCanonicalPath() throws IOException { 
        java.io.File fl = new java.io.File(File.absolutePathFor(this));
        return fl.getCanonicalPath();
    }
    @Override
    public java.io.File[] listFiles() {
        return File.absoluteFileFor(this).listFiles();
    }
    @Override
    public java.io.File[] listFiles(FileFilter filter) {
        return File.absoluteFileFor(this).listFiles(filter);
    }
    @Override
    public boolean isDirectory() {
        return File.absoluteFileFor(this).isDirectory();
    }
    @Override
    public boolean isFile() {
        return File.absoluteFileFor(this).isFile();
    }
}
