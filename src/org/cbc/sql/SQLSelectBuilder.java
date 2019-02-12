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
    public void addField(String name, String alias, String value, String cast) {
        addField(name, value, alias).setCast(cast);
    }
    public void addField(String name, String alias, int value, String cast) {
        addField(name, value, alias).setCast(cast);
    }
    public void addDefaultedField(String name, String nullDefault) {
        addField(name, name, nullDefault, null);
    }
    public void addDefaultedField(String name, String alias, String nullDefault) {
        addField(name, alias, nullDefault, null);
    }
    public void addDefaultedField(String name, int nullDefault) {
        addField(name, name, nullDefault, null);
    }
    public void addDefaultedField(String name, String alias, int nullDefault) {
        addField(name, alias, nullDefault, null);
    }
    public void addDefaultedField(String name, String alias, int nullDefault, int precision) {
        if (precision == 0)
            addField(null, "CAST(COALESCE(" + delimitName(name) + ", " + nullDefault + ") AS " + (protocol.equalsIgnoreCase("mysql")? "SIGNED" : "INT") + ")", alias, false);
        else
            addField(null, "CAST(COALESCE(" + delimitName(name) + ", " + nullDefault + ") AS DECIMAL(12," + precision + "))", alias, false);
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
                
                String id = f.getName();
                
                if (id == null) 
                    id = f.getValue();
                else if (f.getValue() != null) 
                    id = "COALESCE(" +  id + ", " + f.getValue() + ")";
                
                id = f.getCast() != null? "CAST(" + id + " AS " + f.getCast() + ")" : id;
                sql.append(sep);
                sql.append("\r\n    ");
                sql.append(id);
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
