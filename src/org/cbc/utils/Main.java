package org.cbc.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cbc.application.reporting.Report;
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
    public static void testSQLBuilder() throws ParseException {
        DatabaseSession  db  = new DatabaseSession("sqlserver", "127.0.0.1", "Expenditure");
        SQLSelectBuilder sql = new SQLSelectBuilder();
        Date test = (new SimpleDateFormat("H:m:s")).parse("12:34:56");

        try {
            db.setUser("Test1", "Test1");
            db.connect();
            sql.addField("SeqNo");
            sql.addField("SessionId");
            sql.addField("UserId");
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
    public static void main(String[] args) {
        testDate("01-08-02");
        testDate("2014-08-02");
        testDate("01-08-2002");
        testDate("2001-08-2002");
        testJSON();
        /*
        try {
            testSQLBuilder();
        } catch (ParseException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
    }
}
