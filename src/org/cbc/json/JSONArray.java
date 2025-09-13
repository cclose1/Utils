/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.json;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Contains a JSON array. The JSON array is an array of JSONValues. The value can be a JSON object, array or primitive value.
 * 
 * The class implements an iterator which returns JSONValue objects. 
 * 
 * The class methods are not synchronised and object can be corrupted if it is accessed concurrently. 
 */
public class JSONArray implements Iterable<JSONValue> {
    ArrayList<JSONValue> array = new ArrayList<>();

    private class ArrayIterator implements Iterator<JSONValue> {
        int     count     = 0;
        boolean canRemove = false;
        
        @Override
        public boolean hasNext() {
            return count < array.size();
        }

        @Override
        public JSONValue next() {
            if (hasNext()) {
                canRemove = true;
                
                return array.get(count++);
            } else {
                /*
                 * This can only happen if caller has not obeyed hasNext or values
                 * have been removed from array while iterating. 
                 * 
                 * For now return null until suitable exception has been identified.
                 */
                return null;
            }
        }

        @Override
        public void remove() {
            if (!canRemove) throw new IllegalStateException();
            
            array.remove(--count);
            canRemove = false;
        }
    }

    @Override
    public Iterator<JSONValue> iterator() {
        return new ArrayIterator();
    }
    /**
     * Returns the count of the number of elements in the array.
     * @return Array size.
     */
    public int size() {
        return array.size();
    }
    /**
     * Appends value to the end of the array.
     * @param value New array entry.
     */
    public void add(JSONValue value) {
        array.add(value);
    }
    /**
     * Appends object to the end of the array.
     * @param object New object entry.
     */
    public void add(JSONObject object) {
        array.add(new JSONValue(object));
    }
    /**
     * Appends array to the end of the array.
     * @param array New array entry.
     */
    public void add(JSONArray array) {
        this.array.add(new JSONValue(array));
    }
    /**
     * Appends an empty object to the array and returns it.
     * @return Newly created object.
     */
    public JSONObject addObject() {
        JSONObject object = new JSONObject();
        
        add(new JSONValue(object));
        return object;
    }
    /**
     * Appends an empty array to the array and returns it.
     * @return Newly created array.
     */
    public JSONArray addArray() {
        JSONArray arr = new JSONArray();
        
        add(new JSONValue(arr));
        return arr;
    }
    /**
     * Returns the value at index in the array.
     * @param index Required array index.
     * @return Value for index.
     * @throws JSONException  Index is not within the array.
     */
    public JSONValue get(int index) throws JSONException {
        try {
            return array.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new JSONException("Element " + index + " is out of bounds for the array");
        }
    }
    /**
     * Appends the array as a string to buffer formatted as defined by format.
     * @param buffer Target for the formatted string.
     * @param format Format applied to the array string.
     */
    public void append(StringBuilder buffer, JSONFormat format, String nullOverride) {
        boolean first = true;
        
        buffer.append('[');
        format.enter();
       
        for (JSONValue v : this) {
            if (!first) buffer.append(',');
            
            format.startLine(buffer);
            v.append(buffer, format, nullOverride);
            first = false;
        }
        buffer.append(']');
        format.exit();
    }
    public void append(StringBuilder buffer, JSONFormat format) {
        append(buffer, format, null);
    }
    /**
     * Add the fields as an array of objects. The result set must either be empty or
     * contain a single row. Each field object contains the following members:
     * <ul>
     * <li>
     * <code>Name</code>
     * <li>
     * <code>Type</code>
     * <li>
     * <code>Precision</code>
     * <li>
     * <code>Scale</code>
     * <li>
     * <code>Value</code>
     * </ul>
     * @param rs Result set containing fields.
     */
    private void addFields(ResultSet rs, JSONObject.DBOptions dbOptions) throws SQLException, JSONException, ParseException {        
        JSONObject.DBRow dbRow = new JSONObject.DBRow(rs);
             
        if (dbRow.nextRow()) {
            for (int i = 1; i <= dbRow.getColumnCount(); i++) {
                JSONObject field = addObject();

                dbRow.setColumn(i);
                field.add("Name",      new JSONValue(dbRow.getName()));
                field.add("Type",      new JSONValue(dbRow.getType()));
                field.add("Precision", new JSONValue(dbRow.getPrecision()));
                field.add("Scale",     new JSONValue(dbRow.getType().contains("money") ? 2 : dbRow.getScale()));
                field.add("Value",     new JSONValue(dbRow, dbOptions));
                
                if (dbOptions.isOptional(dbRow.getName())) field.add("Optional", new JSONValue(true));
            }
        }
        if (rs.next()) throw new JSONException("Result set conains more than one row");
    }
    /**
     * Add the fields as an array of objects. The result set must either be empty or
     * contain a single row. Each field object contains the following members:
     * <ul>
     * <li>
     * <code>Name</code>
     * <li>
     * <code>Type</code>
     * <li>
     * <code>Precision</code>
     * <li>
     * <code>Scale</code>
     * <li>
     * <code>Value</code>
     * </ul>
     * @param rs Result set containing fields.
     */
    private void addFields(ResultSet rs, boolean toLocalTime) throws SQLException, JSONException, ParseException {
        JSONObject.DBOptions dbOpts = new JSONObject.DBOptions(toLocalTime);
        addFields(rs, dbOpts);
    }
    public void addFields(ResultSet rs) throws SQLException, JSONException, ParseException {
        addFields(rs, false);
    }
    /**
     * Appends the array as a string to buffer.
     * @param buffer Target for the string.
     */
    public void append(StringBuilder buffer) {
        append(buffer, new JSONFormat());
    }
    /**
     * Returns the formatted array string.
     * @param format Applied to the array string.
     * @return Formatted array string.
     */
    public String toString(JSONFormat format) {
        StringBuilder buffer = new StringBuilder();
        append(buffer, format);
        return buffer.toString();
    }
    /**
     * Returns the array string.
     * @return Array string.
     */
    public String toString() {
        return toString(new JSONFormat());
    }
}

