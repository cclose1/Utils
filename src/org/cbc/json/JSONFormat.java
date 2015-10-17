/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.json;

/**
 * Contains the details require to format the JSON data. The relevant parameters are level and depth.
 * 
 * Level is the recursion depth of the data and -1 is used to indicate that no formatting is performed and in this
 * event none of the methods in this class have any effect. The comments for the individual methods apply only in the
 * case that level is not -1.
 * 
 * Indent is the increment applied to the leading spaces for each level, i.e. the level indent is level * indent.
 * 
 * @author Chris
 */
public class JSONFormat {
    private int level  = -1;
    private int indent = 3;
    
    /**
     * Increases the recursion level by 1.
     */
    protected void enter() {
        if (level >= 0) level++;
    }
    /**
     * Decrements the recursion level by 1. The level will not be decremented below 0, although this is an error, i.e.
     * the error is suppressed.
     */
    protected void exit() {
        if (level < 0) return;
                
        if (--level < 0) level = 0;
    }
    /**
     * Adds a newline to buffer and adds the indent for the next line.
     * 
     * @param buffer
     */
    protected void startLine(StringBuilder buffer) {
        int spaces = level * indent;
        
        if (level < 0 ) return;
        
        buffer.append('\n');
        
        while (spaces-- > 0) buffer.append(' ');
    }
    /**
     * Creates a new format and sets level to 0 if enabled and -1 otherwise.
     * 
     * @param enabled
     */
    public JSONFormat(boolean enabled) {
        if (enabled) level = 0;
    }
    /**
     * Creates a new format with formatting disabled.
     */
    public JSONFormat() {
        this(false);
    }
}
