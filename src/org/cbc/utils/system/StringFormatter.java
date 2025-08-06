/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cbc.utils.system;

import java.util.Date;
import org.cbc.Utils;

/**
 *
 * @author chris
 */
public class StringFormatter  {
    private StringBuffer string    = new StringBuffer();
    private String       separator = "";
    
    public void clear() {
        string.setLength(0);
    }
    public String getString() {
        return string.toString();
    }
    public StringFormatter(String separator) {
        this.separator = separator;
    }
    public void add(String field) {
        if (separator.length() > 0 && string.length() > 0) string.append(separator);
        
        string.append(field);
    }
    public void add(double field, int places) {
        add(Utils.format(field, places));
    }
    public void add(Date field, String format) {
        add(DateFormatter.format(field, format));
    }
    public void add(Date field) {
        add(DateFormatter.format(field, "dd-MMM-yyy HH:mm"));
    }
}
