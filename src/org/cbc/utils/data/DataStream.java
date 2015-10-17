/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.data;

import org.cbc.utils.system.Logger;
import org.cbc.utils.system.Timer;

/**
 *
 * @author CClose
 */
public abstract class DataStream extends DataRow {
    protected long    count;
    protected String  description = "";
    protected Timer   timer = new Timer();
    private   boolean debug = false;
    
    abstract String reportPosition();
    
    public DataStream(Logger logger) {
        super(logger);
    }
    private void report(Logger.Type type, String message) {
        log.report(type, (type == Logger.Type.Comment? "" : reportPosition() + ' ') + message);
    }
    private void report(Logger.Type type, DataField field, String message) {
        report(type, "Field " + field.getId() + ' '+ message);
    }
    public void comment(String message) {
        report(Logger.Type.Comment, message);
    }
    public void reportWarning(DataField field, Exception ex) {
        report(Logger.Type.Warning, field, ex.toString());
    }
    public void reportError(DataField field, String message) {
        report(Logger.Type.Fatal, field, message);
    }
    public void reportError(String message) {
        report(Logger.Type.Fatal, message);
    }
    public void reportError(String message, Exception ex) {        
        if (debug) ex.printStackTrace();
        
        report(Logger.Type.Fatal, (message == null? "" : message + " failed-") + ex.getMessage());
    }
    public void reportError(Exception ex) {
        reportError(null, ex.getMessage());
    }
    public void reportError(DataField field, Exception ex) {        
        if (debug) ex.printStackTrace();

        report(Logger.Type.Fatal, field, ex.toString());
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
