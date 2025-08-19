/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cbc;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;

/**
 *
 * @author cclose
 */
public class Utils {
    private static final int SECS_IN_DAY = 24 * 60 *60;
    
    public static String format(float value, int places) {
        String fs = "##0.";
        
        while (places > 0) {
            fs += '0';
            places--;
        }
        NumberFormat f = new DecimalFormat(fs);
        
        return f.format((double)value);
    }
    public static String splitToWords(String source, String separator) {
        StringBuilder split = new StringBuilder();
        char          ch;
        
        for (int i = 0; i < source.length(); i++) {
            ch = source.charAt(i);
            
            if (Character.isUpperCase(ch) && i != 0) {
                split.append(separator);
            }            
            split.append(i == 0? Character.toUpperCase(ch) : ch);
        }
        return split.toString();
    }    
    public static String splitToWords(String source) {
        return splitToWords(source, " ");
    }
    public static double round(double value, int places) {
        int mult = (int)Math.pow(10, places);
        
        return Math.round(value * mult) / (1.0* mult);        
    }
    /*
     * Time is of the hh[:mm:[ss]].
     */
    @SuppressWarnings("fallthrough")
    public static int toSeconds(String time) throws ParseException {
        String fields[];
        int    seconds  = 0;
        int    mult     = 60 * 60;
        int    maxField = 23;
        int    sign     = 1;

        time = time.trim();
        
        if (time.length() > 0) {
            char s = time.charAt(0);
            
            switch (s) {
                case '-':
                    sign = -1;
                case '+':
                    time = time.substring(1);
                    break;
            }
        }
        fields = time.split(":");
        
        if (fields.length > 3) throw new ParseException("On " + time + " too many fields", fields.length);
        
        for (String field : fields) {
            int d = Integer.parseInt(field);

            if (d > maxField) throw new ParseException("On " + time + " time field larger than " + maxField, 0);
            
            maxField = 59;
            seconds += mult * d;
            mult     = mult / 60;
        }
        return sign * seconds;
    }
    /*
     * Sets the time part of date to 00:00:00.
     */
    public static void zeroTime(Date date) {
        date.setTime(1000 * SECS_IN_DAY * (date.getTime() / (1000 * SECS_IN_DAY)));
    }
    public static void setTime(Date date, String time) throws ParseException {
        zeroTime(date);
        date.setTime(date.getTime() + 1000 * toSeconds(time));
    }
    public static void addSeconds(Date date, int seconds) {
        date.setTime(date.getTime() + 1000 * seconds);
    }
    public static void addDays(Date date, int days) {
        addSeconds(date, 24 * 60 * 60 * days);
    }
    /*
     * Returns value as string formatted by DecimalNumber format.
     */
    public static String format(double value, String format) {
        DecimalFormat df = new DecimalFormat(format);

        return df.format(value);
    }
    /* 
     * Rounds value to places decimal precision and returns it formatted by "0.n0" where n0 is replaced by places 0's.
     * e.g if places is 2 format string is "0.00".
     */
    public static String format(double value, int places) {
        String fmt = places == 0? "0" : "0.";
        value = Utils.round(value, places);
        
        while (places > 0) {
            fmt += "0";
            places--;
        }
        return format(value, fmt);
    }
    public static String lpad(String text, int size) {
        while (text.length() < size) text = ' ' + text;
        
        return text;
    }
    public static String rpad(String text, int size) {
        while (text.length() < size) text += ' ';
        
        return text;
    }
    /*
     * This a workaround for dates that can be java.sql.Timesatamp or java.Util.Date.
     *
     * The standard date comparison operators may note, e.g. a.equals(b) can return false
     * when in fact they are equal.
     *
     * java.Util.Date is known to be problematic and java.Util.Time should be used.
     */
    public static boolean compare(Date a, String test, Date b) throws ParseException {
        long at = a.getTime();
        long bt = b.getTime();

        switch (test) {
            case "=":
                return at == bt;
            case "<=":
                return at <= bt;
            case "<":
                return at < bt;
            case ">=":
                return at >= bt;
            case ">":
                return at > bt;
            case "!=":
            case "<>":
                return at != bt;
            default:
                throw new ParseException("Comparison operator " + test + " not supported", 0);
        }
    }
}
