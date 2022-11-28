/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.data;

import org.cbc.utils.system.HighResolutionTime;
import org.cbc.utils.system.Logger;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author CClose
 */
public class DataRow {
    private boolean headersClosed = false;
    
    protected Logger log = null;
    
    public DataRow(Logger logger) {
        log = logger;
    }
    public class ColumnsException extends RuntimeException {
        private String error;
        private static final long serialVersionUID = 42L;
        
        private ColumnsException(String error) {
            this.error = error;
        }
        public String toString() {
            return error;
        }
    }
    private ArrayList<DataField> columns = new ArrayList<>();

    public boolean canAddHeaders() {
        return !headersClosed;
    }
    public void closeHeaders() {
        headersClosed = true;
    }
        
    private DataField getColumn(String id, boolean errorNotFound, boolean forValue) {
        if (forValue) {
            if (!headersClosed) {
                closeHeaders();
                headersClosed = true;
            }
        } else {
            if (headersClosed) throw new Error("Attempt to add column " + id + " after headers closed");
        }
        for (DataField column : columns) {
            if (column.getId().equals(id)) {
                return column;
            }
        }
        if (errorNotFound) throw new Error("Column " + id + " does not exist");
        
        return null;
    };
    
    public int getColumnCount() {
        return columns.size();
    }
    public DataField addColumn(String id, String heading, String type) {
        DataField column = getColumn(id, false, false);
        
        if (column != null) throw new ColumnsException("Column " + id + "already exists");
        
        columns.add(column = new DataField(id, heading, type));
        
        return column;
    }

    public DataField addColumn(String id, String heading) {
        return addColumn(id, heading, "");
    }
    public DataField addColumn(String id) {
        return addColumn(id, id);
    }
    public DataField getColumn(String id) {
        return getColumn(id, true, false);
    }
    public DataField getColumn(int index) {
        if (index >= columns.size()) throw new ColumnsException("Column index " + index + " out of bounds");
        
        return columns.get(index);
    }
    public DataField getColumn(String id, boolean errorNotFound) {
        return getColumn(id, errorNotFound, false);
    }
    public void setValue(String id, String value) {
        getColumn(id, true, true).setValue(value);
    }
    public void setValue(String id, boolean value) {
        getColumn(id, true, true).setValue(value);
    }

    public void setValue(String id, int value) {
        getColumn(id, true, true).setValue(value);
    }
    public void setValue(String id, long value) {
        getColumn(id, true, true).setValue(value);
    }

    public void setValue(String id, double value) {
        getColumn(id, true, true).setValue(value);
    }
    public void setValue(String id, double value, double nullValue) {
        getColumn(id, true, true).setValue(value, nullValue);
    }
    public void setValue(String id, Date value, String format) {
        getColumn(id, true, true).setValue(value, format);
    }
    public void setValue(String id, HighResolutionTime value, String format) {
        getColumn(id, true, true).setValue(value, format);
    }
    public void setValue(String id, HighResolutionTime value) {
        getColumn(id, true, true).setValue(value);
    }
    
    public void discard() {
        for (DataField column : columns) {
            column.clear();
        }
    }
}
