/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.htmlreader;

import java.util.ArrayList;

/**
 *
 * @author CClose
 */
public class Table {
    private ArrayList<Row> rows = new ArrayList<Row>();

    public ArrayList<Row> getRows() {
        return rows;
    }
    public void addRow(Row row) {
        rows.add(row);
    }
}
