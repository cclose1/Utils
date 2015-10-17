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
public class SQLDeleteBuilder extends SQLBuilder {
    public SQLDeleteBuilder(String table) {
        this.table = table;
    }
    public String build() {
        StringBuilder sql = new StringBuilder("DELETE FROM " + table + " ");
        addWhere(sql);
        
        return sql.toString();
    }
}
