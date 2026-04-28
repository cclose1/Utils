/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cbc.json;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import org.cbc.Utils;
import org.cbc.utils.data.EnhancedResultSet;

/**
 *
 * @author chris
 */
public class JSONTable extends JSONObject {
    public static enum ColumnType{Text, DateTime, Time, Int, Decimal};
    
    private String    name;
    private JSONArray header;
    private JSONArray data;
    private JSONArray row;
    private DBOptions dbOptions = new DBOptions();
    
    public JSONTable(String name) throws JSONException {
        super();
        this.name   = name;
        this.add("Table", new JSONValue(name));        
        this.header = this.add("Header", (JSONArray)null);        
        this.data   = this.add("Data", (JSONArray)null);
    }
    private int getColumnIndex(String name) throws JSONException {
        JSONArray.ArrayIterator cols = (JSONArray.ArrayIterator)header.iterator();
        
        while (cols.hasNext()) {
            JSONValue col = cols.next().getObject().get("Name");
            
            if (col.getString().equals(name)) return cols.getIndex();            
        }
        return -1;
    }
    public JSONObject getColumn(String name) throws JSONException {
        int index = getColumnIndex(name);
        
        if (index == -1) throw new JSONException("Column " + name + " not in table " + this.name);

        return header.get(index).getObject();
    }
    public void setDBOptions(JSONObject.DBOptions options) {
        dbOptions = options;
    }
    public JSONObject addColumn(String name, String type, int precision, int scale) throws JSONException {
        if (getColumnIndex(name) != -1) { 
            throw new JSONException("Column " + name + " is already defined for table " + this.name);
        }
        JSONObject col = header.addObject();        
        
        col.add("Name",      new JSONValue(name));
        col.add("Type",      new JSONValue(type));
        col.add("Scale",     new JSONValue(scale));
        col.add("Precision", new JSONValue(precision)); 
        
        return col;
    }
    public JSONObject addColumn(String name, ColumnType type, int precision, int scale) throws JSONException {
        return addColumn(name, type.toString().toLowerCase(), precision, scale);
    }
    public void addRow() throws JSONException { 
        row = data.addArray();
        
        for (int i = 0; i < header.size(); i++) {
            row.add(new JSONValue((String)null));
        }        
    }
    public void set(String name, JSONValue value, boolean mustExist) throws JSONException {
        int index = getColumnIndex(name);
        
        if (index == -1) {
            if (mustExist) throw new JSONException("Column " + name + " is not defined for table " + this.name);
            
            return;
        }
        /*
         * Maybe add check to see if value matches column type.
         */
        JSONObject col = header.get(index).getObject();
        /*
         * The following provides for column type specific modifications to the value to be loaded
         */
        switch (col.get("Type").getString()) {
            case "time":
                /*
                 * Time values can be stored on the database as a decimal contain a fractional number
                 * of when representing a delay, e.g. 1.5 is equivalent to "01:30:00".
                 * 
                 * If value string converts to a double, it is converted. Otherwise the value is not changed.
                 */
                try {                    
                    value = new JSONValue(Utils.hoursToTime(value.getDouble()), true);         
                } catch (JSONException e) {
                    // No action required, the parameter value is not changed.                    
                }
                break;
        }
        row.set(index, value);
    }
    public void set(String name, String value) throws JSONException {
        set(name, new JSONValue(value), true);
    }
    public void set(String name, int value) throws JSONException {
        set(name, new JSONValue(value), true);
    }
    public void set(String name, double value) throws JSONException {
        set(name, new JSONValue(value), true);
    }
    public void addRow(EnhancedResultSet row) throws JSONException, SQLException, ParseException {
        addRow();
        
        for (int i = 1; i <= row.getColumnCount(); i++) {
            row.setColumn(i);
            set(row.getName(), new JSONValue(row, dbOptions), false);
        }
    }
    public void load(ResultSet rs) throws SQLException, JSONException, ParseException {
        EnhancedResultSet dbRow = new EnhancedResultSet(rs); 
        
        for (int i = 1; i <=  dbRow.getColumnCount(); i++) {
            JSONObject col;
            
            dbRow.setColumn(i);
            col = addColumn(
                    dbRow.getName(),
                    dbRow.getType(),
                    dbRow.getPrecision(),
                    dbRow.getScale());
            col.add("Nullability", new JSONValue(dbRow.getIsNullable()));
            
            if (dbOptions.isOptional(dbRow.getName())) col.add("Optional", new JSONValue(true));
        }        
        while (dbRow.nextRow()) {
            addRow(dbRow);
        }        
    }
    public void load(ResultSet rs, boolean toLocalTime) throws SQLException, JSONException, ParseException{
        boolean saveToLocal = dbOptions.toLocalTime;
        
        dbOptions.toLocalTime = toLocalTime;
        load(rs);
        dbOptions.toLocalTime = saveToLocal;
    }
}
