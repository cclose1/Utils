package org.cbc.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cbc.application.reporting.Report;
import org.cbc.filehandler.FileOutput;
import org.cbc.json.JSONArray;
import org.cbc.json.JSONException;
import org.cbc.json.JSONObject;
import org.cbc.json.JSONReader;
import org.cbc.json.JSONValue;
import org.cbc.sql.SQLSelectBuilder;
import org.cbc.utils.data.DatabaseSession;
import org.cbc.utils.system.DateFormatter;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Chris
 */
public class Main {
    static private class LogValues implements FileOutput.FieldValue {
        private FileOutput   logFile;
        private java.io.File file;
        
        public LogValues(String logFileName) throws IOException {
            this.logFile = new FileOutput(logFileName, " ");
            this.logFile.openFile(logFileName);
            this.logFile.setValueProvider(this);
        }
        public void setFile(java.io.File file) {
            this.file = file;
        }
        public void addColumn(String id, int size) {
            logFile.addColumn(id, size);
        }
        @Override
        public FileOutput.Value getValue(String id) {
            FileOutput.Value value = null;
            
            switch (id) {
                case "Class":
                    value = logFile.new Value(file.getClass().getName());
                    break;
                case "Parent":
                    value = logFile.new Value(file.getParent());
                    break;
                case "Name":
                    value = logFile.new Value(file.getName());
                    break;
                case "AbsPath":
                    value = logFile.new Value(file.getAbsolutePath());
                    break;                    
            }
            return value;
        }
        public void outputFields(java.io.File file) throws IOException {
            this.file = file;
            logFile.addFields();
        }
    }
    private static class CompDetails {
        public FileOutput report = null;
        public FileClass  jav    = new FileClass();
        public FileClass  cbc    = new FileClass();
        
        private class FileClass  implements FileOutput.FieldValue {
            /*
            public String        className;
            public String        path;
            public String        parent;
            public String        name;
            public String  absPath;
            public boolean isAbs;   
            public boolean isDir;            
            public boolean isFile;
            public boolean exists;
            */
            public java.io.File file;
            
            public void setProperties(java.io.File file) throws IOException {
                report.setValueProvider(this);
                this.file = file;
                /*
                className = file.getClass().getName();
                path      = file.getPath();
                parent    = file.getParent();
                name      = file.getName();
                absPath   = file.getAbsolutePath();
                isAbs     = file.isAbsolute();
                isDir     = file.isDirectory();
                isFile    = file.isFile();
                exists    = file.exists();
                */
                if (report != null) {
                    report.addFields();
                }
            }
            @Override
            public FileOutput.Value getValue(String id) {
                FileOutput.Value value = report.new Value("Id " + id + " not implemented");
                
                switch (id) {
                    case "Class":
                        value = report.new Value(file.getClass().getName());
                        break;
                  
                    case "Path":
                        value = report.new Value(file.getPath());
                        break;
                }
                switch (id) {
                    case "Parent":
                        value = report.new Value(file.getParent());
                        break;
                }
                switch (id) {
                    case "Name":
                        value = report.new Value(file.getName());
                        break;
                }
                switch (id) {
                    case "Abs":
                        value = report.new Value(file.isAbsolute());
                        break;
                }
                switch (id) {
                    case "Dir":
                        value = report.new Value(file.isDirectory());
                        break;
                }
                switch (id) {
                    case "File":
                        value = report.new Value(file.isFile());
                        break;
                }
                switch (id) {
                    case "Exists":
                        value = report.new Value(file.exists());
                        break;
                }
                switch (id) {
                    case "AbsPath":
                        value = report.new Value(file.getAbsolutePath());
                        break;
                }
                return value;
            }
        }
        public void setReportFile(String file) throws IOException {
            report = new FileOutput(file, " ");
            report.openFile(file, new Date());
            report.addColumn("Class",   25);
            report.addColumn("Path",    15);
            report.addColumn("Parent",  15);
            report.addColumn("Name",    15);           
            report.addColumn("Abs",     3);            
            report.addColumn("Dir",     3);        
            report.addColumn("File",    4);        
            report.addColumn("Exists",  7);
            report.addColumn("AbsPath", 25); 
        }
        public void log(java.io.File jav, org.cbc.filehandler.File cbc) throws IOException {
            this.jav.setProperties(jav);
            this.cbc.setProperties(cbc);
        }
        public void log(java.io.File jav) throws IOException {
            log(jav, new org.cbc.filehandler.File(jav));
        }
        public void closeReport() throws IOException {
            if (report != null) {
                report.close();
                report = null;
            }            
        }
    }
    static private void compare(CompDetails cd, String path, String name, String workingDirectory) throws IOException {
        org.cbc.filehandler.File.setWorkingDirectory(workingDirectory);
        cd.log(new java.io.File(path, name));
    };

    private static void readFile(String file) throws FileNotFoundException, JSONException {
        JSONReader r = new JSONReader(new File(file));
        JSONValue  v;
        JSONReader.Token t;
        
        while ((t = r.next()) != null) {
            System.out.println(t.toString());
        }
        v = JSONValue.load(new File(file));
        System.out.println(v.toString());
    }
    public static void testJSON() {
        try {
            readFile("C:\\MyFiles\\My Documents\\AgeConcern\\LoadCRM.txt");
            JSONObject json = new JSONObject();
            JSONObject obj1;
            JSONArray arr1;
            JSONValue val;
            System.out.println(json.toString());
            json.add("a", new JSONValue(100));
            json.add("b", new JSONValue(new JSONArray()));
            obj1 = json.add("c", new JSONObject());
            arr1 = json.add("d", new JSONArray());
            arr1.add(new JSONValue(1));
            arr1.add(new JSONValue(1.2e4));
            arr1.add(new JSONValue("12e34", true));
            arr1.add(new JSONValue(12e34));
            obj1.add("a1", new JSONValue("str1"));
            obj1.add("a2", new JSONValue("str2\tx\\ / \" end"));

            for (JSONValue v : arr1) {
                System.out.println(v.getString());
            }
            val = obj1.get("a2");
            //       obj1.get("a2").getArray();
            System.out.println("Type " + val.getType().toString() + " value " + val.getString());
            System.out.println(json.toString());
        } catch (JSONException e) {
            System.out.println(e.getMessage());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    public static void testSQLBuilder() throws ParseException, SQLException {
        DatabaseSession  db  = new DatabaseSession("mysql", "127.0.0.1", "Expenditure");
        SQLSelectBuilder sql = new SQLSelectBuilder("", db.getProtocol());
        Date test = (new SimpleDateFormat("H:m:s")).parse("12:34:56");

        try {
            db.setUser("Test2", "Test2");
            db.connect();
            sql.addField("SeqNo");
            sql.addField("SessionId");
            sql.addField("UserId");
            sql.addField("UserName1", "UserId");            
            sql.addField("UserName2", sql.setFieldSource("UserId"), sql.setValue(""), sql.setCast("VARCHAR", 10));
            sql.addField("Last");
            sql.addField("Accesses");
            sql.addField("Deadlocks");
            sql.addField("MaxIdleTime");
            sql.setFrom("Session");
            sql.setWhere("SessionId = ?SessionId AND State = ?State AND P1 = ?Date AND P2 = ?Time AND P3 = ?TimeStamp AND P4 = ?Int");
            Report.comment(null, sql.build());
            sql.setParameter("SessionId", java.sql.Types.VARCHAR,   "Session1");
            sql.setParameter("State",     java.sql.Types.VARCHAR,   "Active");
            sql.setParameter("Date",      java.sql.Types.DATE,      "2014-08-31");
            sql.setParameter("Time",      java.sql.Types.TIME,      "12:34:56");
            sql.setParameter("TimeStamp", java.sql.Types.TIMESTAMP, "2014-08-31 12:34:56.1");
            sql.setParameter("Int",       java.sql.Types.INTEGER,   123);
            PreparedStatement st = db.getConnection().prepareStatement(sql.resolve(false));
            sql.setParameters(st);
            Report.comment(null, sql.resolve(false));
            sql.clear();
            sql.addField("SeqNo");
            sql.addField("SessionId");
            sql.addField("Last");
            sql.addField("Accesses");
            sql.setFrom("Session");
            sql.setWhere(""
                    + "SessionId <> ?SessionId AND "
                    + "Last      >= ?TimeStamp AND "
                    + "Accesses   = ?Accesses");
            sql.setParameter("SessionId", java.sql.Types.VARCHAR,   "ZxFCrSAr-0wNbg");
            sql.setParameter("TimeStamp", java.sql.Types.TIMESTAMP, "2013-08-31 12:34:56.2");
            sql.setParameter("Accesses",  java.sql.Types.INTEGER,   9);
            st = db.getConnection().prepareStatement(sql.resolve(false));
            sql.setParameters(st);
            Report.comment(null, sql.resolve(false));
            ResultSet rs = st.executeQuery();
            DatabaseSession.log(rs, 31);
        } catch (SQLException ex) {
            Report.error(null, ex);
        } catch (ParseException ex) {
            Report.error(null, ex);
        }
    }
    private static void testDate(String date) {
        try {
            Report.comment(null, DateFormatter.getDateFormat(date) + ' ' + DateFormatter.parseDate(date));
        } catch (ParseException ex) {
            Report.error(date, ex);
        }
    }
    private static void testDate(String date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        
        try {
            LocalDateTime dt = LocalDateTime.parse(date, formatter);

            Object d = formatter.parse(date);
            Report.comment(null, d.toString());
        } catch (Exception ex) {
            Report.error(date, ex);
        }
    }
    private static void testCbcFile() {        
        try {
            CompDetails cd = new CompDetails();
            cd.setReportFile("C:/Logs/Test/File!Date.log");
        
            compare(cd, null, "TestDirOrFl", "C:/Test/CbcIo/");
            compare(cd,  null, "C:/TestDir", "C:/Test/CbcIo/");
            cd.closeReport();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    static private void logFilesList(LogValues log, java.io.File file) throws IOException {
        java.io.File list[] = file.listFiles();
        
        for (java.io.File fl : list) {
            log.outputFields(fl);
        }
    }
    static private void testList() throws IOException {
        LogValues log = new LogValues("C:/Logs/Test/FilesList!Date.log");
        log.addColumn("Class",   15);
        log.addColumn("Parent",  20);
        log.addColumn("Name",    20);
        log.addColumn("AbsPath", 50);
        org.cbc.filehandler.File.setWorkingDirectory("C:/Test/CbcIo/");
        logFilesList(log, new java.io.File("Data"));
        logFilesList(log, new org.cbc.filehandler.File("Data"));        
    }
    public static void main(String[] args) throws SQLException, IOException {
//        testCbcFile();
        testList();
        testDate("01-08-02");
        testDate("2014-08-02");
        testDate("01-08-2002");
        testDate("2001-08-2002");
        testDate("2020-08x28 23:12:12", "yyyy-MM-dd HH:mm:ss");
        testDate("2020-08-28 23:12:12", "yyyy-MM-dd HH:mm:ss");
        testDate("2020-08-36 23:12:12", "yyyy-MM-dd HH:mm:ss");
        testJSON();
        try {
            testSQLBuilder();
        } catch (ParseException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
