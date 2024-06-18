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
public class SQLUpdateBuilder extends SQLBuilder {
    public SQLUpdateBuilder(String table, String protocol) {
        this.table    = table;
        this.protocol = protocol;
    }
    public void addIncrementField(String name, int increment) {
        addField(name, name + (increment < 0? '-' : '+') + increment, false);
    }
    public void addIncrementField(String name, double increment) {
        addField(name, name + '+' + increment, false);
    }
    @Override
    public String build() {
        StringBuilder sql = new StringBuilder("UPDATE " + table + "\r\n SET");
        char          sep = ' ';
        
        for (Field f : fields) {
            sql.append(sep);
            sql.append("\r\n    ");
            sql.append(f.getName()).append(" = ").append(f.getValue());
            sep = ',';
        }
        addWhere(sql);
        
        return sql.toString();
    }
}
