/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.system;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author cclose
 */
public class Timer {
    SimpleDateFormat datefmt   = null;
    NumberFormat     format    = NumberFormat.getInstance();
    private long     startTime = System.nanoTime();
    private boolean  autoReset = true;

    public Timer() {
        setFractionalDigits(3);
    }
    public void setFractionalDigits(int precision) {
        format.setMinimumFractionDigits(precision);
        format.setMaximumFractionDigits(precision);
    }
    public String getTime() {
        return datefmt == null? "" : datefmt.format(new Date());
    }
    public double getElapsed() {
        long   end     = System.nanoTime();
        double elapsed = (end - startTime) / 1000000000.0;
        
        if (autoReset) startTime = end;

        return elapsed;
    }
    public double getElapsed(int precision) {
        return getElapsed();
    }
    public String addElapsed(String message) {
        return message + " took " + format.format(getElapsed()) + " seconds";
    }
    public String addElapsed(String message, int precision) {
        setFractionalDigits(precision);
        return addElapsed(message);
    }
    public void setFormat(String format) {
        datefmt = new SimpleDateFormat(format);
    }
    public Date reset() {
        startTime = System.nanoTime();
        
        return new Date();
    }
    public void setAutoReset(boolean flag) {
        autoReset = flag;
    }
}
