/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.cbc.utils.data.DatabaseSession;
import org.cbc.utils.system.DateFormatter;

/**
 *
 * @author CClose
 */
public abstract class SQLBuilder {
    protected ArrayList<Field> fields   = new ArrayList<>();
    protected String           table    = null;
    protected StringBuilder    where    = null;
    private   char             paramSep = '?';
    protected String           protocol = "sqlserver";
    
    private HashMap<String, ParameterValue> parameters   = new HashMap<>(0);
    private ArrayList<String>               uses         = new ArrayList<>(0);
    private SimpleDateFormat                fmtTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    protected enum ValueType {Field, Expression, Text, Double, Integer, Date};
    
    protected class Value {
        protected ValueType type;
        
        protected String    txtValue;
        protected double    dblValue;
        protected int       intValue;
        protected Date      datValue;
        protected boolean   isQuoted;
        
        protected Value(String value, ValueType type) {
            txtValue  = value;
            this.type = type;
            isQuoted  = type == ValueType.Text;
        }
        protected Value(int value) {
            intValue = value;
            type     = ValueType.Integer;
        }
        protected Value(double value) {
            dblValue = value;
            type     = ValueType.Double;
        }
        protected Value(Date value) {
            datValue = value;
            type     = ValueType.Date;
            isQuoted = true;
        }
        protected void setQuoted(boolean yes) {
            isQuoted = yes;
        }
        protected String getValue() {
            String text = "";
            
            switch (type) {
                case Field:
                    text = txtValue == null? null : DatabaseSession.delimitName(txtValue, protocol);
                    break;
                case Expression:
                    text = txtValue;
                    break;
                case Text:
                    text = txtValue;
                    break;
                case Double:
                    text = "" + dblValue;
                    break;
                case Integer:
                    text = "" + dblValue;
                    break;         
                case Date:
                    text = fmtTimestamp.format(datValue);
                    break;         
            }
            return text == null? null : isQuoted? '\'' + DatabaseSession.escape(text) + '\'' : text;
        }
    }
    public Value setValue(double value) {
        return new Value(value);
    }
    public Value setValue(int value) {
        return new Value(value);
    }
    public Value setValue(Date value) {
        return new Value(value);
    }
    public Value setValue(String value) {
        return new Value(value, ValueType.Text);
    }
    public Value setFieldValue(String value) {
        return new Value(value, ValueType.Field);
    }
    public Value setExpressionValue(String value) {
        return new Value(value, ValueType.Expression);
    }
    protected class Source extends Value {
        protected Source(String value, ValueType type) {
            super(value, type);
        }
        protected Source(int value) {
            super(value);
        }
        protected Source(double value) {
            super(value);
        }
    }
    public Source setSource(int value) {
        return new Source(value);
    }
    public Source setSource(String value) {
        return new Source(value, ValueType.Text);
    }
    public Source setFieldSource(String value) {
        return new Source(value, ValueType.Field);
    }
    public Source setExpressionSource(String value) {
        return new Source(value, ValueType.Expression);
    }   
    public String delimitName(String name) {
        return name == null? null : DatabaseSession.delimitName(name, protocol);
    }
    protected class Field {
        private   String  name;
        private   Source  source;
        private   Value   value;

        public Field(String name, String value, boolean quoted) {
            this.name   = name;
            this.value  = new Value(value, ValueType.Text);
            this.value.setQuoted(quoted);
        }
        public Field(String name, Value value) {
            this.name  = name;
            this.value = value;
        }
        public Field(String name, Source source, Value value) {
            this.name   = name;
            this.source = source;
            this.value  = value;
        }
        public String getName() {            
            return delimitName(name);
        }
        public String getSource() {
            return source == null? null : source.getValue();
        }
        public String getValue() {
            return value == null? null : value.getValue();
        }
        protected void setObject(Object object) throws SQLException {
            if (object == null) return;
            
            String cName = object.getClass().getSimpleName();
            
            switch (cName) {
                case "Source":
                    source = (Source) object;
                    break;
                case "Value":
                    value = (Value) object;
                    break;
                case "String":
                    value = new Value((String) object, ValueType.Text);
                    break;
                case "Date":
                    value = new Value((Date) object );
                    break;
                default:
                    throw new SQLException("SQLBuilder.setObject does not support class " + cName);
            }
        }
        protected void setIsQuoted(boolean source, boolean yes) {
            if (source)
                this.source.isQuoted = yes;
            else
                this.value.isQuoted = yes;
        }
        /**
         * @return the quoted
         */
        public boolean getIsQuoted(boolean source) {
            if (source) return this.source.isQuoted;
            
            return value.isQuoted;
        }
    }
    public void setTable(String table) {
        this.table = table;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public String getProtocol() {
        return protocol;
    }
    public Field addField(String name, String value, boolean quoted) {
        Field f = new Field(name, value, quoted);
        fields.add(f);
        return f;
    }
    public void addField(String name, String value) {
        addField(name, value, true);
    }
    public void addField(String name, Value value) {
        Field f = new Field(name, value);
        fields.add(f);

    }
    public void addField(String name, Date value) {
        addField(name, setValue(value));
    }
    public void addField(String name, int value) {
        addField(name, setValue(value));
    }
    public void addField(String name, double value) {
        addField(name, setValue(value));
    }
    protected Field getField(String name) {
        return fields.get(fields.indexOf(name));
    }
    protected void addClause(StringBuilder sql, String name, String value) {
        if (value != null) {
            sql.append("\r\n");
            sql.append(delimitName(name));
            sql.append(' ');
            sql.append(value);            
        }
    }
    protected void addWhere(StringBuilder sql) {
        if (where != null) addClause(sql, "WHERE", where.toString());
    }
    public void setWhere(String where) {
        if (this.where == null)
            this.where = new StringBuilder(where);
        else {
            this.where.append(' ');
            this.where.append(where);
        }
    }
    public void addAndClause(String clause) {
        if (where == null) where = new StringBuilder();
        
        if (where.length() != 0) where.append(" AND ");
        
        where.append(clause);        
    }
    public void addAnd(String field, String operator, String value, boolean quoted) {
        if (where == null) where = new StringBuilder();
        
        if (where.length() != 0) where.append(" AND ");
        
        where.append(field);
        where.append(' ');
        where.append(operator);
        
        if (quoted) {
            where.append('\'');
            DatabaseSession.appendEscaped(where, value);
            where.append('\'');
        } else
            where.append(value);
    }
    public String getTimestamp(Date date) {
        if (date == null) date = new Date();
        
        return fmtTimestamp.format(date);
    }
    public String getWhere() {
        return where.toString();
    }
    public void addAnd(String field, String operator, String value) {
        if (value != null && value.trim().length()!= 0) addAnd(field, operator, value, true);
    }
    public void addAnd(String field, String operator, Date value) {
        if (value != null) addAnd(field, operator, fmtTimestamp.format(value), true);
    }
    public void addAnd(String field, String operator, int value) {
        addAnd(field, operator, "" + value, false);
    }
    /*
     * fields identifies the database field names for an And clause and consists of one or more clauses separated by the
     * clauseSeparator character, e.g. ,. Each clause consists of the database field name seperated from from the values by the fieldSeparator e.g. =. 
     * There can be one or more values separated by the valueSeparator character, e.g. |. Text values must be explicitly quoted, i.e. unlike the 
     * other addAnd methods there is not a quoted parameter.
     *
     * If the values contain the % character, there must be only one value in the list and the comparison is done using LIKE. If the list has
     * more than one value, the test uses the IN operator, otherwise, the comparison is done using =.
     *
     * E.g. the following fields string
     *
     *   Name='John'|'Bill', Account='AC1',Comment='%and', Code=1,6,10
     *
     * generates
     *    Name    IN('John','Bill') AND
     *    Account = 'AC1'           AND
     *    Comment LIKE '%and'       AND
     *    Code    IN(1,6,10)
     */
    public void addAnd(String fields, char clauseSeparator, char fieldSeparator, char valueSeparator) throws SQLException {
        if (fields == null || fields.length() == 0) return;
        
        String clauses[] = fields.split("" + clauseSeparator);
        
        for (String clause : clauses) {
            String flds[] = clause.split("" + fieldSeparator);
            
            if (flds.length != 2) throw new SQLException("Clause '" + clause + "' not of form Name = Value(s)");
            
            if (flds[1].contains("" + valueSeparator)) {
                addAnd(flds[0], "IN", '(' + flds[1].replace('|', ',') + ')', false);
            } else if (flds[1].contains("%")) 
                addAnd(flds[0], "LIKE", flds[1], false);
            else
                addAnd(flds[0], "=", flds[1], false);
        }        
    }
    public void addAnd(String fields) throws SQLException {
        addAnd(fields, ',', '=', '|');
    }
    public void addAndStart(Date start) {
        if (start != null) {
            addAnd("Start", "<=", start);
            setWhere(
                " AND (" + delimitName("End") + " >= '" + getTimestamp(start) +
                "' OR "  + delimitName("End") + " IS NULL)");
        }
    }
    public void clearFields() {
        fields.clear();
    }
    public void clearWhere() {
        where = null;
    }
    public void clear() {
        fields.clear();
        parameters.clear();
        uses.clear();
        where = null;
    }
    public abstract String build();
    
    private class ParameterValue {
        int       type   = java.sql.Types.VARCHAR;
        boolean   isNull = true;
        String    sValue = null;
        Timestamp dValue = null;
        int       iValue;
        long      lValue;
        
        public void set(String value) throws ParseException {
            set(java.sql.Types.VARCHAR, value);
        }
        public void set(Date value) {
            set(java.sql.Types.TIMESTAMP, value);
        }
        public void set(java.sql.Date value) {
            type   = java.sql.Types.DATE;
            dValue = new Timestamp(value.getTime());
        }
        public void set(java.sql.Time value) {
            type   = java.sql.Types.TIME;
            dValue = new Timestamp(value.getTime());
        }
        public void set(java.sql.Timestamp value) {
            type   = java.sql.Types.TIMESTAMP;
            dValue = value;
        }
        public void set(int type, Date value) {
            this.type   = type;
            this.sValue = null;
            this.dValue = new Timestamp(value.getTime());
        }
        public void set(int type, String value) throws ParseException {
            this.type   = type;
            this.isNull = value == null;
            this.sValue = value;
            
            if (type == java.sql.Types.DATE || type == java.sql.Types.TIMESTAMP || type == java.sql.Types.TIME) {
                String fields[] = value.split("\\.");
                
                dValue = new Timestamp(DateFormatter.parseDate(fields[0]).getTime());
                
                if (fields.length == 2) {
                    double n = 1000000 * Double.parseDouble("0." + fields[1]);
                    
                    dValue.setNanos((int) n);
                }
                
                if (fields.length > 2) throw new ParseException(value + " is not a valid timestamp", 0);
            }
        }
        public void set(int type, int value) throws ParseException {
            iValue = value;
            set(type, "" + value);
        }
        public void set(int type, long value) throws ParseException {
            lValue = value;
            set(type, "" + value);
        }
        public void set(int index, PreparedStatement statement) throws SQLException {
            if (isNull) {
                statement.setNull(index, type);
            } else {
                switch (type) {
                    case java.sql.Types.VARCHAR:
                        statement.setString(index, sValue);
                        break;
                    case java.sql.Types.INTEGER:
                        statement.setInt(index, iValue);
                        break;
                    case java.sql.Types.BIGINT:
                        statement.setLong(index, lValue);
                        break;
                    case java.sql.Types.DATE:
                        statement.setDate(index, new java.sql.Date(dValue.getTime()));
                        break;
                    case java.sql.Types.TIME:
                        statement.setTime(index, new java.sql.Time(dValue.getTime()));
                        break;
                    case java.sql.Types.TIMESTAMP:
                        statement.setTimestamp(index, dValue);
                        break;
                    default:
                        statement.setString(java.sql.Types.VARCHAR, sValue);
                }
            }
        }
        public String get() {
            DateFormatter df = new DateFormatter();
            
            if (isNull) return "NULL";
            
            switch (type) {
                case java.sql.Types.VARCHAR:
                    return '\'' + sValue + '\'';
                case java.sql.Types.DATE:
                    return '\'' + df.format(dValue, "yyyy-MM-dd") + '\'';
                case java.sql.Types.TIME:
                    return '\'' + df.format(dValue, "HH:mm:ss") + '\'';
                case java.sql.Types.TIMESTAMP: {
                    String ts = df.format(dValue, "yyyy-MM-dd HH:mm:ss");
                    
                    if (dValue.getNanos() != 0) {
                        String [] fr = Double.toString(dValue.getNanos() / 1000000.0).split("\\.");
                        
                        while (fr[1].length() < 3) fr[1] += '0';
                        
                        ts = ts + '.' + fr[1];
                    }
                    return '\'' + ts + '\'';
                }
                case java.sql.Types.INTEGER:
                case java.sql.Types.BIGINT:
                    return sValue;
                default:
                    return '\'' + sValue + '\'';
            }
        }
    }
    private ParameterValue getValue(String name) {
        ParameterValue value = parameters.get(name);
        
        if (value == null) {
            value = new ParameterValue();
            parameters.put(name, value);
        }
        return value;
    }
    /*
     * This method sets a parameter without identifying the column type, which defaults to 
     * a VARCHAR. This will work depending on how the database handles the type conversion. 
     * It is better to specify the column type, to ensure that the database handles the value in the
     * optimal way, e.g. on SQL Server testing a INT column with a VARCHAR will work, provided the
     * value is a valid integer, but it may mean that an index on the column is not used.
     */
    public void setParameter(String name, String value) throws ParseException {
        getValue(name).set(java.sql.Types.VARCHAR, value);
    }
    /*
     * Sets parameter name with column type to value.
     *
     * Only the types VARCHAR, DATE, TIME, TIMESTAMP, INT and BIGINT are fully supported. All other
     * types are passed as a VARCHAR. Depending on the database, this may work, i.e. the database may convert
     * the string to the correct column value.
     *
     * For dates the string is parsed and converted to corresponding sql type. An attempt is
     * made to deduce the correct format string from the value content and may not end up with the
     * correct result. A value of the form yyyy-MM-dd HH:mm:ss.ffff will produce the correct result, the date or
     * fractional time can be ommitted. If the date field start with a one or 2 character field it is assumed to be a day
     * and the third a year, otherwise, the first is assumed to be a year and the third a year. If the second field is less
     * than 3 character it is assumed to be a numeric month, otherwise a 3 character alpha month. The time field, if present,
     * can have the secords, or minutes and seconds sub-fields omitted. No check is made that the date string is consistent
     * with type, e.g. if type is TIMESTAMP, the string can be just a time. If date is not provided, it defaults to 01-Jan-1970
     * and if time is not provided, it defaults to 00:00:00. Fractional seconds default to 000. 
     */
    public void setParameter(String name, int type, String value) throws ParseException {
        getValue(name).set(type, value);
    }
    public void setParameter(String name, int type, int value) throws ParseException {
        getValue(name).set(type, value);
    }
    /*
     * Sets a Timestamp column type parameter.
     *
     * 
     */
    public void setParameter(String name, Date value) throws ParseException {
        getValue(name).set(java.sql.Types.TIMESTAMP, value);
    }
    public void setParameter(String name, int type, Date value) throws ParseException {
        getValue(name).set(type, value);
    }
    /*
     * Expands the parameters in resolve. 
     *
     * If prepare is true the names are replaced with ?, otherwise they are replaced with
     * the parameter value.
     *
     * Note: Parameter usage in the WHERE clause has to take account of a situation where the
     *       value can be NULL, e.g. Col = ?Name will not work if ?Name is NULL, it should be replaced by Col IS NULL.
     *       The above test can be change to Col = $Name OR (?Name IS NULL AND A IS NULL). However, this can impact
     *       on the database optimiser's ability to find the best plan.
     */
    public String resolve(String sql, boolean prepare) throws SQLException {
        boolean       inParam = false;
        StringBuilder param   = new StringBuilder();
        StringBuilder eSql    = new StringBuilder();
        
        uses.clear();
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            
            if (inParam) {
                
                if (c == ' ' || c == ')' || i == sql.length() - 1) {
                    /*
                     * Parameter name is terminated by a space or if last character in SQL string. If
                     * last character append it to the parameter name.
                     */
                    if (c != ' ' && c != ')') {
                        param.append(c);
                        c = 0;
                    }
                    
                    String         name  = param.toString();
                    ParameterValue value = parameters.get(name);
                    
                    if (value == null) throw new SQLException("Parameter ?" + name + " has not been set");
                    /*
                     * If creating prepared statement, replace parameter by ?, otherwise by its value.
                     */
                    if (prepare) {                        
                        uses.add(name);
                        eSql.append('?');
                    } else {
                        eSql.append(value.get());
                    }
                    if (c != 0) eSql.append(c);
                    
                    inParam = false;
                    param.setLength(0);
                } else
                    param.append(c);
            } else {
                if (c == paramSep) {
                    inParam = true;
                    param.setLength(0);
                } else
                    eSql.append(c);
            }
        }
        return eSql.toString();
    }    
    public String resolve(boolean prepare) throws SQLException {
        return resolve(build(), prepare);
    }
    public void setParameters(PreparedStatement statement) throws SQLException {
        statement.clearParameters();
        
        for (int i = 0; i < uses.size(); i++) {
            parameters.get(uses.get(i)).set(i + 1, statement);
        }
    }
}
