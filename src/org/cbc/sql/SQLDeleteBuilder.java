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
public class SQLDeleteBuilder extends SQLBuilder {
    public SQLDeleteBuilder(String table, String protocol) {
        this.table    = table;
        this.protocol = protocol;
    }
    @Override
    public String build() throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE FROM " + table + " ");
        
        if (where == null) throw new SQLException("No where clause for DELETE FROM " + table);
        addWhere(sql);
        
        return sql.toString();
    }
}
