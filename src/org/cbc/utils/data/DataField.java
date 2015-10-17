/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.utils.data;

import org.cbc.utils.system.HighResolutionTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author CClose
 */
public class DataField {

    private String id;
    private String heading;
    private String value = "";
    private String type = "";
    private int    sqlType = -1;
    private boolean empty = true;
    private boolean hidden = false;
    private boolean isNull = true;
    
    public String getHeading() {
        return heading == null || heading.length() == 0? id : heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public DataField(String id, String heading, String type) {
        this.id = id;
        this.heading = heading;
        this.type = type;
    }
    public void clear() {
        value = "";
        empty = true;
        isNull = true;
    }
    public boolean isEmpty() {
        return empty;
    }
    public String getId() {
        return id;
    }
    private void assignValue(String value) {
        this.value = isNull? "" : value;
        this.empty = false;
    }
    public void setValue(int value) {
        isNull = false;
        setValue("" + value);
    }

    public void setValue(boolean value) {
        isNull = false;
        setValue(value ? "Y" : "N");
    }

    public void setValue(long value) {
        isNull = false;
        setValue("" + value);
    }

    public void setValue(int value, int nullValue) {
        isNull = value == nullValue;
        assignValue("" + value);
    }
    public void setValue(long value, long nullValue) {
        isNull = value == nullValue;
        assignValue("" + value);
    }

    public void setValue(double value) {
        isNull = false;
        assignValue("" + value);
    }
    public void setValue(double value, double nullValue) {
        isNull = value == nullValue;
        assignValue("" + value);
    }
    public void setValue(String value) {
        isNull = value == null;
        assignValue(value);
    }
    public void setValue(Date value, String format) {
        isNull = value == null;
        assignValue(isNull? null : new SimpleDateFormat(format).format(value.getTime()));
    }
    public void setValue(HighResolutionTime value, String format) {
        isNull = value.isNull();
        assignValue(isNull? null : value.getFormatted(format));
    }
    public void setValue(HighResolutionTime value) {
        isNull = value.isNull();
        assignValue(isNull? null : value.getFormatted());
    }
    public String getValue() {
        return value;
    }
    public String getValue(String nullValue) {
        return empty? nullValue : getValue();
    }
    public int getIntValue() {        
        return Integer.parseInt(value);
    }
    public int getIntValue(int nullValue) {
        return empty || value.length() == 0? nullValue : getIntValue();
    }
    public double getDoubleValue() {        
        return Double.parseDouble(value);
    }
    public double getDoubleValue(double nullValue) {
        return empty || value.length() == 0? nullValue : getDoubleValue();
    }
    public Date getDateValue(String pattern) throws ParseException {        
        return new SimpleDateFormat(pattern).parse(value);
    }
    public Date getDateValue(String pattern, Date nullValue) throws ParseException {
        return empty || value.length() == 0? nullValue : getDateValue(pattern);
    }
    public boolean isNull() {
        return this.isNull;
    }
    public int getSqlType() {
        return sqlType;
    }

    public void setSqlType(int sqlType) {
        this.sqlType = sqlType;
    }

    /**
     * @return the hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param hidden the hidden to set
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    public String toString() {
        if (this.isNull()) return "null";
        if (this.isEmpty()) return "empty";

        return this.value;
    }
}
