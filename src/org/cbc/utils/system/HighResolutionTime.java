/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.utils.system;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author CClose
 */
public class HighResolutionTime {
    private double milliSeconds = -1;
    private Date timestamp = null;
    
    public HighResolutionTime(String s) {
        XMLGregorianCalendar xmlCalendar;
        GregorianCalendar    calendar;
        
        if (s == null || s.contains("0001-01-01T00:00:00")) {
            return;
        }
        try {
            xmlCalendar  = DatatypeFactory.newInstance().newXMLGregorianCalendar(s);
            calendar     = xmlCalendar.toGregorianCalendar();
            timestamp    = calendar.getTime();
            milliSeconds = (double) timestamp.getTime();
            
            /*
             * Extract the microseconds from the 7 digit fractional seconds following the decimal point.
             * Ignore failures as the microsecond precision is probably not achievable.
             */
            try {
                int i = s.indexOf('.');
                
                if (i != -1) {
                    milliSeconds += Double.parseDouble(s.substring(i + 4, i + 8)) / 10000;
                }                
            } catch (NumberFormatException ef) {
                
            }
        } catch (DatatypeConfigurationException ex) {
            System.err.println("Cannot instantiate XML date time parser.");
        }
    }
    public HighResolutionTime(long seconds, long microSeconds) {
        milliSeconds = 1000.0 * seconds + microSeconds / 1000.0;
        timestamp = new Date((long) milliSeconds);
    }
    public boolean isNull() {
        return timestamp == null;
    }
    public Date getDate() {
        return timestamp;
    }
    public double getTimeMillis() {
        return milliSeconds;
    }
    public String getFormatted(String format) {
        return (isNull())? "" : new SimpleDateFormat(format).format(timestamp);
    }
    public String getFormatted() {
        return getFormatted("yyyy-MM-dd HH:mm:ss.SSS");
    }
    public double elapsed(HighResolutionTime fromTime) {
        if (isNull() || fromTime.isNull()) return -1;
        
        return (getTimeMillis() - fromTime.getTimeMillis()) / 1000.0;
    }
}
