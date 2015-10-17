package org.cbc.utils.data;

import org.cbc.utils.system.Logger;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author CClose
 */
public abstract class DataOutputStream extends DataStream {
    public abstract void writeRow();
    
    public DataOutputStream(Logger logger) {
        super(logger);
    }
    public void close() {
        if (description != null && description.length() != 0) log.elapsed(timer, "Output of " + description);
    }
    public abstract DataField addColumn(String name, String heading, String type, int size, int precision);
  
}
