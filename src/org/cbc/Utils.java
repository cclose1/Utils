/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cbc;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 *
 * @author cclose
 */
public class Utils {
    
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
}
