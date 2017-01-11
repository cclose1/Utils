/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 *
 * @author Chris
 */
public class JSONReader {
    StringReader sr;
    InputStream  is;
    int          index = -1;
    boolean      atEnd = false;
    
    public class Token {
        private char          separator;
        private StringBuilder value;
        private boolean       quoted;    
        private int           start;
        
        private Token() {
            separator = 0;
            value     = null;
            quoted    = false;
            start     = index;
        }

        /**
         * @return the separator
         */
        public char getSeparator() {
            return separator;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value == null? null : value.toString();
        }

        /**
         * @return the quoted
         */
        public boolean isQuoted() {
            return quoted;
        }

        /**
         * @return the start
         */
        public int getStart() {
            return start;
        }
        public String toString() {
            return "Start " + start + " separator " + separator + " value " + value;
        }
        private void add(char ch) {
            if (value == null) value = new StringBuilder();
                    
            value.append(ch);
        }
    }
    private int getChar() throws JSONException {
        try {
            int ch;
            
            if (sr != null)
                ch = sr.read();
            else if (is != null)
                ch = is.read();
            else
                ch = -1;
            
            if (ch != -1) 
                index++;
            else
                atEnd = true;
            
            return ch;
        } catch (IOException ex) {
            throw new JSONException(index, "IOException " + ex.getMessage());
        }       
    }
    public JSONReader(String jsonData) {
        sr = new StringReader(jsonData);
    }
    public JSONReader(StringReader jsonData) {
        sr = jsonData;
    }
    public JSONReader(InputStream jsonData) {
        is = jsonData;
    }
    public JSONReader(File jsonData) throws FileNotFoundException {
        is = new FileInputStream(jsonData);
    }
    public Token next(String allowed) throws JSONException {
        Token   t        = null;
        int     ch       = 0;
        boolean inString = false;
        
        if (atEnd) return null;
        
        while ((ch = getChar()) != -1) {
            if (inString) {
                if ((char)ch == '"') {
                    inString = false;
                    continue;
                }
                if ((char)ch == '\\') {
                    ch = getChar();

                    if (ch == -1) throw new JSONException("Escape character \\ is at end of text ");
                    
                    switch ((char)ch) {
                        case 'r':
                            ch = '\r';
                            break;
                        case 'n':
                            ch = '\n';
                            break;
                        case 't':
                            ch = '\t';
                            break;
                        case 'b':
                            ch = '\b';
                            break;
                        case 'f':
                            ch = '\f';
                            break;
                    }
                }
                t.add((char)ch);               
            } else if ("{}[],:".indexOf(ch) != -1) {
                if (t == null) t = new Token();
                
                t.separator = (char)ch;
                
                if (allowed != null && allowed.indexOf(ch) == -1)
                    throw new JSONException(index, (char)ch + " is not in the allowed list " + allowed);
                return t;
            } else {
                if ((char)ch == '"') {
                    if (t != null)
                        throw new JSONException(index, "Quoted string must have quote as first character");
                              
                    inString = true;
                    t        = new Token();
                    t.quoted = true;
                } else if (" \t\r\n\f".indexOf((char)ch) == -1) {
                    if (t == null) {
                        t = new Token();
                    }
                    if (t.isQuoted())
                        throw new JSONException(index, "Quoted string not allowed characters after closing quote");
                    
                    t.add((char)ch);
                }
            }            
        }
        if (inString)
            throw new JSONException(index, "Unterminated string starting at " + t.getStart());
        return t;
    }
    public Token next() throws JSONException {
        return next(null);
    }
}