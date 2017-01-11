/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.sql;

import org.cbc.sql.SQLBuilder;

/**
 *
 * @author Chris
 */
public class SQLInsertBuilder extends SQLBuilder {
    public SQLInsertBuilder(String table) {
        this.table = table;
    }
    public SQLInsertBuilder(String table, String protocol) {
        this.table    = table;
        this.protocol = protocol;
    }
    public String build() {
        StringBuilder sql    = new StringBuilder("INSERT " + table + "(\r\n   ");
        StringBuilder values = new StringBuilder(") VALUES (\r\n");
        char          sep    = ' ';
        
        for (Field f : fields) {
            sql.append(sep);
            sql.append("\r\n    ");
            sql.append(f.getName());
            values.append(sep);
            values.append("\r\n    ");
            values.append(f.getValue());
            sep = ',';
        }
        values.append(")");
        sql.append(values);
        addWhere(sql);
        
        return sql.toString();
    }
}
