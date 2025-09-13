/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import org.cbc.utils.system.DateFormatter;
import org.cbc.utils.system.TimeWithDate;

/**
 * This class represents a JSON value. Each value has a JSONType and the class has methods appropriate for each type. If a
 * method is called that is not applicable to the type a JSONException is thrown.
 * 
 * @author Chris
 */
public class JSONValue {
    private JSONType   type   = null;
    private JSONObject object = null;
    private JSONArray  array  = null;
    private String     value  = null;
    /*
     * Adds field to buffer escaping control characters/
     */
    private void append(StringBuilder buffer, String field) {
        if (field == null) return;
        
        for (int i = 0; i < field.length(); i++) {
            char ch = field.charAt(i);
            
            switch (ch) {
                case '"':
                case '\\':
                case '/':
                    buffer.append('\\');
                    break;
                case '\b':
                    buffer.append('\\');
                    ch = 'b';
                    break;
                case '\f':
                    buffer.append('\\');
                    ch = 'f';
                    break;
                case '\n':
                    buffer.append('\\');
                    ch = 'n';
                    break;  
                case '\r':
                    buffer.append('\\');
                    ch = 'r';
                    break;      
                case '\t':
                    buffer.append('\\');
                    ch = 't';
                    break;  
                default:
                    break;
            }
            buffer.append(ch);
        }
    }
    /**
     * Creates an Object value, i.e. it has type JSONType.Object.
     * 
     * @param object
     */
    public JSONValue(JSONObject object) {
        this.type   = JSONType.Object;
        this.object = object;
    }
    /**
     *
     * Creates an Array value, i.e. it has type JSONType.Array.
     * @param array
     */
    public JSONValue(JSONArray array) {
        this.type  = JSONType.Array;
        this.array = array;
    }
    private void storeString(String value) {
        if (value == null) {
            this.type  = JSONType.Null;
            this.value = "null";
        } else {
            this.type  = JSONType.String;
            this.value = value;
        }
    }
    /**
     * Creates a primitive value of type JSONType.Boolean.
     * 
     * @param value Boolean value for the type.
     */
    public JSONValue(boolean value) {
        this.type  = JSONType.Boolean;
        this.value = value? "true" : "false";
    }
    /**
     * Creates a value of type JSONType.String, or JSONType.Null if value is null.
     * 
     * @param value
     */
    public JSONValue(String value) {
        storeString(value);
    }
    /**
     * Creates a value of type type, or JSONType.Null if value is null.
     * 
     * @param value
     * @param type  The json type if value is not the null string.
     */
    public JSONValue(String value, JSONType type) {
        storeString(value);
        
        if (value != null) this.type = type;
    }
    /**
     * Creates a value of type JSONType.Number.
     * 
     * @param value
     */
    public JSONValue(int value) {
        this.type  = JSONType.Number;
        this.value = Integer.toString(value);
    }
    private String placesFormat(int places) {
        StringBuilder fmt = new StringBuilder("#0.");
        
        while (places-- > 0) fmt.append('0');
        
        return fmt.toString();
    }
    /**
     * Creates a value of type JSONType.Number.
     * 
     * @param value
     */
    public JSONValue(double value) {
        this.type = JSONType.Number;
        this.value = Double.toString(value);
    }
    /**
     * Creates a value of type JSONType.Number rounded to places decimal places.
     *
     * @param value
     */
    public JSONValue(double value, int places) {
        this.type  = JSONType.Number;
        this.value = (new DecimalFormat(placesFormat(places))).format(value);
    }
    /**
     * Creates a value of type JSONType.Number.
     *
     * @param value
     */
    public JSONValue(float value) {
        this.type = JSONType.Number;
        this.value = Float.toString(value);
    }
    /**
     * Creates a value of type JSONType.Number rounded to places decimal places.
     *
     * @param value
     */
    public JSONValue(float value, int places) {
        this((double)value, places);
    }
    /**
     * Creates a simple value.
     * 
     * @param value    String value.
     * @param isQuoted True if the value is a quoted string and in this case the type will be <code>JSONType.String</code> unless value is null
     * in which case it will be <code>JSONType.Null</code>. False if the value is not a string. In this case the JSONType will be:
     * <ul>
     * <li>
     * <code>Null    </code>value is null or its value is the string null. If the value is null, the string null is stored as the value.
     * <li>
     * <code>Boolean </code>value, ignoring case, is the string true or false.
     * <li>
     * <code>Number  </code>value converts to a valid number. A JSONException is throw if it does not.
     * </ul>
     * @param strictQuotes True the rules above are applied, otherwise, if the value fails to convert it is stored as is, i.e.
     * it is treated as if isQuoted is true.
     * 
     * @throws JSONException Fails to convert to a value and this is only possible is strictQuotes is true.
     */
    public JSONValue(String value, boolean isQuoted, boolean strictQuotes) throws JSONException {
        this.value = value;
        this.type  = JSONType.String;
        
        if (isQuoted) 
            storeString(value == null? "" : value);
        else if (value == null)
            storeString(value);
        else {
            if (value.equalsIgnoreCase("null")) {
                this.type  = JSONType.Null;
                this.value = value.toLowerCase();
            } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                this.type  = JSONType.Boolean;
                this.value = value.toLowerCase();
            } else {
                try {
                    float f   = Float.parseFloat(value);
                    this.type = JSONType.Number;
                } catch (Exception e) {
                    if (strictQuotes) throw new JSONException("Unable to convert '" + value + "' to a number");
                }
            }
        }
    }
    /**
     * As above with strictQuotes true.
     * 
     * @param value
     * @param isQuoted
     * @throws JSONException
     */
    public JSONValue(String value, boolean isQuoted) throws JSONException {
        this(value, isQuoted, true);
    }
    /**
     * 
     * @param rs
     * @param i
     * @param dbOptions 
     */
    public JSONValue(JSONObject.DBRow row, JSONObject.DBOptions dbOptions) throws SQLException, ParseException { 
        JSONValue  ret;
        
        String dbType  = row.getType();
        String dbValue = row.getValue();
        
        if (dbValue != null && dbOptions.stripFractionalSeconds && (dbType.equals("datetime") || dbType.equals("time"))) {
            String flds[] = dbValue.split("\\.", 2);
            
            dbValue = flds[0];
        }
        if (dbValue != null & dbType.equals("datetime") && dbOptions.toLocalTime) {
            /*
             * This is a timestamp in GMT and has to be converted to local time.
             */
            Date ts = DateFormatter.parseDate(dbValue);
            
            TimeWithDate td = new TimeWithDate("Europe/London");

            dbValue = DateFormatter.format(td.toLocal(ts), "yyyy-MM-dd HH:mm:ss");
        }
        if (dbValue == null) 
            ret = new JSONValue(dbValue);
        else if (dbType.equals("int"))
            ret = new JSONValue(dbValue, JSONType.Number);
        else if (dbType.equals("decimal")) 
            ret = new JSONValue(dbValue, JSONType.Number);
        else
            ret = new JSONValue(dbValue);  
        
        this.type   = ret.type;
        this.array  = ret.array;
        this.object = ret.object;
        this.value  = ret.value;        
    }
    /*
     * Remove this
    */
    public void append(StringBuilder buffer, JSONFormat format, String nullOverride) {
        switch (getType()) {
            case Object:
                object.append(buffer, format, nullOverride);
                break;
            case Array:
                array.append(buffer, format, nullOverride);
                break;
            case String:
                buffer.append('"');
                append(buffer, value);
                buffer.append('"');
                break;
            case Number:
                buffer.append(value);
                break;
            case Boolean:
                buffer.append(value);
                break;
            case Null:
                buffer.append(nullOverride != null? '"' + nullOverride + '"' : value);
                break;
            default:
                throw new AssertionError(getType().name());
        }
    }    
    public void append(StringBuilder buffer, JSONFormat format) {
        append(buffer, format, null);
    }
    /**
     * Returns the values type.
     * 
     * @return the type as defined by JSONType.
     */
    public JSONType getType() {
        return type;
    }
    /**
     * @return the string value of a primitive type.
     * 
     * @throws JSONException if the type is JSONType.Array or JSONType.Object.
     */
    public String getString() throws JSONException {
        if (type == JSONType.Object || type == JSONType.Array) throw new JSONException(this, "getString not valid for Object or Array");
        
        return value;
    }
    public double getDouble() throws JSONException {
        if (type != JSONType.Number && type != JSONType.String) throw new JSONException(this, "Type is " + type + ". getDouble requires type Number or String");
        
        try {
            return Double.parseDouble(value);
        }
        catch(NumberFormatException e) {
            throw new JSONException(this, "Value " + value + " is not a valid double");            
        }
    }
    public int getInt() throws JSONException {
        if (type != JSONType.Number) throw new JSONException(this, "Type is " + type + ". getInt requires type " + JSONType.Number);
        
        return Integer.valueOf(value);
    }
    public boolean getBoolean() throws JSONException {
        if (type != JSONType.Boolean) throw new JSONException(this, "Type is " + type + ". getBoolean requires type " + JSONType.Boolean);
        
        return Boolean.parseBoolean(value);
    }
    /**
     * @return the object for the value.
     * 
     * @throws JSONException if the type is not JSONType.Object.
     */
    public JSONObject getObject() throws JSONException {
        if (type != JSONType.Object) throw new JSONException(this, "getObject requires type Object");
        
        return object;
    }

    /**
     * @return the array for the value.
     * 
     * @throws JSONException if the type is not JSONType.Array.
     */
    public JSONArray getArray() throws JSONException {
        if (type != JSONType.Array) throw new JSONException(this, "getArray requires type Array");
        
        return array;
    }
    private static JSONArray loadArray(JSONReader stream, boolean strictQuotes, boolean ordered) throws JSONException {
        JSONReader.Token t;
        JSONArray        a       = new JSONArray();
        int              index   = 0;
        boolean          noValue = false;
        
        while ((t = stream.next("[{,]")) != null) {
            if (index == 0 && t.getValue() == null && t.getSeparator() == ']') break; //This is for the empty array case;
            
            if (noValue) {
                //Previous was an object or array terminator.
                
                noValue = false;
                
                if (t.getSeparator() == ',') continue;
                if (t.getSeparator() == ']') break;
            }
            switch (t.getSeparator()) {
                case ']': case ',':
                    a.add(new JSONValue(t.getValue(), t.isQuoted(), strictQuotes));
                    break;
                case '[':
                    a.add(loadArray(stream, strictQuotes, ordered));
                    noValue = true;
                    break;
                case '{':
                    a.add(loadObject(stream, strictQuotes, ordered));
                    noValue = true;
                    break;
                default:
                    throw new JSONException("Array field " + index + " invalid token " + t.toString());
            }
            index++;
            
            if (t.getSeparator() == ']') break;
        }
        return a;
    }
    private static JSONObject loadObject(JSONReader stream, boolean strictQuotes, boolean ordered) throws JSONException {
        JSONReader.Token t;
        JSONObject       o = new JSONObject(ordered);
        
        while ((t = stream.next(":},")) != null) {
            String name = t.getValue();
            
            if (t.getSeparator() == ',') continue; //Comma following object or array value.
            if (t.getSeparator() == '}') break;    //This is for the empty object case.
            
            t = stream.next();
            
            if (t == null) throw new JSONException("Object field " + name + " stream end reached before value read");
            
            switch (t.getSeparator()) {
                case '}': case ',':
                    o.add(name, new JSONValue(t.getValue(), t.isQuoted(), strictQuotes));
                    break;
                case '[':
                    o.add(name, loadArray(stream, strictQuotes, ordered));
                    break;
                case '{':
                    o.add(name, loadObject(stream, strictQuotes, ordered));
                    break;
                default:
                    throw new JSONException("Object field " + name + " invalid token " + t.toString());
            }
            if (t.getSeparator() == '}') break;
        }
        return o;
    }
    /**
     * Reads stream and returns the first JSONValue from it. The overloads of load work the same way
     * but for different character streams.
     * 
     * @param stream characters stream containing JSON data.
     * @param strictQuotes false if string values are permitted to have quotes omitted.
     * @return first JSONValue from stream.
     * 
     * @throws JSONException if the data is not a valid JSONValue
     */
    public static JSONValue load(JSONReader stream, boolean strictQuotes, boolean ordered) throws JSONException {
        JSONReader.Token t    = stream.next("{[");
        
        if (t.getSeparator() == '{')
            return new JSONValue(loadObject(stream, strictQuotes, ordered));
        
        return new JSONValue(loadArray(stream, strictQuotes, ordered));
    }
    /**
     * As above but with strictQuotes set to true.
     * 
     * @param stream
     * @return
     * @throws JSONException
     */
    public static JSONValue load(JSONReader stream) throws JSONException {
        return load(stream, true, true);
    }
    /**
     *
     * @param stream
     * @param strictQuotes
     * @return
     * @throws JSONException
     */
    public static JSONValue load(StringReader stream, boolean strictQuotes, boolean ordered) throws JSONException {
        return load(new JSONReader(stream), strictQuotes, ordered);
    }
    /**
     *
     * @param stream
     * @return
     * @throws JSONException
     */
    public static JSONValue load(StringReader stream) throws JSONException {
        return load(stream, true, true);
    }
    /**
     *
     * @param stream
     * @param strictQuotes
     * @return
     * @throws JSONException
     */
    public static JSONValue load(InputStream stream, boolean strictQuotes, boolean ordered) throws JSONException {
        return load(new JSONReader(stream), strictQuotes, ordered);
    }
    /**
     *
     * @param stream
     * @return
     * @throws JSONException
     */
    public static JSONValue load(InputStream stream) throws JSONException {
        return load(stream, true, true);
    }
    /**
     *
     * @param stream
     * @param strictQuotes
     * @return
     * @throws JSONException
     */
    public static JSONValue load(File stream, boolean strictQuotes, boolean ordered) throws JSONException, FileNotFoundException {
        return load(new JSONReader(stream), strictQuotes, ordered);
    }
    /**
     *
     * @param stream
     * @return
     * @throws JSONException
     */
    public static JSONValue load(File stream) throws JSONException, FileNotFoundException {
        return load(stream, true, true);
    }
    /**
     * Returns the value string formatted according format. 
     * @param format
     * @return
     */
    public String toString(JSONFormat format) {
        switch (type) {
            case Object:
                return object.toString(format);
            case Array:
                return array.toString(format);
            case String:
                return '"' + value + '"';
            default:
                return value;                
        }
    }
    public String toString() {
        return toString(new JSONFormat());
    }
}
