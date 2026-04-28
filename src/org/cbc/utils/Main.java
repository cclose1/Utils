package org.cbc.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.cbc.Utils.rpad;
import org.cbc.filehandler.FileOutput;
import org.cbc.json.JSONArray;
import org.cbc.json.JSONException;
import org.cbc.json.JSONObject;
import org.cbc.json.JSONReader;
import org.cbc.json.JSONTable;
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
    static private int testNo = 0;
    static private class LogValues implements FileOutput.TableSource {
        private FileOutput   logFile;
        private java.io.File file;
        
        public LogValues(FileOutput logFile) throws IOException {
            this.logFile = logFile;
            this.logFile.setTableSource(this);
        }
        public void setFile(java.io.File file) {
            this.file = file;
        }
        public void addColumn(String id, int size) {
            logFile.addColumn(id, size);
        }
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
        @Override
        public void setColumns(FileOutput log) {        
            log.addColumn("Class",   25);
            log.addColumn("Parent",  15);
            log.addColumn("Name",    15);  
            log.addColumn("AbsPath", -25);    
        }
        public void outputFields(java.io.File file) throws IOException {
            this.file = file;
            logFile.addTableRow();
        }
    }
    private static class CompDetails {
        public FileOutput report = null;
        public FileClass  file   = new FileClass();
        
        private class FileClass  implements FileOutput.TableSource {
            public java.io.File file;
            
            public void logFile(java.io.File file) throws IOException {
                this.file = file;
                if (report != null) {
                    report.addTableRow();
                }
            }
            public FileOutput.Value getValue(String id) {
                FileOutput.Value value = report.new Value("Id " + id + " not implemented");
                
                switch (id) {
                    case "Class":
                        value = report.new Value(file.getClass().getName());
                        break;                  
                    case "Path":
                        value = report.new Value(file.getPath());
                        break;
                    case "Parent":
                        value = report.new Value(file.getParent());
                        break;
                    case "Name":
                        value = report.new Value(file.getName());
                        break;
                    case "Abs":
                        value = report.new Value(file.isAbsolute());
                        break;
                    case "Dir":
                        value = report.new Value(file.isDirectory());
                        break;
                    case "File":
                        value = report.new Value(file.isFile());
                        break;
                    case "Exists":
                        value = report.new Value(file.exists());
                        break;
                    case "AbsPath":
                        value = report.new Value(file.getAbsolutePath());
                        break;
                }
                return value;
            }
            @Override
            public void setColumns(FileOutput log) {
                log.addColumn("Class",    25);
                log.addColumn("Path",     20);
                log.addColumn("Parent",   15);
                log.addColumn("Name",     15);
                log.addColumn("Abs",      3);
                log.addColumn("Dir",      3);
                log.addColumn("File",     4);
                log.addColumn("Exists",   7);
                log.addColumn("AbsPath", -25);
            }
        }
        public CompDetails(FileOutput log) {
            report = log;
            report.setTableSource(file);
        }
        public void log(java.io.File jav, org.cbc.filehandler.File cbc) throws IOException {
            this.file.logFile(jav);
            this.file.logFile(cbc);
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

    private static void readFile(String file, FileOutput report) throws FileNotFoundException, JSONException, IOException {
        JSONReader r = new JSONReader(new File(file));
        JSONValue  v;
        JSONReader.Token t;
        
        while ((t = r.next()) != null) {
            report.writeLine(t.toString());
        }
        v = JSONValue.load(new File(file));
        report.writeLine(v.toString());
    }
    public static void testJSON(FileOutput report) throws IOException {
        try {
            readFile("C:\\MyFiles\\My Documents\\AgeConcern\\LoadCRM.txt", report);
            JSONObject json = new JSONObject();
            JSONObject obj1;
            JSONArray arr1;
            JSONValue val;
            report.writeLine(json.toString());
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
                report.writeLine(v.getString());
            }
            val = obj1.get("a2");
            report.writeLine("Type " + val.getType().toString() + " value " + val.getString());
            report.writeLine(json.toString());
        } catch (JSONException e) {
            report.write(e);
        } catch (FileNotFoundException ex) {
            report.write(ex);
        }        
    }
    
    public static void testJSONTable(FileOutput report) throws IOException {
        try {
            JSONTable table = new JSONTable("Test");
            table.addColumn("Text1", JSONTable.ColumnType.Text,   12, 0);
            table.addColumn("Text2", JSONTable.ColumnType.Text,     5,   0);
            table.addColumn("Ts1",   JSONTable.ColumnType.DateTime, 5,   0);
            table.addColumn("Tim1",  JSONTable.ColumnType.Time,     5,   0);
            table.addColumn("Dec1",  JSONTable.ColumnType.Decimal , 6,   2);
            table.addColumn("Int",   JSONTable.ColumnType.Int,      5,   0);
            table.addRow();
            table.set("Text1", "Val1");
            table.set("Int", 1);
            table.set("Tim1", 1.5);
            report.writeLine(table.toString());
        } catch (JSONException e) {
            report.write(e);
        } catch (FileNotFoundException ex) {
            report.write(ex);
        }        
    }
    public static void testSQLBuilder(FileOutput report) throws ParseException, SQLException, IOException {
        DatabaseSession  db  = new DatabaseSession("mysql", "127.0.0.1", "Expenditure");
        SQLSelectBuilder sql = new SQLSelectBuilder("", db.getProtocol());

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
            report.add("SQL ");
            report.add(sql.build());
            report.writeLine();
            sql.setParameter("SessionId", java.sql.Types.VARCHAR,   "Session1");
            sql.setParameter("State",     java.sql.Types.VARCHAR,   "Active");
            sql.setParameter("Date",      java.sql.Types.DATE,      "2014-08-31");
            sql.setParameter("Time",      java.sql.Types.TIME,      "12:34:56");
            sql.setParameter("TimeStamp", java.sql.Types.TIMESTAMP, "2014-08-31 12:34:56.1");
            sql.setParameter("Int",       java.sql.Types.INTEGER,   123);
            PreparedStatement st = db.getConnection().prepareStatement(sql.resolve(false));
            sql.setParameters(st);      
            report.add("Resolve ");
            report.add(sql.resolve(false));
            report.writeLine();
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
            report.add("Resolve ");
            report.add(sql.resolve(false));
            report.writeLine();
            ResultSet rs = st.executeQuery();
            DatabaseSession.log(report, rs, 31);
        } catch (SQLException ex) {
            report.write(ex);
        } catch (ParseException ex) {
            report.write(ex);
        }
    }
    private static void testDate(FileOutput report, String date) throws IOException {
        String format = DateFormatter.getDateFormat(date);
        
        try {            
            report.writeLine("Text " + rpad(date, 20) + " format " + format + " result " + DateFormatter.parseDate(date));
        } catch (ParseException ex) {
            report.write(ex);
        }
    }
    private static void testDate(FileOutput report, String date, String format) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        
        try {
            Object d = formatter.parse(date);
            report.writeLine("Text " + rpad(date, 20) + " format " + format + " result " + d.toString());
        } catch (Exception ex) {
            report.write("Date " + date + " format " + format, ex);
        }
    }
    private static void testCbcFile(FileOutput log) {        
        try {
            CompDetails cd = new CompDetails(log);
            compare(cd, null, "TestDirOrFl", "C:/Test/CbcIo/");
            compare(cd,  null, "C:/TestDir", "C:/Test/CbcIo/");
            compare(cd,  "TestDirOrFl", "Folder1", "C:/Test/CbcIo/");
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
    static private void testList(FileOutput logFile) throws IOException {
        LogValues log = new LogValues(logFile);
        org.cbc.filehandler.File.setWorkingDirectory("C:/Test/CbcIo/");
        logFilesList(log, new java.io.File("Data"));
        logFilesList(log, new org.cbc.filehandler.File("Data"));        
    }
    private static void testDate(FileOutput log) throws IOException {        
        testDate(log, "01-08-02");
        testDate(log, "2014-08-02");
        testDate(log, "01-08-2002");
        testDate(log, "2001-08-2002");
        testDate(log, "2020-08x28 23:12:12", "yyyy-MM-dd HH:mm:ss");
        testDate(log, "2020-08-28 23:12:12", "yyyy-MM-dd HH:mm:ss");
        testDate(log, "2020-08-36 23:12:12", "yyyy-MM-dd HH:mm:ss");
    }
    private static void startTest(FileOutput file, String name) throws IOException {
        if (++testNo != 1) file.writeLine();
        
        file.clearColumns();
        file.writeLine("Test " + name);
        file.writeLine();
    }
    /*
     * Most of these tests are not of much use. There is no means of checking if the results are as they should be.
    */
    public static void main(String[] args) throws SQLException, IOException, ParseException {
        FileOutput log = new FileOutput("C:/Logs/Test/Utils!Date.log");
        startTest(log, "JSON Table");
        testJSONTable(log);
        if (log != null) return;
        startTest(log, "Cbc File");
        testCbcFile(log);
        startTest(log, "List Files");
        testList(log);
        startTest(log, "Test Date");
        testDate(log);
        startTest(log, "JSOn");
        testJSON(log);
        startTest(log, "SQLBuilder");
        testSQLBuilder(log);
        log.close();
    }
}
