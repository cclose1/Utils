/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.system;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author CClose
 */
public class DateFormatter {
    private boolean useLenient = true;
    private SimpleDateFormat formatter = new SimpleDateFormat();

    private static void addTimeFormat(StringBuilder format, String time) {
        int i = time.indexOf(':');
        /*
         * There is a time part, so count : to determine if min and sec format fields
         * are required. 
         */        
        format.append(format.length() == 0? "H" : " H");

        if (i != -1) {
            format.append(":m");

            if (time.indexOf(':', i + 1) != -1) {
                format.append(":s");
            }
        }
    }
    /*
     * Returns a potential format for parsing date. The format returned does not guarantee that date will
     * successfully parse with the returned format.
     *
     * Date is expected to be a date followed by optional time. The time can include minutes and seconds.
     *
     * The date fields are separated by the characters - or / but both cannot be used. If the first field is less than 
     * 3 characters, the first field is assumed to be day in month. If the second field is more than 3 characters
     * the month is assumed to be in alpha format.
     *
     * The following shows examples of dates and formats generated.
     *
     *   Date               Format
     *   01-May-14          d-MMM-y
     *   2014-May-14        yyyy-MMM-d
     *   1-6-14 14          d-M-y H
     *   1/7/14 14:02       d/M/y H:m
     *   2/8/2014 14:02:04  d/M/y H:m:s
     *
     * Note: 01-Mad-14 returns d-MMM-y and 01-Ma-14 returns d-M-y. However, these are not valid dates and 
     *       the parse will fail.
     */
    public static String getDateFormat(String date) {
        StringBuilder fmt = new StringBuilder();
        
        String[] flds = date.split("[-/]", 3);
        
        if (flds.length < 2) {
            /*
             * There is no date part so parse for time format.
             */
            addTimeFormat(fmt, flds[0]);
        } else {
            char    sep      = date.charAt(flds[0].length());
            String  month    = flds[1].length() == 3? "MMM" : "M";
            boolean dayFirst = flds[0].length() <= 2;
            
            fmt.append(dayFirst? "d" : "y");
            fmt.append(sep);
            fmt.append(month);
            fmt.append(sep);
            fmt.append(dayFirst? "y" : "d");
            
            flds = date.split(" ");
            
            if (flds.length == 2) addTimeFormat(fmt, flds[1]); 
        }        
        return fmt.toString();
    }
    public static Date parseDate(String date, boolean lenient) throws ParseException {
        SimpleDateFormat fm = new SimpleDateFormat(getDateFormat(date));
        
        fm.setLenient(lenient);
        return new SimpleDateFormat(getDateFormat(date)).parse(date);
    }
    public static Date parseDate(String date) throws ParseException {
        return new SimpleDateFormat(getDateFormat(date)).parse(date);
    }
    public static String format(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }
    public DateFormatter() {

    }
    public DateFormatter(String format) {
        formatter = new SimpleDateFormat(format);
    }
    public DateFormatter(String format, boolean lenient) {
        this(format);
        this.useLenient = lenient;
        formatter.setLenient(lenient);
    }
    public String format(Date date) {
        return formatter.format(date);
    }
    public Date parse(String date, String format) throws ParseException {
        SimpleDateFormat f = new SimpleDateFormat(format);
        
        f.setLenient(useLenient);
                
        return f.parse(date);
    }
    public Date parse(String date) throws ParseException {
        return formatter.parse(date);
    }
}
