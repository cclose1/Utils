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
public class Row {
    public enum Type {
        Header,
        Data
    };
    private Type type = Type.Header;
    private ArrayList<String> columns = new ArrayList<String>();

    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }
    public ArrayList<String> getColumns() {
        return columns;
    }
    public void addColumn(String Value) {
        columns.add(Value);
    }
}
