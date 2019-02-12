/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.system;

/**
 *
 * @author cclose
 */
public interface LogReport {
    public void comment(String message);
    public void warning(String message);
    public void error(String message);
    public void fatalError(String message);
    public void fatalError(Exception exception);
    public void fatalError(String message, Exception exception);
    public void setTimePrefix(String format);
    public void setDebug(boolean on);
    public boolean getBebug();
}
