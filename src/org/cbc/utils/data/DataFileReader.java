/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.utils.data;

import org.cbc.utils.system.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author CClose
 */
public class DataFileReader extends DataStream {

    private boolean endOfFile = true;
    protected File file = null;
    private BufferedReader reader = null;
    
    //private char separator = ',';
    int fieldIndex = 0;

    private String nextLine() {
        String line = null;

        try {
            while ((line = reader.readLine()) != null && line.trim().length() == 0) {
                count++;
            }
            count++;
        } catch (IOException ex) {
            log.fatalError(ex);
        }
        fieldIndex = 0;
        endOfFile = line == null;
        return line;
    }

    private String nextField(String line) {
        int start = fieldIndex;
        char sep = ',';
        String field = null;

        while (start < line.length() && (sep = line.charAt(start++)) == ' ');

        if (sep == '"') {
            fieldIndex = start;
        } else {
            sep = ',';
            start = fieldIndex;
        }

        if (fieldIndex < line.length()) {
            fieldIndex = line.indexOf(sep, fieldIndex);

            if (fieldIndex != -1) {
                field = line.substring(start, fieldIndex).trim();
                fieldIndex++;
            } else {
                field = line.substring(start).trim();
                fieldIndex = line.length();
            }
        }
        return field;
    }
    
    public DataFileReader(File file, Logger logger) {
        super(logger);
        String field;
        String line = null;
        this.file = file;

        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException ex) {
            endOfFile = true;
            log.fatalError(ex);
            return;
        }
        line = nextLine();

        if (endOfFile) {
            return;
        }
        while ((field = nextField(line)) != null) {
            addColumn(field, field);
        }
    }
    public void close() {
        try {
            reader.close();
        } catch (IOException ex) {
            log.fatalError(ex);
        }
    }

    @Override
    String reportPosition() {
        return file == null? "null" : file.getName() + '(' + count + ')';
    }

    public boolean endOfStream() {
        return this.endOfFile;
    }

    public boolean readRow() {
        String line = nextLine();
        String value;
        int column = 0;

        discard();
        
        if (endOfFile || line == null) {
            return false;
        }
        while ((value = nextField(line)) != null) {
            if (value.length() != 0) {
                try {
                    getColumn(column).setValue(value);
                } catch (Exception ex) {
                    reportError(ex);
                }
            }
            column++;
        }
        return true;
    }

}
