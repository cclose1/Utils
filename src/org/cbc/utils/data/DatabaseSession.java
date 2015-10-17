/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.data;

import org.cbc.utils.system.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import org.cbc.application.reporting.Report;

/**
 *
 * @author CClose
 */
public class DatabaseSession {

    private static String pad(ResultSet rs, int max, int column, String value) throws SQLException {
        int size = rs.getMetaData().getColumnDisplaySize(column);
        
        if (size > max) size = max;
        
        while (value.length() <= size) value = ' ' + value;
        
        return value;
    }       
    public static void appendEscaped(StringBuilder buffer, String value) {
        char c[] = value.toCharArray();
        
        for (int i = 0; i < value.length(); i++) {
            if (c[i] == '\'') buffer.append(c[i]);
            
            buffer.append(c[i]);
        }
    }
    public static String escape(String value) {
        StringBuilder buffer = new StringBuilder();
        
        appendEscaped(buffer, value);
        return buffer.toString();
    }
    public static void log(ResultSet rs, int maxColumSize) throws SQLException {
        int           columns = rs.getMetaData().getColumnCount();
        StringBuilder line    = new StringBuilder();
        
        for (int i = 1; i <= columns; i++) {
            line.append(pad(rs, maxColumSize, i, rs.getMetaData().getColumnLabel(i)));
        }
        Report.comment(null, line.toString());
        while (rs.next()) {
            line.setLength(0);
            
            for (int i = 1; i <= columns; i++) {
                line.append(pad(rs, maxColumSize, i, rs.getString(i)));
            }
            Report.comment(null, line.toString());
        }
    }
    
    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @return the database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }
    public enum Error {
        Duplicate,
        NotStandard,
        Deadlock;
    }
    public class DatabaseStatistics {
        private String name;
        private double size;
        private double unallocated;

        public String getName() {
            return name;
        }
        public double getSize() {
            return size;
        }
        public double getFree() {
            return unallocated;
        }
        public double getUsed() {
            return size - unallocated;
        }
    }
    public class Column {
        private String name;
        private int position;
        private int type;
        private String typeName;

        protected Column(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }

        public int getPosition() {
            return position;
        }

        protected void setPosition(int position) {
            this.position = position;
        }

        public int getType() {
            return type;
        }

        protected void setType(int type) {
            this.type = type;
        }

        protected void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName(String typeName) {
            return typeName;
        }
    }
    private Connection connection          = null;
    private String     server              = null;
    private String     database            = null;
    private String     user                = null;
    private String     password            = null;
    private String     version             = null;
    private boolean    reportException     = true;
    private boolean    querying            = false;
    private Logger     log                 = new Logger();
    private String     protocol            = null;
    private int        updateResultSetType = ResultSet.TYPE_FORWARD_ONLY;
    private int        statementTimeout    = 0;
    private int        defaultIsolation    = 0;

    public String delimitName(String name) {
        if (name.equalsIgnoreCase("transaction") ||
            name.equalsIgnoreCase("database")    ||
            name.equalsIgnoreCase("index")       ||
            name.equalsIgnoreCase("sent")) return '[' + name + ']';
        
        return name;
    }
    public void logException(Exception exception) {
        if (reportException) {
            log.fatalError(exception);
        }
    }
    private void addConnectParameter(StringBuffer connect, char delimiter, String name, String value) {
        if (value.length() != 0) {
            connect.append(delimiter);
            connect.append(name);
            connect.append('=');
            connect.append(value);
        }
    }
    private Connection createConnection(String server, String database, String user, String password) throws SQLException {
        Connection conn = null;
        
        this.server   = server;
        this.database = database;
        this.user     = user;
        this.password = password;
        this.protocol = "sqlserver";
    
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            StringBuffer params = new StringBuffer("jdbc:sqlserver://" + server);
            
            addConnectParameter(params, ';', "databaseName", database);

            if (user == null || user.length() == 0) {
                addConnectParameter(params, ';',  "integratedSecurity", "true");
            } else {
                addConnectParameter(params, ';',  "user", user);
                
                if (password != null) addConnectParameter(params, ';', "password", password);
            }
            conn = DriverManager.getConnection(params.toString());
            version = conn.getMetaData().getDriverVersion();
            return conn;
        } catch (ClassNotFoundException ex) {
            log.comment("Probably missing the jdbc jar file on the classpath");
            log.fatalError(ex);
        }
        return null;
    }
    public boolean requiresStrongType() {
        return !protocol.equalsIgnoreCase("sqlserver");
    }
    public void open(String server, String database, String user, String password) throws SQLException {
        connection       = createConnection(server, database, user, password);  
        defaultIsolation = connection.getTransactionIsolation();    
    }
    public void open(String server, String database) throws SQLException  {
        connection = createConnection(server, database, "", "");
    }
    public void open(
            String protocol,
            String driverClass,
            String server,
            String database,
            String user,
            String password) throws SQLException {
        this.protocol = protocol;
        try {
            if (protocol.equalsIgnoreCase("sqlserver")) {
                open(server, database, user, password);
            } else if (protocol.equalsIgnoreCase("jdbc:odbc")) {
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
                updateResultSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;
                connection          = DriverManager.getConnection("jdbc:odbc:Driver=" + server + ";" + database);
                defaultIsolation    = connection.getTransactionIsolation();
            } else {
                Class.forName(driverClass);
                StringBuffer params = new StringBuffer("jdbc:" + this.getProtocol() + "://" + server + "/" + database);

                addConnectParameter(params, '?', "user", user);
                addConnectParameter(params, '&', "password", password);
                connection       = DriverManager.getConnection(params.toString());
                defaultIsolation = connection.getTransactionIsolation();
                /*
                 * For PostgreSQL change string handling to stamdard.
                 */
                if (this.getProtocol().equals("postgresql")) executeUpdate("set standard_conforming_strings=on");
            }
        } catch (ClassNotFoundException ex) {
            throw new SQLException("For protocol " + protocol + " class " + driverClass + " not found");
        }
    }
    public void open(Properties properties) throws SQLException {
        Enumeration<?> e          = properties.propertyNames();
        String         protName   = "sqlserver";
        String         protDriver = null;
        String         dbServer   = null;
        String         dbName     = null;
        String         dbUser     = null;
        String         dbPassword = null;
        
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();

            if (key.equalsIgnoreCase("Protocol")) {
                protName = properties.getProperty(key);
            } else if (key.equalsIgnoreCase("Driver")) {
                protDriver = properties.getProperty(key);
            } else if (key.equalsIgnoreCase("Server")) {
                dbServer = properties.getProperty(key);
            } else if (key.equalsIgnoreCase("Database")) {
                dbName = properties.getProperty(key);
            } else if (key.equalsIgnoreCase("User")) {
                dbUser = properties.getProperty(key);
            } else if (key.equalsIgnoreCase("Password")) {
                dbPassword = properties.getProperty(key);
            } else {
                log.fatalError("Opening connection property " + key + " not supported");
            }
        }
        open(protName, protDriver, dbServer, dbName, dbUser, dbPassword);
    }
    public void startTransaction() throws SQLException {
        connection.setAutoCommit(false);
    }
    public void startTransaction(int isolationLevel) throws SQLException {
        connection.setTransactionIsolation(isolationLevel);
        startTransaction();
    }
    public void commit() throws SQLException {
        connection.commit();
        connection.setTransactionIsolation(defaultIsolation);
        connection.setAutoCommit(true);
    }
    public void rollback() throws SQLException {
        if (connection.getAutoCommit()) return;
        
        connection.rollback();
        connection.setTransactionIsolation(defaultIsolation);
        connection.setAutoCommit(true);
    }
    public void setStatementTimeout(int seconds) {
        statementTimeout = seconds;
    }
    public void open(Connection connection) {
        this.connection = connection;
    }
    public Connection getConnection() {
        return connection;
    }
    public void close(){
        if (connection != null) try {
            connection.close();
        } catch (SQLException ex) {
        }
        connection = null;
    }
    public boolean isOpen() {
        return connection != null;
    }
    private Statement getStatement() throws SQLException {
        Statement st = connection.createStatement();

        st.setQueryTimeout(statementTimeout);

        return st;
    }
    public void executeUpdate(String sql) throws SQLException {
        querying = true;

        try {
            getStatement().executeUpdate(sql);
        } catch (SQLException ex) {
            if (reportException) {
                synchronized (this) {
                    querying = false;
                }
                throw ex;
            }
        }
        synchronized (this) {
            querying = false;
            reportException = true;
        }
    }
    public ResultSet executeQuery(String sql) throws SQLException {
        ResultSet results = null;
        
        if (connection == null) return null;
        
        querying = true;
            
        try {
            results = getStatement().executeQuery(sql);
        } catch (SQLException ex) {
            if (reportException) {
                synchronized (this) {
                    querying = false;
                }
                throw ex;
            }
        }

        synchronized (this) {
            querying = false;
            reportException = true;
        }
        return results;
    }
    public ResultSet updateQuery(String sql, int setType) throws SQLException {
        ResultSet results = null;

        if (connection == null) return null;

        querying = true;

        try {
            Statement st = connection.createStatement(setType, ResultSet.CONCUR_UPDATABLE);

            st.setQueryTimeout(statementTimeout);
            results = st.executeQuery(sql);
        } catch (SQLException ex) {
            if (reportException) {
                synchronized (this) {
                    querying = false;
                }
                throw ex;
            }
        }

        synchronized (this) {
            querying = false;
            reportException = true;
        }
        return results;
    }
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return getConnection().prepareStatement(sql);
    }
    public PreparedStatement prepareStatement(String sql, int scrollType, int updateType) throws SQLException {
        return getConnection().prepareStatement(sql, scrollType, updateType);
    }
    public ResultSet updateQuery(String sql) throws SQLException {
        return updateQuery(sql, updateResultSetType);
    }
    public ResultSet insertTable(String table) throws SQLException {
        return updateQuery("SELECT * FROM " + table + " WHERE 1=0", updateResultSetType);
    }
    public void killQuery() {
        synchronized (this) {
            if (querying) {
                try {
                    reportException = false;
                    connection.close();
                    connection = createConnection(this.getServer(), this.getDatabase(), this.user, this.password);
                } catch (SQLException ex) {
                    logException(ex);
                }
            }
        }
    }
    public void load(ArrayList<ArrayList<String>> data, int maxRows, boolean includeHeadings, ResultSet records) throws SQLException {
        int i = 0;
        ArrayList<String> headings = null;

        if (includeHeadings) {
            headings = new ArrayList<String>();
            data.add(headings);
        }
        while ((i++ < maxRows || maxRows == 0) && records.next()) {
            ArrayList<String> row = new ArrayList<String>();

            for (int col = 0; col < records.getMetaData().getColumnCount(); col++) {
                if (i == 1 && headings != null) headings.add(records.getMetaData().getColumnName(col + 1));
                
                row.add(records.getString(col + 1));
            }
            data.add(row);
        }
    }
    public ArrayList<String> getColumn(String sql, String column) {
        ArrayList<String> data = new ArrayList<String>();

        try {
            ResultSet records;
            records = executeQuery(sql);
            
            while (records.next()) {
                for (int col = 0; col < records.getMetaData().getColumnCount(); col++) {
                    if (records.getMetaData().getColumnName(col + 1).equalsIgnoreCase(column)) {
                        data.add(records.getString(col + 1));
                        break;
                    }
                }
            }
        } catch (SQLException ex) {
            logException(ex);
        }
        return data;
    }
    private String getColumnDefinition(String name, int type, int size, int precision) throws SQLException {
        String typeName = "VARCHAR";
        ResultSet types = connection.getMetaData().getTypeInfo();
        
        while (types.next()) {
            if (types.getInt("DATA_TYPE") == type) {
                typeName = types.getString("TYPE_NAME");
                break;
            }
        }
        if (size > 0) {
            /*
             * Can't use '' here because it will be treated as an integer as there are no character types.
             * on right hand side.
             */
            typeName += "(" + size;
            
            if (precision >= 0) typeName += "," + precision;
            
            typeName += ')';
        }
        
        return delimitName(name) + ' ' + typeName;
    }
    public StringBuffer initialiseCreateTable(String table) {
        return new StringBuffer("CREATE Table " + table + '(');
    }
    public void addColumn(StringBuffer createDDL, String column, int type, int size, int precision) throws SQLException {
        if (createDDL.charAt(createDDL.length() - 1) != '(') createDDL.append(",\n");
      
        createDDL.append(getColumnDefinition(column, type, size, precision));
    }
    public void createTable(StringBuffer createDDL) throws SQLException {
        createDDL.append(')');
        connection.createStatement().execute(createDDL.toString());
    }
    public boolean columnExists(String table, String column) throws SQLException {
        ResultSet rs = connection.getMetaData().getColumns(null, null, table, column);
        boolean exists = rs.next();

        rs.close();
        return exists;
    }
    public HashMap<String, Column> getColumns(String table) throws SQLException {
        ResultSet rs = connection.getMetaData().getColumns(null, null, table, "%");
        HashMap<String, Column> columns = new HashMap<String, Column>();
        
        while (rs.next()) {
            Column column = new Column(rs.getString("COLUMN_NAME"));
            
            column.setPosition(rs.getInt("ORDINAL_POSITION"));
            column.setType(rs.getInt("DATA_TYPE"));
            column.setTypeName(rs.getString("TYPE_NAME"));
            columns.put(column.getName(), column);
        }
        rs.close();
        
        return columns;
    }
    public String getVersion() {
        return version;
    }
    private double getSize(String size) {
        String fields[] = size.split(" ");
        double   value    = 0;
        
        value = Double.parseDouble(fields[0]);
        
        if (fields.length > 1) {
            if (fields[1].equalsIgnoreCase("kb")) 
                value *= 1000;
            else if (fields[1].equalsIgnoreCase("mb")) 
                value *= 1000000;
            else if (fields[1].equalsIgnoreCase("gb")) 
                value *= 1000000000;
        }
        return value;
    }
    public DatabaseStatistics getDatabaseStatistics(boolean updateUsage) throws SQLException {
        DatabaseStatistics statistics = new DatabaseStatistics();

        ResultSet db = executeQuery("EXEC sp_spaceused @updateusage = " + (updateUsage? "true" : "false"));

        if (db.next()) {
            statistics.name        = db.getString("database_name");
            statistics.size        = getSize(db.getString("database_size"));
            statistics.unallocated = getSize(db.getString("unallocated space"));
        }
        return statistics;
    }
    public Error getStandardError(SQLException exception) {
        String message = exception.getMessage();
        String state   = exception.getSQLState();
        
        if (getProtocol().equalsIgnoreCase("sqlserver")) {
            switch (exception.getErrorCode()) {
                case 2627:
                    return Error.Duplicate;
                case 1205:
                    return Error.Deadlock;
                default:
                    return Error.NotStandard;
            }
        } else if (getProtocol().equalsIgnoreCase("postgresql")) {    
            if (state.equals("23505")) return Error.Duplicate;
            if (state.equals("40P01")) return Error.Deadlock;
        } else if (getProtocol().equalsIgnoreCase("mysql")) {
            switch (exception.getErrorCode()) {
                case 1022: 
                case 1068:
                    return Error.Duplicate;
                case 1213:
                    return Error.Deadlock;
                default:
                    return Error.NotStandard;
            }
        } else if (message.equals("[Microsoft][ODBC Microsoft Access Driver]Error in row")) {
            /*
             * One of the reasons for this message is trying to insert a row that already exists.
             * However, as there is no better indication of a an insert clash, will assume this denotes a duplicate.
             */
            return Error.Duplicate;
        }
        return Error.NotStandard;
    }
}