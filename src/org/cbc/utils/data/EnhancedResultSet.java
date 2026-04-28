/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cbc.utils.data;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
/*
 * A wrapper for a Result set allowing access the column values in a row and the associated meta data.
 *
 * The user of this class provides the result set and the column number of the target. These can be retrieved
 * although it should not be necessary as the user should have the result set and know the last column accessed. 
 * However, this class can be passed to another method, thus providing it with these parameters.
 *
*/
public class EnhancedResultSet {
    private ResultSet         rs;
    private ResultSetMetaData md;      
    private int               column;

    public EnhancedResultSet(ResultSet rs) throws SQLException {
        this.rs     = rs;
        this.md     = rs.getMetaData();
        this.column = -1;
    }
    public int getColumnCount() throws SQLException {
        return rs.getMetaData().getColumnCount();
    }
    public int getColumnIndex(String name) throws SQLException {
        for (int col = 1; col <= getColumnCount(); col++) {
            if (md.getColumnName(col).equalsIgnoreCase(name)) return column;
        }
        return -1;
    }
    public boolean existsColumn(String name) throws SQLException {
        return getColumnIndex(name) != -1;
    }
    public int setColumnIndex(String name, boolean mustExist) throws SQLException {
        column = getColumnIndex(name);
        
        if (column == -1 && mustExist) throw new SQLException("Column " + name + " does not exist");
      
        return column;
    }
    public boolean nextRow() throws SQLException {
        return this.rs.next();
    }
    public ResultSet getResultSet() {
        return this.rs;
    }
    public void setColumn(int column) {
        this.column = column;
    }
    public int getColumn() {
        return this.column;
    }
    public String getTableName() throws SQLException {
        return rs.getMetaData().getTableName(1);
    }
    public String getName() throws SQLException {
        return rs.getMetaData().getColumnLabel(column);
    }
    public String getType() throws SQLException {
        return rs.getMetaData().getColumnTypeName(column).toLowerCase();
    }

    public String getValue() throws SQLException {
        return rs.getString(column);
    }

    public int getPrecision() throws SQLException {
        return rs.getMetaData().getPrecision(column);
    }

    public int getScale() throws SQLException {
        return rs.getMetaData().getScale(column);
    }

    public int getIsNullable() throws SQLException {
        return rs.getMetaData().isNullable(column);
    }
}
