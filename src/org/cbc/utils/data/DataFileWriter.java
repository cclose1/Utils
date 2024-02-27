package org.cbc.utils.data;

import org.cbc.utils.system.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import org.cbc.utils.system.DateFormatter;
/**
 *
 * @author cclose
 */
public class DataFileWriter extends DataOutputStream {
    private   FileWriter    writer      = null;
    protected File          file        = null;
    private   boolean       writeHeader = true;
    private   char          separator   = ',';
    private   DateFormatter fmtDate     = new DateFormatter("dd-MMM-yy HH:mm:ss");

    public DataFileWriter(Logger logger) {
        super(logger);
    }
    public void setSeparator(char separator) {
        this.separator = separator;
    }
    public void create(File file) throws IOException {
        this.file = file;
        writer = new FileWriter(file);
        writeHeader = true;

        count = 0;
    }
    public void create(String file) throws IOException {
        if (file.indexOf('\'') != -1) {
            file = DateFormatter.format(new Date(), file);
        }
        create(new File(file));
    }
    public void append(File file) throws IOException {
        int ch = 0;

        if (!file.exists())
            create(file);
        else {
            StringBuilder line   = new StringBuilder();
            FileReader    reader = new FileReader(file);

            while ((ch = reader.read()) != -1 && ch != '\n' && ch != '\r') line.append((char)ch);

            String[] columns = line.toString().split("" + separator + "");

            for (String column : columns) addColumn(column);

            reader.close();
            closeHeaders();
            this.file   = file;
            writer      = new FileWriter(file, true);
            writeHeader = false;
            count       = 0;
        }
    }
    public void append(String file) throws IOException {
        if (file.indexOf('\'') != -1) {
            file = DateFormatter.format(new Date(), file);
        }
        append(new File(file));
    }
    public void create() {
        writer = null;
        writeHeader = true;
        count = 0;
    }
    public boolean isOpen() {
        return writer != null;
    }
    public boolean create(File file, String description){
        try {
            this.description = description;
            create(file);
            comment("Assigned " + description + " to " + file.getAbsolutePath());
            return true;
        } catch (IOException ex) {
            reportError("Create " + file.getAbsolutePath(), ex);
        }
        return false;
    }
    public void close() {
        if (!isOpen()) return;
        
        try {
            writer.flush();
            writer.close();
            writer = null;
        } catch (IOException ex) {
            reportError("Close " + description, ex);
        }
        super.close();
    }

    @Override
    String reportPosition() {
        return file == null? "System.out" : file.getName() + '(' + count + ')';
    }

    private String quote(String value) {
        if (value.indexOf(',') == -1) return value;
        
        return '"' + value + '"';
    }
    
    public void writeRow() {
        StringBuilder line = new StringBuilder();
        
        count++;
        
        try {
            for (int i = 0; i < getColumnCount(); i++)
            {
                DataField field = getColumn(i);
                
                if (i != 0) line.append(separator);
                
                line.append(writeHeader? field.getHeading() : quote(field.getValue()));
            }
            if (writer == null)
                System.out.println(line.toString());
            else
                writer.write(line.toString() + "\r\n");
        } catch (IOException ex) {
            log.fatalError(ex);
        }

        if (writeHeader) {
            writeHeader = false;
            writeRow();
        }
        else
            discard();
    }

    public DataField addColumn(String name, String heading, String type, int size, int precision) {
        DataField field = getColumn(name, false);
        
        if (field != null) return field;
        
        return addColumn(name, heading, type);
    }
}
