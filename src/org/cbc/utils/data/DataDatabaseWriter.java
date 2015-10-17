/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.data;

import org.cbc.utils.system.Logger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 *
 * @author cclose
 */
public class DataDatabaseWriter  extends DataOutputStream {
    
    DatabaseSession db = new DatabaseSession();
    PreparedStatement pStat = null;
    String table = null;
    boolean exists = true;
    StringBuffer create = new StringBuffer();
    int batchSize = 1000;
    int batchCount = 0;
        
    public DataDatabaseWriter(Logger logger) {
        super(logger);
    }
    String reportPosition() {
        return "Table " + table + '(' + count + ") ";
    }
    
    private void commit() throws SQLException {
        if (batchCount != count) {
            pStat.executeBatch();
            db.getConnection().commit();
        }
        batchCount = 0;
    }
    private void setAvailableColumnsOld() {
        for (int i = 0; i < getColumnCount(); i++) {
            DataField field = getColumn(i);
            try {
                field.setHidden(!db.columnExists(table, field.getHeading()));
            } catch (SQLException ex) {
                reportError("Set available on " + table + '.' + field.getHeading(), ex);
            }
        }
    }
    private void setAvailableColumns() {
        try {
            HashMap<String, DatabaseSession.Column> columns = db.getColumns(table);
            
            for (int i = 0; i < getColumnCount(); i++) {
                DataField field = getColumn(i);
                
                field.setHidden(!columns.containsKey(field.getHeading()));
            }
        } catch (SQLException ex) {
             reportError("Set available on " + table, ex);
        }
    }
    private void prepare() throws SQLException {
        StringBuffer insert = new StringBuffer("INSERT INTO " + table + " (");
        StringBuffer params = null;

        if (exists) setAvailableColumns();
        
        for (int i = 0; i < getColumnCount(); i++) {
            DataField field = getColumn(i);

            if (!field.isHidden()) {
                if (params != null) {
                    insert.append(',');
                    params.append(',');
                }
                else
                    params = new StringBuffer();
                
                insert.append(db.delimitName(field.getHeading()));
                params.append('?');
            }
        }
        if (!exists) {
            db.createTable(create);
            comment("Assigned " + description + " to new table " + table);
            create = null;
        } else
            comment("Assigned " + description + " to table " + table);
        
        insert.append(") VALUES (");
        insert.append(params.toString());
        insert.append(')');
        pStat = db.getConnection().prepareStatement(
                    insert.toString(), 
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_UPDATABLE);
    }
    private void logField(DataField field) {
        for (int i = 0; i < getColumnCount(); i++) {
            field = getColumn(i);
            log.comment(field.getId() + '=' + field.getValue());
        }
    }
    public void writeRow() {
        DataField field = null;
        int index = 0;
        
        try {
            for (int i = 0; i < getColumnCount(); i++) {
                field = getColumn(i);

                if (!field.isHidden()) {
                    if (field.isNull() || field.isEmpty())
                    {
                        pStat.setNull(index + 1, field.getSqlType());
                    }
                    else
                        pStat.setString(index + 1, field.getValue());
                    index++;
                }
            }
            pStat.addBatch();
            
            if (batchCount++ >= batchSize) commit();
        } catch (SQLException ex) {
            reportError("Insert " + table, ex);
        }
        discard();
    }

    public DataField addColumn(String name, String heading, String type, int size, int precision) {
        DataField field = getColumn(name, false);
        
        if (field != null) return field;
        
        field = super.addColumn(name, heading, type);
        field.setSqlType(java.sql.Types.VARCHAR);
        
        if (type.equalsIgnoreCase("smallint"))
            field.setSqlType(java.sql.Types.SMALLINT);
        else if (type.equalsIgnoreCase("int"))
            field.setSqlType(java.sql.Types.INTEGER);
        else if (type.equalsIgnoreCase("bigint"))
            field.setSqlType(java.sql.Types.BIGINT);
        else if (type.equalsIgnoreCase("double"))
            field.setSqlType(java.sql.Types.DOUBLE);
        else if (type.equalsIgnoreCase("string")) {
            field.setSqlType(java.sql.Types.VARCHAR);
            
            if (size <= 0) size = 4000;
        }
        else if (type.equalsIgnoreCase("timestamp"))
            field.setSqlType(java.sql.Types.TIMESTAMP);
        else if (type.equalsIgnoreCase("time"))
            field.setSqlType(java.sql.Types.TIME);
        else {
            reportError("Data type " + type + " is not supported");
        }
        try {
            db.addColumn(create, field.getHeading(), field.getSqlType(), size, precision);
        } catch (SQLException ex) {
            reportError("Add column " + name + " to " + table, ex);
        }
        
        return field;
    }
    public DataField addColumn(String name, String heading, String type) {
        return addColumn(name, heading, type, -1, -1);
    }
    public DataField addColumn(String name, String type) {
        return addColumn(name, null, type);
    }
    public DataField addColumn(String name) {
        return super.addColumn(name, null, "");
    }
    public void closeHeaders() {
        try {
            prepare();
        } catch (SQLException ex) {
            reportError(create != null? "Create" : "Open" + table, ex);
        }
        super.closeHeaders();
    }
    public void setDatabaseSession(DatabaseSession session) {
        db = session;
        try {
            db.getConnection().setAutoCommit(false);
        } catch (SQLException ex) {
            reportError("Set auto commit on " + table, ex);
        }
    }
    public void open(String table, String description) {
        this.table = table;
        this.description = description;
            
        create = db.initialiseCreateTable(table);
    }
    public void setCreate() {
        exists = false;
    }
    public boolean getExists() {
        return exists;
    }
    public void setBatchSize(int size) {
        batchSize = size;
    }
    public void close() {
        try {
            commit();
            super.close();
        } catch (SQLException ex) {
            reportError("Close of " + table, ex);
        }
    }
    public String getTable() {
        return table;
    }
}
