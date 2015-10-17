/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.json;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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
     * Appends the object as a string to buffer formatted as defined by format.
     * @param buffer Target for the formatted string.
     * @param format Format applied to the object string.
     */
    public void append(StringBuilder buffer, JSONFormat format) {
        boolean first = true;
        
        buffer.append('{');
        format.enter();

        for (JSONNameValue nv : this) {
            if (!first) buffer.append(',');
            
            format.startLine(buffer);
            buffer.append('"');
            buffer.append(nv.getName());
            buffer.append("\":");
            nv.getValue().append(buffer, format);
            first = false;
        }
        buffer.append('}');
        format.exit();
    }

    /**
     * Appends the object as a string to buffer.
     * @param buffer Target for the string.
     */
    public void append(StringBuilder buffer) {
        append(buffer, new JSONFormat());
    }

    /**
     * Returns the formatted object string.
     * @param format Applied to the object string.
     * @return Formatted object string.
     */
    public String toString(JSONFormat format) {
        StringBuilder buffer = new StringBuilder();
        append(buffer, format);
        return buffer.toString();
    }
    /**
     * Returns the object string.
     * @return Object string.
     */
    public String toString() {
        return toString(new JSONFormat());
    }
    private class Column {
        String type;
        int    precision;
        int    scale;
    }
    public void add(String name, ResultSet rs, String optionalColumns, boolean fractionalSeconds) throws SQLException, JSONException {
        class Field {
            boolean present = false;
        }
        HashMap<String, Field> fields = new HashMap<String, Field>();
        
        if (optionalColumns != null) {
            for (String f : optionalColumns.split(",")) fields.put(f, new Field());
        }        
        JSONArray         row;
        ArrayList<Column> columns = new ArrayList<Column>();
        
        int count = rs.getMetaData().getColumnCount();
        
        this.add("Table", new JSONValue(name));
        row = this.add("Header", (JSONArray)null);

        for (int i = 1; i <= count; i++) {
            JSONObject col;
            Column     hdr      = new Column();
            String     field    = rs.getMetaData().getColumnName(i);
            Field      optional = fields.get(field);
            
            columns.add(hdr);
            col           = row.addObject();
            hdr.type      = rs.getMetaData().getColumnTypeName(i);
            hdr.precision = rs.getMetaData().getPrecision(i);
            hdr.scale     = rs.getMetaData().getScale(i);
            col.add("Name", new JSONValue(field));
            col.add("Type", new JSONValue(hdr.type));
            
            if (optional != null) {
                optional.present = true;
                col.add("Optional", new JSONValue(true));
            }
        }
        row = this.add("Data", (JSONArray)null);
        
        while (rs.next()) {
            JSONArray col = row.addArray();
            
            for (int i = 1; i <= count; i++) {
                String value = rs.getString(i);   
                Column hdr   = columns.get(i - 1);                
                
                if (value == null)
                    col.add(new JSONValue(value));
                else if (hdr.type.equalsIgnoreCase("int"))                    
                    col.add(new JSONValue(rs.getInt(i)));
                else if (hdr.type.equalsIgnoreCase("decimal"))                    
                    col.add(new JSONValue(rs.getDouble(i), hdr.scale));
                else if (!fractionalSeconds && (hdr.type.equals("datetime") || hdr.type.equals("time"))) {
                    String flds[] = value.split("\\.", 2);
                    
                    col.add(new JSONValue(flds[0]));
                } 
                else
                    col.add(new JSONValue(value.trim()));
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
