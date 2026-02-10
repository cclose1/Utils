/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cbc.filehandler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import org.cbc.Utils;
import org.cbc.utils.system.DateFormatter;
import org.cbc.utils.system.StringFormatter;

/**
 *
 * @author chris
 */
    public class FileOutput { 
        public class FileOutputError extends RuntimeException {
            private static final long serialVersionUID = 1L;

            public FileOutputError(String message) {
                super(message);
            }
        }
        public class Value {
            private String  type;
            private String  strVal;
            private boolean boolVal;
            private Date    dateVal;
            private double  dblVal;
            private String  format;
            private int     places;

            public Value(String value) {
                type   = "String";
                strVal = value;
            }
            public Value(boolean value) {
                type    = "boolean";
                boolVal = value;                
            }
            public Value(Date date, String datFormat) {
                type   = "Date";
                format = datFormat;
            }
            public Value(Date date) {
                this(date, "dd-MMM-yyy HH:mm");
            }
            public Value(double value, int places) {
                this.type   = "double";
                this.dblVal = value;
                this.places = places;
            }
        }
        public interface FieldValue {
            public Value getValue(String id);
        }
        private class Column {
            private String id = "";
            private String title;
            private int    width     = -1;
            /*
             * Id    The column identifier. Currently there is no use for this.
             * Title The column title sent to the output file. If empty string it is set to Id.
             * Width The column width. If the column value length is smaller than it is space padded to this
             *       width. If Width is negative it is spaced to the right, otherwise it is spaced padded
             *       to the left.
            */
            private Column(String id, String title, int width) {
            this.title = "";
                this.id    = id;
                this.title = title.isEmpty()? id : title;
                this.width = width;
            }
        }
        private int               fldIndex;
        private boolean           hdrOutput     = false;
        private ArrayList<Column> columns       = new ArrayList<>();
        private StringFormatter   sf;
        private String            root          = System.getenv("AR_ROOT");
        private FileWriter        writer        = null;   
        private FieldValue        valueProvider = null;
        /*
         * Adds field to the line string. 
         *
         * If columns have been defined, the field string will be padded according to the column width.
         *
         * If columns are defined and fldIndex is beyond the last column, errorExit occurs.
         */
        private void append(String field) {
            if (!columns.isEmpty()) {
                if (fldIndex >= columns.size()) throw new FileOutputError("FileOutput field " + field + " exceeds maximum line fields " + columns.size());                
           
                Column fld = columns.get(fldIndex++);
            
                if (fld.width < 0) 
                    field = Utils.rpad(field, -fld.width);
                else
                    field = Utils.lpad(field, fld.width);                
            }
            sf.add(field);
        }
        /*
         * If there are columns and they have not already output i.e hdrOutput is false, they are
         * output and hdrOutput is set to false.
         */
        private void outputHeader() throws IOException {
            if (hdrOutput || columns.isEmpty()) return;
            
            fldIndex  = 0;
            hdrOutput = true; // Set here to prevent append causing a recursive loop as it call this method.
            
            for (Column col : columns) {
                append(col.title);
            }
            writeLine();
        }
        public boolean isFileOpen() {
            return writer != null;
        }            
        /*
         * Sets StringFormatter separator. 
         *
         * Setting separator to the empty string is allowed, but possibly not useful. It can be more than one
         * character, but again probably not useful.
         */
        public void setValueProvider(FieldValue provider) {
            valueProvider = provider;
        }
        /*
         * Defaults StringFormatter separator to ,
         */
        public FileOutput(String path, String separator, Date fileTime) throws IOException {
            openFile(path, fileTime);            
            sf = new StringFormatter(separator);
        }
        public FileOutput(String path, String separator) throws IOException {
            this(path, separator, new Date());
        }
        /*
         * The root directory to path for the expansion relative files. If path is the empty string the
         * root is the default working directory.
         *
         * This defaults to the environment variable AR_ROOT.
         */
        public void setRoot(String path) {
            this.root = path;
        }
        /*
         * Creates a file using path. If path contains the string !Date it is replaced timestamp
         * formatted as ddMMMyy't'HHmmss.
         *
         * If path is not absolute and root is not "", path is appended to it.
         *
         * The writer is opened with the absolute path resulting from the above. This is also returned to
         * the caller.        
         */
        public final String openFile(String path, Date timestamp) throws IOException {
            java.io.File file = new java.io.File(path.replace("!Date", DateFormatter.format(timestamp, "ddMMMyy't'HHmmss")));
            
            if (!file.isAbsolute() && !"".equals(root)) file = new java.io.File(root, file.getPath());
            
            writer = new FileWriter(file.getAbsolutePath(), true);
            
            return file.getAbsolutePath();
        }
        /*
         * As above with timestamp set the current time.
         */
        public String openFile(String path) throws IOException {
            return openFile(path, new Date());
        }
        /*
         * See comment on constructor for private class Columm for explanation of parameters.
         */
        public void addColumn(String id, String title, int width) {
            Column col = new Column(id, title, width);
            columns.add(col);
        }
        public void addColumn(String id, int width) {
            addColumn(id, "", width);
        }
        /*
         * Create column for id, left justified to length of id.
         */
        public void addColumn(String id) {
            addColumn(id, "", -id.length());
        }
        public void add(String field) throws IOException {
            outputHeader();
            append(field == null? "" : field);
        }
        public void add(Date field, String format) throws IOException {
            add(DateFormatter.format(field, format));
        }
        public void add(Date field) throws IOException {
            add(field, "dd-MMM-yyy HH:mm");
        }
        public void add(double field, int places) throws IOException {
            add(Utils.format(field, places));
        }
        public void add(boolean field) throws IOException {            
            add(field? "Y" : "N");
        }
        public void addFields() throws IOException {
            if (valueProvider == null) throw new FileOutputError("addFields requires a values provider");
            
            for (Column col : columns) {
                Value value = valueProvider.getValue(col.id);
                
                if (value == null) throw new FileOutputError("Field " + col.id + " did not return a value");
                
                switch (value.type) {
                    case "String":
                        add(value.strVal);
                        break;
                    case "Date":
                        add(value.dateVal, value.format);
                        break;
                    case "boolean":
                        add(value.boolVal);
                        break;
                    case "double":
                        add(value.dblVal, value.places);
                        break;
                    default:
                        throw new FileOutputError("Data type " + value.type + " is not support");
                }
            }
            writeLine();
        }
        public void writeLine() throws IOException {            
            writer.append(sf.getString() + "\n");
            writer.flush();
            sf.clear();
            fldIndex = 0;
        }
        public void close() throws IOException {
            writer.close();
            writer = null;
        }
    }
