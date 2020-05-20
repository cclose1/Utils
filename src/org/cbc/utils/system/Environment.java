/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.utils.system;

import java.util.Properties;
import org.cbc.application.reporting.Report;

/**
 *
 * @author chris
 * 
 * Returns externally defined values which can be provided by environment variables or a Properties object.
 */
public class Environment {
    private Properties properties = null;
    private String     source     = null;
    
    private void error(String name, String value, String message) {
        Report.error(null, source + ' ' + name + " value=" + value + ' ' + message);
    }
    /*
     * Values will be obtained from environment variable.
     */
    public Environment() {
        source = "Env variable";
    }
    /*
     * Values will be obtained from properties. If properties is null, properties object is created.
     */
    public Environment(Properties properties) {
        this.source     = "Property";        
        this.properties = properties == null? new Properties() : properties;
    }
    public String getValue(String name) {
        String value = null;
        
        if (properties == null) { 
            value = System.getenv(name);
            
            if (value != null && value.startsWith("${")) {
                value = System.getenv(value.substring(2, value.length() - 1));
            }
        } else {
            value = properties.getProperty(name);
        }
        return value;
    }
    public String getValue(String name, String defaultValue) {
        String value = getValue(name);
        
        return value == null? defaultValue : value;
    }
    public int getIntValue(String name, int defaultValue) {
        String value = getValue(name);

        if (value == null) return defaultValue;
       
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            error(name, value, "is not an integer defaulting to " + defaultValue);
        }
        return defaultValue;
    }
    public double getDoubleValue(String name, double defaultValue) {
        String value = getValue(name);

        if (value == null) return defaultValue;
       
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            error(name, value, "is not a double, defaulting to " + defaultValue);
        }
        return defaultValue;
    }
    public boolean getBooleanValue(String name, boolean ifNull) {
        String value = getValue(name).trim();
            
        if (value.equals("")) return ifNull;
            
        value = value.toLowerCase();
            
        return value.equals("true") || value.equals("t") || value.equals("yes") || value.equals("y");
    }
    public boolean getBooleanValue(String name) {
        return getBooleanValue(name, false);
    }
}
