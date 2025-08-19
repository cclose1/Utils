/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.sql;

import java.sql.SQLException;

/**
 *
 * @author Chris
 */
public class SQLSelectBuilder extends SQLBuilder {
    /*
    protected class Field extends SQLBuilder.Field {
    }
    */
    private String options = null;
    private String from    = null;
    private String orderBy = null;
    private String groupBy = null;
    private int    maxRows = -1;
    
    public SQLSelectBuilder(String table, String protocol) {
        this.table    = table;
        this.protocol = protocol;
    }
    public void setOptions(String options) {
        this.options = options;
    }
    public void setMaxRows(int rows) {
        this.maxRows = rows;
    }
    public void clearOrderBy() {
        orderBy = null;
    }
    @Override
    public void clear() {
        super.clear();
        clearOrderBy();
        maxRows = -1;
    }            
    protected class Cast {
        private String type;
        private int    precision = 0;
        private int    scale     = 0;
        
        protected Cast(String type, int precision, int scale) {
            this.type      = type.equalsIgnoreCase("varchar") && protocol.equalsIgnoreCase("mysql")? "CHAR" : type;
            this.precision = precision;
            this.scale     = scale;
        }
        @Override
        public String toString() {
            if (precision < 0) return type;
            if (scale     < 0) return type + '(' + precision + ')';
            
            return type + '(' + precision + ", " + scale + ')';
        }
    }
    public Cast setCast(String type) {
        return new Cast(type, -1, -1);
    }
    public Cast setCast(String type, int precision) {
        return new Cast(type, precision, -1);
    }
    public Cast setCast(String type, int precision, int scale) {
        return new Cast(type, precision, scale);
    }
    private class Field extends SQLBuilder.Field {
        private String  cast;
        
        public Field(String name) {
            super(name, null, null);
        }
        public Field(String name, Source source, Cast cast, Value value) {
            super(name, source, value);
            
            if (cast != null) this.cast = cast.toString();
        }
        @Override
        protected void setObject(Object obj) throws SQLException {
            if (obj != null && obj.getClass().getSimpleName().equals("Cast"))
                cast = obj.toString();
            else
                super.setObject(obj);
        }
        protected String getCast() {
            return cast;
        }
        protected void setCast(String cast) {
            this.cast = cast;
        }
    }
    protected Field addField(String name, Source source, Cast cast, Value value) {
        Field f = new Field(name, source, cast, value);
        
        fields.add(f);
        
        return f;
    }
    @Override
    public void addField(String name) {
        addField(name, null, null, null);
    } 
    @Override
    public void addField(String name, String source) {
        addField(name, setFieldSource(source), null, null);
    }    
    public void addField(String name, Source source, Cast cast) {
        addField(name, source, cast, null);
    }
    public void addField(String name, Cast cast) {
        addField(name, null, cast, null);
    }
    public void addField(String name, Source source) {
        addField(name, source, null, null);
    }
    @Override
    public void addField(String name, Value value) {
        addField(name, null, null, value);
    }
    public void addField(String name, Object o1, Object o2, Object o3) throws SQLException {
        Field f = addField(name, null, null, null);
        
        f.setObject(o1);
        f.setObject(o2);
        f.setObject(o3);
    }
    public void addField(String name, Object o1, Object o2) throws SQLException {
        addField(name, o1, o2, null);
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public void addOrderByField(String name, boolean desc) {
        if (orderBy == null)
            orderBy = "";
        else
            orderBy += ", ";  
        
        orderBy += delimitName(name);
        
        if (desc) orderBy += " DESC";
    }
    /* 
     * Better to use above method to ensure that field names are correctly deliited.    
     */
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }
    public void addGroupByField(String name) {
        if (groupBy == null)
            groupBy = "";
        else
            groupBy += ", ";  
        
        groupBy += delimitName(name);
    }
    @Override
    public String build() {
        StringBuilder sql = new StringBuilder("SELECT ");
        char          sep = ' ';
        
        if (options != null) sql.append(options);
        
        if (maxRows > 0 && protocol.equalsIgnoreCase("sqlserver")) {
            sql.append(" TOP ");
            sql.append(maxRows);
        }
        if (fields.isEmpty()) {
            sql.append("* ");
        } else {
            for (Object fObj : fields) {    
                Field f       = (Field) fObj;
                String name   = f.getName();
                String source = f.getSource();
                String cast   = f.getCast();
                String value  = f.getValue();
                String alias  = name;
                
                if (source == null) source = name;
                
                if (value != null) source = "COALESCE(" +  source + ", " + value + ")";
                if (cast  != null) source = "CAST(" + source + " AS " + cast + ")";
                
                sql.append(sep);
                sql.append("\r\n    ");
                sql.append(source);
                sep = ',';
                
                if (alias != null && !source.equals(alias)) {
                    if (alias.contains(" ")) alias = "'" + alias + "'";
                    
                    sql.append(" AS ");
                    sql.append(alias);
                }
            }
        }
        addClause(sql, "FROM", from == null? table : from);
        addWhere(sql);          
        addClause(sql, "GROUP BY", groupBy);
        addClause(sql, "ORDER BY", orderBy);
        
        if (maxRows > 0 && !protocol.equalsIgnoreCase("sqlserver")) {
            sql.append(" LIMIT ");
            sql.append(maxRows);
        }
        return sql.toString();
    }
}
