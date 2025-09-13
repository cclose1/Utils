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
import java.util.Set;
import org.cbc.Utils;

/**
 * Contains a JSON object. The JSON object is a collection of pairs of name and value. The name is a String and the value
 * is a JSONValue. The value can be a JSON object, array or primitive value.
 * 
 * The class implements an iterator which returns JSONNameValue objects. The order of JSON object members is not defined, i.e.
 * they can be returned in any order defined by the implementation. The JSONObject can be defined to remember the order 
 * in which the members are added to the object. This done by setting insertOrdered in the constructor. In this case the 
 * iterator returns the members in the order they were added to the object.
 * 
 * The class methods are not synchronised and object can be corrupted if it is accessed concurrently.
 * 
 * Note: InsertOrdered objects are a bit more costly in terms of memory and speed and by default objects are not insert ordered.
 */
public class JSONObject implements Iterable<JSONNameValue>{
    private HashMap<String, JSONValue> members = new HashMap<>();
    private ArrayList<String>          order;
    
    public static class DBOptions {
        private String optCols[]               = new String[0];
        public  boolean stripFractionalSeconds = true;
        public  boolean toLocalTime            = false;
        
        public DBOptions() {            
        }
        public DBOptions(String optionalColumns, boolean toLocalTime, boolean stripFractionalSeconds) {
            if (optionalColumns != null) {
                optCols = optionalColumns.split(",");
            } else {
                optCols = new String[0];
            }
            this.toLocalTime            = toLocalTime;
            this.stripFractionalSeconds = stripFractionalSeconds;
        }        
        public DBOptions(String optionalColumns) {
            this(optionalColumns, false, true);
        }       
        public DBOptions(boolean toLocalTime) {
            this("", toLocalTime, false);
        }
        public boolean isOptional(String column) {
            for (String s: optCols) {
                if (s.equals(column)) return true;
            }
            return false;
        }        
    }
    /*
     * A wrapper for a Result set allowing access the column values in a row and the associated meta data.
     *
     * The user of this class provides the result set and the column number of the target. These can be retrieved
     * although it should not be necessary as the user should have the result set and know the last column accessed. 
     * However, this class can be passed to another method, thus providing it with these parameters.
     *
     * It may better to define this class in DatabaseSession and make non static so that it can access the 
     * DatabaseSession data.
     */
    public static class DBRow {
        private ResultSet rs     = null;
        private int       column;
        
        public DBRow(ResultSet rs) {
            this.rs     = rs;
            this.column = -1;
        }
        public boolean nextRow() throws SQLException {
            return this.rs.next();
        }
        public ResultSet getResult() {
            return this.rs;
        }
        public int getColumnCount() throws SQLException {
            return rs.getMetaData().getColumnCount();
        }
        public void setColumn(int column) {
            this.column = column;
        }
        public int getColumn() {
            return this.column;
        }
        public String getName() throws SQLException {
            return rs.getMetaData().getColumnLabel(column);
        }
        public String getType() throws SQLException {
            return rs.getMetaData().getColumnTypeName(column).toLowerCase();
        }
        public String getValue() throws SQLException {
            return rs.getString(column);
        }
        public int getPrecision() throws SQLException {
            return rs.getMetaData().getPrecision(column);
        }
        public int getScale() throws SQLException {
            return rs.getMetaData().getScale(column);
        }
        public int getIsNullable() throws SQLException {
            return rs.getMetaData().isNullable(column);
        }
    }
    private class ObjectIterator implements Iterator<JSONNameValue> {
        int              count    = 0;
        String           lastKey  = null;
        Iterator<String> iterator = order == null?  members.keySet().iterator() : null;
        
        @Override
        public boolean hasNext() {
            if (iterator != null) return iterator.hasNext();
            
            return count < members.size();
        }
        @Override
        public JSONNameValue next() {
            if (hasNext()) {
                JSONNameValue value = new JSONNameValue();
                
                value.name  = iterator == null? order.get(count++) : iterator.next();
                lastKey     = value.name;
                value.value = members.get(value.name);
                
                return value;
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
            if (lastKey == null) throw new IllegalStateException();
            
            if (iterator != null)
                iterator.remove();
            else
                order.remove(--count);
            
            members.remove(lastKey);
            lastKey = null;
        }
    }
    @Override
    public Iterator<JSONNameValue> iterator() {
        return new JSONObject.ObjectIterator();
    }
    /**
     * Creates an Object allowing explicit setting of the insert ordered property.
     * 
     * @param insertOrdered true if insert ordering is enabled.
     */
    public JSONObject(boolean insertOrdered) {
        order = insertOrdered? new ArrayList<String>() : null;
    }
    /**
     * Creates an Object with insert ordering disabled.
     */
    public JSONObject() {
        this(false);
    }
    /**
     * Returns the names of the Object members.
     * 
     * @return Set of Object member names.
     */
    public Set<String> getNames() {
       return members.keySet();
    }
    public void add(String name, JSONValue value) throws JSONException {
        if (members.containsKey(name)) throw new JSONException("Member " + name + " already exists");
       
        members.put(name, value);
        
        if (order != null) order.add(name);
    }
    public void add(String name, String value) throws JSONException {
        add(name, new JSONValue(value));
    }
    public void add(String name, boolean value) throws JSONException {
        add(name, new JSONValue(value));
    }
    public void add(String name, int value) throws JSONException {
        add(name, new JSONValue(value));
    }
    public void add(String name, double value) throws JSONException {
        add(name, new JSONValue(value));
    }
    public void add(String name, double value, int places) throws JSONException {
        add(name, new JSONValue(Utils.format(value, places)));
    }
    public void update(String name, JSONValue value) throws JSONException {
        if (!members.containsKey(name)) throw new JSONException("Member " + name + " does not exists");
       
        members.put(name, value);
    }
    /**
     * Adds a new object member for name and value.
     * 
     * @param name   Name of new member
     * @param object Object that is the value of the new member.
     * @return       The input parameter object.
     * @throws JSONException  name is already a member of object. The object is unchanged.
     */
    public JSONObject add(String name, JSONObject object) throws JSONException {
        add(name, new JSONValue(object));
 
        return object;
    }
    /**
     * Adds a new object member for name and value.
     * 
     * @param name   Name of new member
     * @param array  Array that is the value of the new member.
     * @return       The input parameter array.
     * @throws JSONException  name is already a member of object. The object is unchanged.
     */
    public JSONArray add(String name, JSONArray array) throws JSONException {
        if (array == null) array = new JSONArray();
        
        add(name, new JSONValue(array));
 
        return array;
    }
    /**
     * Returns the value of object member name.
     * @param name Required member name.
     * @return The value of member name or null if name is not a member.
     */
    public JSONValue get(String name) {
        return members.get(name);
    }
    /**
     * Returns the value of object member name.
     * 
     * @param  name     Required member name.
     * @param  required If true name must be a member of the object.
     * @return The value of the member name or null if not a member and required is false.
     * @throws JSONException Thrown if member is not found and required is true;
     */
    public JSONValue get(String name, boolean required) throws JSONException {
        JSONValue value = get(name);
        
        if (value == null && required) throw new JSONException(name, "is not a member of the object");
        
        return value;
    }
    /**
     * Appends the object as a string to buffer formatted as defined by format.
     * @param buffer Target for the formatted string.
     * @param format Format applied to the object string.
     */
    public void append(StringBuilder buffer, JSONFormat format, String nullOverride) {
        boolean first = true;
        
        buffer.append('{');
        format.enter();

        for (JSONNameValue nv : this) {
            if (!first) buffer.append(',');
            
            format.startLine(buffer);
            buffer.append('"');
            buffer.append(nv.getName());
            buffer.append("\":");
            nv.getValue().append(buffer, format, nullOverride);
            first = false;
        }
        buffer.append('}');
        format.exit();
    }
    public void append(StringBuilder buffer, JSONFormat format) {
        append(buffer, format, null);
    }
    /**
     * Appends the object as a string to buffer.
     * @param buffer Target for the string.
     */
    public void append(StringBuilder buffer) {
        append(buffer, new JSONFormat(), null);
    }
    public void append(StringBuilder buffer, String nullOverride) {
        append(buffer, new JSONFormat(), nullOverride);
    }

    /**
     * Returns the formatted object string.
     * @param format Applied to the object string.
     * @return Formatted object string.
     */
    public String toString(JSONFormat format, String nullOverride) {
        StringBuilder buffer = new StringBuilder();
        append(buffer, format, nullOverride);
        return buffer.toString();
    }
    public String toString(JSONFormat format) {
        return toString(format, null);
    }
    /**
     * Returns the object string.
     * @return Object string.
     */
    public String toString() {
        return toString(new JSONFormat(), null);
    }
    private void add(String name, ResultSet rs, JSONObject.DBOptions dbOptions) throws SQLException, JSONException, ParseException {
        JSONArray row;
        DBRow dbRow = new DBRow(rs);
        int count = rs.getMetaData().getColumnCount();
        
        this.add("Table", new JSONValue(name));
        row = this.add("Header", (JSONArray)null);

        for (int i = 1; i <=  dbRow.getColumnCount(); i++) {
            JSONObject col;
            
            dbRow.setColumn(i);
            col = row.addObject();
            col.add("Name",        new JSONValue(dbRow.getName()));
            col.add("Type",        new JSONValue(dbRow.getType()));
            col.add("Scale",       new JSONValue(dbRow.getScale()));
            col.add("Precision",   new JSONValue(dbRow.getPrecision()));
            col.add("Nullability", new JSONValue(dbRow.getIsNullable()));
            
            if (dbOptions.isOptional(dbRow.getName())) col.add("Optional", new JSONValue(true));
        }
        row = this.add("Data", (JSONArray)null);
        
        while (dbRow.nextRow()) {
            JSONArray col = row.addArray();
            
            for (int i = 1; i <= count; i++) {
                dbRow.setColumn(i);
                col.add(new JSONValue(dbRow, dbOptions));
            }
        }
    }
    public void add(String name, ResultSet rs, String optionalColumns) throws SQLException, JSONException, ParseException {
        add(name, rs, new DBOptions(optionalColumns));
    }
    public void add(String name, ResultSet rs, boolean toLocalTime) throws SQLException, JSONException, ParseException {
        add(name, rs, new DBOptions(toLocalTime));
    }
    public void add(String name, ResultSet rs) throws SQLException, JSONException, ParseException {
        add(name, rs, new DBOptions());
    } 
}
