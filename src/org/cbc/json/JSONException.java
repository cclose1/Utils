/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.json;

/**
 *
 * @author Chris
 */
public class JSONException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public JSONException(int index, String message) {
        super((index < 0? "" : "At " + index + " ") +  message);
    }    
    public JSONException(String message) {
        this(-1, message);
    }    
    public JSONException(JSONValue value, String message) {
        super("Value is type " + value.getType().toString() + ", " + message);
    }  
    public JSONException(String key, String message) {
        super("Key " + key + " " + message);
    }
}
