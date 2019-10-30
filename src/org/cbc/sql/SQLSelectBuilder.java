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
    private int    maxRows = -1;
    
    public SQLSelectBuilder(String table) {
        this.table = table;
    }
    public SQLSelectBuilder(String table, String protocol) {
        this.table    = table;
        this.protocol = protocol;
    }
    public SQLSelectBuilder() {
    }
    public void setOptions(String options) {
        this.options = options;
    }
    public void setMaxRows(int rows) {
        this.maxRows = rows;
    }
    public void addField(String name) {
        addField(name, (Source)null, null);
    }
    /*
     * Must override as SQLBuilder addField set the value rather than the alias
     */
    @Override
    public void addField(String name, String source) {
        addField(name, setFieldSource(source));
//        addField(source, null, name, false);
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
    public void addField(String name, Value value) {
        addField(name, null, null, value);
    }
    public void addField(String name, Object o1, Object o2, Object o3) throws SQLException {
        Field f = addField(name, null, false);
        
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
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
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
            for (Object x : fields) {    
                Field f = (Field) x;
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
                    sql.append(" AS ");
                    sql.append(alias);
                }
            }
        }
        addClause(sql, "FROM", from == null? table : from);
        addWhere(sql);       
        addClause(sql, "ORDER BY", orderBy);
        
        if (maxRows > 0 && !protocol.equalsIgnoreCase("sqlserver")) {
            sql.append(" LIMIT ");
            sql.append(maxRows);
        }
        return sql.toString();
    }
}
