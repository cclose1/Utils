/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.json;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
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
    private HashMap<String, JSONValue> members = new HashMap<String, JSONValue>();
    private ArrayList<String>          order;
  
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
        add(name, new JSONValue(Utils.round(value, places)));
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
    public void add(String name, ResultSet rs, String optionalColumns, boolean fractionalSeconds) throws SQLException, JSONException {
        class Field {
            boolean present = false;
        }
        HashMap<String, Field> fields = new HashMap<String, Field>();
        
        if (optionalColumns != null) {
            for (String f : optionalColumns.split(",")) fields.put(f, new Field());
        }        
        JSONArray row;
        
        int count = rs.getMetaData().getColumnCount();
        
        this.add("Table", new JSONValue(name));
        row = this.add("Header", (JSONArray)null);

        for (int i = 1; i <= count; i++) {
            ResultSetMetaData md       = rs.getMetaData();
            String            field    = md.getColumnLabel(i);
            Field             optional = fields.get(field);
            JSONObject        col;
            
            col = row.addObject();
            col.add("Name",        new JSONValue(field));
            col.add("Type",        new JSONValue(md.getColumnTypeName(i).toLowerCase()));
            col.add("Scale",       new JSONValue(md.getScale(i)));
            col.add("Precision",   new JSONValue(md.getPrecision(i)));
            col.add("Nullability", new JSONValue(md.isNullable(count)));
            
            if (optional != null) {
                optional.present = true;
                col.add("Optional", new JSONValue(true));
            }
        }
        row = this.add("Data", (JSONArray)null);
        
        while (rs.next()) {
            JSONArray col = row.addArray();
            
            for (int i = 1; i <= count; i++) {                
                col.add(JSONValue.getJSONValue(rs, i, fractionalSeconds));
            }
        }
    }
    public void add(String name, ResultSet rs, String optionalColumns) throws SQLException, JSONException {
        add(name, rs, optionalColumns, false);
    }
    public void add(String name, ResultSet rs, boolean fractionalSeconds) throws SQLException, JSONException {
        add(name, rs, null, fractionalSeconds);
    }    
    public void add(String name, ResultSet rs) throws SQLException, JSONException {
        add(name, rs, null, false);
    }
}
