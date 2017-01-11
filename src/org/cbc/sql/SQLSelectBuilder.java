/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.sql;

/**
 *
 * @author Chris
 */
public class SQLSelectBuilder extends SQLBuilder {
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
        addField(name, null, null);
    }
    public void addField(String name, String alias) {
        addField(name, null, alias);
    }
    public void addDefaultedField(String name, String nullDefault) {
        addField(null, "COALESCE(" + name + ", '" + nullDefault + "')", name, false);
    }
    public void addDefaultedField(String name, String alias, String nullDefault) {
        addField(null, "COALESCE(" + name + ", '" + nullDefault + "')", alias, false);
    }
    public void addDefaultedField(String name, int nullDefault) {
        addField(null, "COALESCE(" + name + ", " + nullDefault + ")", name, false);
    }
    public void addDefaultedField(String name, String alias, int nullDefault) {
        addField(null, "COALESCE(" + name + ", " + nullDefault + ")", alias, false);
    }
    public void addDefaultedField(String name, String alias, int nullDefault, int precision) {
        if (precision == 0)
            addField(null, "CAST(COALESCE(" + name + ", " + nullDefault + ") AS " + (protocol.equalsIgnoreCase("mysql")? "SIGNED" : "INT") + ")", alias, false);
        else
            addField(null, "CAST(COALESCE(" + name + ", " + nullDefault + ") AS DECIMAL(12," + precision + "))", alias, false);
    }
    public void addDefaultedField(String name, int nullDefault, int precision) {
        addDefaultedField(name, name, nullDefault, precision);
    }
    public void addValueField(String alias, String value) {
        addField(null, value, alias);
    }
    public void addValueField(String alias, String value, boolean quoted) {
        addField(null, value, alias, quoted);
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }
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
            for (Field f : fields) {
                sql.append(sep);
                sql.append("\r\n    ");
                sql.append(f.getName() == null? f.getValue() : f.getName());
                sep = ',';
            
                if (f.getAlias() != null) {
                    sql.append(" AS ");
                    sql.append(f.getAlias());
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
