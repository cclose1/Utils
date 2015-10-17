/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.NotSerializableException;
import java.io.PrintStream;

/**
 *
 * @author cclose
 */
public class Logger implements LogReport {
    private boolean debug = false;

    @Override
    public void setDebug(boolean on) {
        debug = on;
    }

    @Override
    public boolean getBebug() {
        return debug;
    }

    public enum Type {
        Comment, Warning, Error, Fatal
    };
    public interface Interceptor {
        public String modifyMessage(Type type, String message);
    }
    public class FatalError extends RuntimeException {
        static final long serialVersionUID = -3387516993124229948L;

        private String message = "Logger fatal error reported";

        private void readObject() throws NotSerializableException {
            throw new NotSerializableException();
        }

        private void writeObject() throws NotSerializableException {
            throw new NotSerializableException();
        }
        protected FatalError(String message) {
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
        public String toString() {
            return getMessage();
        }
    }
    private class Stream {
        private   PrintStream defaultStream = System.out;
        protected PrintStream stream        = null;
        private   boolean     append        = false;
        protected File        file          = null;
        protected boolean     open          = false;

        public Stream(PrintStream stream) {
            defaultStream = stream;
            open          = false;
            file          = null;
        }
        public void setFile(File file, boolean append) {
            if (open && stream != defaultStream) stream.close();
            
            this.file   = file;
            this.open   = false;
            this.append = append;
        }
        public PrintStream setStream() {
            if (!open) {
                stream = defaultStream;

                if (file != null)
                    try {
                        stream = new PrintStream(new FileOutputStream(file, append));
                    } catch (FileNotFoundException ex) {
                        System.err.println("Unable to assign stream to " + file + " error-" + ex.getMessage());
                        file = null;
                    }
                open = true;
            }
            return stream;
        }
        public void write(String message) {
            setStream();
            stream.println(message);
            stream.flush();
        }
        public boolean isFileStream() {
            return file != null;
        }
    }
    private Timer       timer        = new Timer();
    private LogReport   tLog         = null;
    private Interceptor interceptor  = null;
    private boolean     logException = true;
    private Stream      strErr       = new Stream(System.err);
    private Stream      strOut       = new Stream(System.out);
    private boolean     errors       = false;
    private boolean     warnings     = false;

    private void log(Exception exception) {
        if (logException) exception.printStackTrace(strErr.setStream());
    }
    private void internalReport(Type type, String message) {
        String time = timer.getTime();

        if (interceptor != null) message = interceptor.modifyMessage(type, message);
        if (time.length() != 0) message = time + ' ' + message;
        
        if (type == Type.Error || type == Type.Fatal)
            strErr.write(message);
        else
            strOut.write(message);
    }
    public void setLogException(boolean logException) {
        this.logException = logException;
    }
    public void setErrorStream(String file, boolean append) {
        strErr.setFile(new File(file), append);
    }
    public void setReportStream(String file, boolean append) {
        strOut.setFile(new File(file), append);
    }
    public void setInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }
    public void comment(String message) {
        if (tLog == null)
            internalReport(Type.Comment, message);
        else
            tLog.comment(message);
    }

    public void debug(String message) {
        if (this.debug) comment(message);
    }
    public void warning(String message) {
        warnings = true;

        if (tLog == null)
            internalReport(Type.Warning, "WARNING: " + message);
        else
            tLog.warning(message);
    }

    public void error(String message) {
        errors= true;

        if (tLog == null)
            internalReport(Type.Error, "ERROR: " + message);
        else
            tLog.error(message);
    }
    public void error(String cause, Exception exception) {
        log(exception);
        error(cause + '-' + exception.getMessage());
    }
    public void error(Exception exception) {
        error(exception.getClass().getSimpleName(), exception);
    }
    public void fatalError(String message) {
        errors = true;

        if (tLog == null) {
            internalReport(Type.Fatal, "ABORT: " + message);
            abort(message);
        }
        else
            tLog.fatalError(message);
    }

    public void fatalError(Exception exception) {
        log(exception);
        fatalError(exception.getMessage());
    }

    public void fatalError(String className, Exception exception) {
        log(exception);
        fatalError("In - " + className + '-' + exception.getMessage());
    }
    private void abort(String message) {
        System.err.println(message);
        System.exit(-1);
    }
    public void setTimePrefix(String format) {
        timer.setFormat(format);
    }
    public void setLogger(LogReport logger) {
        tLog = logger;
    }
    public void report(Type type, String message) {
        switch (type) {
            case Comment:
                comment(message);
                break;
            case Warning:
                warning(message);
                break;
            case Error:
                error(message);
                break;
            case Fatal:
                fatalError(message);
                break;
            default:
                fatalError("Report type " + type.toString() + " not implemented-message is " + message);
        }
    }
    public void elapsed(Timer timer, String message) {
        report(Logger.Type.Comment, timer.addElapsed(message));
    }
    public boolean errorsReported() {
        return errors;
    }
    public boolean warningsReported() {
        return warnings;
    }
    public boolean isErrorToFile() {
        return strErr.isFileStream();
    }
    public boolean isOutToFile() {
        return strOut.isFileStream();
    }
}
