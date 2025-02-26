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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import org.cbc.Utils;
import org.cbc.application.reporting.Report;
import org.cbc.json.JSONArray;
import org.cbc.json.JSONException;
import org.cbc.json.JSONObject;
import org.cbc.utils.system.Timer;

/**
 *
 * @author CClose
 */
public class DatabaseSession {
    private static String pad(ResultSet rs, int max, int column, String value) throws SQLException {
        int size = rs.getMetaData().getColumnDisplaySize(column);

        if (size > max) {
            size = max;
        }

        while (value.length() <= size) {
            value = ' ' + value;
        }

        return value;
    }

    public static void appendEscaped(StringBuilder buffer, String value) {
        char c[] = value.toCharArray();

        for (int i = 0; i < value.length(); i++) {
            if (c[i] == '\'') {
                buffer.append(c[i]);
            }

            buffer.append(c[i]);
        }
    }

    public static String escape(String value) {
        StringBuilder buffer = new StringBuilder();

        appendEscaped(buffer, value);
        return buffer.toString();
    }

    public static void log(ResultSet rs, int maxColumSize) throws SQLException {
        int columns = rs.getMetaData().getColumnCount();
        StringBuilder line = new StringBuilder();

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
        private String          name;
        private boolean         display;
        private String          displayName;
        private int             position;
        private int             pKeyPosition;
        private int             type;
        private String          typeName;
        private String          source;
        private boolean         auto;
        private boolean         generated;
        private boolean         modifiable;
        private boolean         nullable;
        private int             columnSize;
        private int             decimalDigits;
        private TableDefinition table;
        
        protected Column(String name, TableDefinition table) {
            this.name         = name;
            this.display      = !name.equalsIgnoreCase("modified");
            this.modifiable   = true;
            this.pKeyPosition = -1;
            this.table        = table;
        }
        protected Column(String name) {
            this(name, null);
        }
        public String getName() {
            return name;
        }
        public int getPosition() {
            return position;
        }
        private void setPosition(int position) {
            this.position = position;
        }
        /*
         * The column in table providing the possible field values.
         */
        public void setSource(String table, String column) throws SQLException {
            ResultSet rs = connection.getMetaData().getTables(null, null, table, null);
            
            if (!rs.next()) throw new SQLException("Table " + table + " does not exist");
            
            rs = connection.getMetaData().getColumns(null, null, table, column);
            
            if (!rs.next()) throw new SQLException("Column " + column + " does not exist in table " + table);
            
            source = table + "." + column;
        }
        public void setSource(String tableColumn) throws SQLException {
            String fields [] = tableColumn.split(".");
            
            if (fields.length != 2) throw new SQLException("Table column " + tableColumn + "must be of the form table.column");
            
            setSource(fields[0], fields[1]);
        }
        public String getSource() {
            return source;
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
        public String getTypeName() {
            return typeName;
        }
        public String getDisplayName() {
            return displayName == null ? Utils.splitToWords(name) : displayName;
        }
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
            
            if (table != null) table.setMaxDisplayLabel();
        }
        public boolean getDisplay() {
            return isDisplay();
        }
        public void setDisplay(boolean display) {
            this.display = display;
        }
        public boolean isDisplay() {
            return display;
        }
        public boolean isAuto() {
            return auto;
        }
        protected void setAuto(boolean auto) {
            this.auto = auto;
            
            if (auto) {
                modifiable = false;
                display    = false;
            }
        }
        public boolean isGenerated() {
            return generated;
        }
        protected void setGenerated(boolean generated) {
            this.generated = generated;
            
            if (generated) modifiable = false;
        }
        public boolean isModifiable() {
            return modifiable;
        }
        public void setModifiable(boolean modifiable) {
            this.modifiable = modifiable;
        }
        public int getpKeyPosition() {
            return pKeyPosition;
        }
        protected void setpKeyPosition(int pKeyPosition) {
            this.pKeyPosition = pKeyPosition;
        }
        public boolean isPrimeKeyColumn() {
            return pKeyPosition > 0;
        }
        public boolean isNullable() {
            return nullable;
        }
        protected void setNullable(boolean nullable) {
            this.nullable = nullable;
        }
        public int getColumnSize() {
            return columnSize;
        }
        protected void setColumnSize(int columnSize) {
            this.columnSize = columnSize;
        }
        public int getDecimalDigits() {
            return decimalDigits;
        }
        protected void setDecimalDigits(int decimalDigits) {
            this.decimalDigits = decimalDigits;
        }
    }
    /*
     * This is required for the primary key index name. This is often defined by the database, e.g. for MySQL
     * it is PRIMARY, but for SQLServer it starts with PK_. This function may need to be extended to use
     * protocol if other database are to be supported.
     */
    private String normalizeIndexName(String name) {
        return name == null ? null : name.startsWith("PK_") ? "PRIMARY" : name;
    }
    /*
     * Converts a boolean column property to a boolean.
     *
     * SQL Server jdbc does not appear to implement some of the boolean properties. So a default is
     * is applied when an exception occurs.
     */
    private boolean toBoolean(ResultSet rs, String property, boolean defaultValue) throws SQLException {
        try {
            String value = rs.getString(property);
            return value != null && value.equalsIgnoreCase("yes");
        } catch (SQLException ex) {
            return defaultValue;
        }
    }
    private HashMap<String, ArrayList<Column>> getTableIndexes(String catalog, String schemaPattern, String table) throws SQLException {
        Column column;
        String iName;
        HashMap<String, ArrayList<Column>> indexes = new HashMap<>();

        ResultSet rs = connection.getMetaData().getIndexInfo(catalog, schemaPattern, table, true, false);

        while (rs.next()) {
            iName = normalizeIndexName(rs.getString("INDEX_NAME"));

            if (iName != null) {
                column = new Column(rs.getString("COLUMN_NAME"));
                column.setPosition(rs.getInt("ORDINAL_POSITION"));
                ArrayList<Column> cols = indexes.get(iName);

                if (cols == null) {
                    cols = new ArrayList<>();
                    indexes.put(iName, cols);
                }
                cols.add(column);            }
        }
        return indexes;
    }    /*
     * The JDBC driver allows table meta data using matches on catalog, schema and name. However, it appears
     * that different database use catalog and schema to hold the database name in different ways.
     * Some set catalog to database and others set schema to database. This contains both allowing the
     * meta data interfaces to work in either case.
     */
    public class TableIdentifier {
        private String catalog = null;
        private String schema  = null;
        private String name    = null;
        
        protected TableIdentifier(String catalog, String schema, String name) {
            this.catalog = catalog;
            this.schema  = schema;
            this.name    = name;        
        }
        public String getCatalog() {
            return catalog;
        }
        public String getSchema() {
            return schema;
        }
        public String getName() {
            return name;
        }
    }
    /*
     * The jdbc metadata interface makes use of the term catalog and schema. It appears that these have different meanings
     * for any particular database. MySQL does not use schema and catalog is the database name containing the table. For
     * SQLServer catalog is also set to the database name and schema is set to dbo, presumably you could define other
     * schema values.
     *
     * This uses connection.getCatalog to determine which of getTables catalog and schema fields to use. If it is not null
     * catalog is set to database and schema is set to null, otherwise, schema is set to database and catalig to null.
     *
     * This works for MySQL and SQLServer, but may not work for other database servers.
     *
     * database   The database to be searched, if null, all databases are searched.
     * tableMatch A pattern to be used to select tables. It uses the same pattern definition as the the like clause,
     *            e.g. %abc% will retrieve all tables that contain abc in the name ignoring case.
     */
    private void addTableIds(ArrayList<TableIdentifier> tables, String database, String tableMatch) throws SQLException {
        boolean useCatalog = connection.getCatalog() != null;
        
        ResultSet rs = connection.getMetaData().getTables(useCatalog? database : null, useCatalog? null : database, tableMatch, null);
            
        while (rs.next()) {
            String catalog = rs.getString("TABLE_CAT");
            String schema  = rs.getString("TABLE_SCHEM");
            String name    = rs.getString("TABLE_NAME");
            String type    = rs.getString("TABLE_TYPE");
            
            if (!type.equalsIgnoreCase("table")) continue;
            
            tables.add(new TableIdentifier(catalog, schema, name));
        }
    }
    /*
     * Returns an array of TableIdentifiers matching database and nameMatch. 
     *
     * database  The database to be searched or null, if all databases are searched.
     * nameMatch Table name matching pettern. Uses the same pattern specifiers as used by like.
     *
     * Only objects of type TABLE are included, i.e. system tables and views are ignored.
     *
     * Returns an array of tableIdentifiers, that can be passed to the appropriate TableDefinition constructor.     *         
     */
    public ArrayList<TableIdentifier> getTableIdentifiers(String database, String nameMatch) throws SQLException {
        ArrayList<TableIdentifier> tables = new ArrayList<>();
        
        addTableIds(tables, database, nameMatch);
        
        return tables;
    }
    /*
     * Same as above except it only returns TableIdentifiers, that are accessible from the DatabaseSession instance.
     */
    public ArrayList<TableIdentifier> getTableIdentifiers(String nameMatch) throws SQLException {  
        return getTableIdentifiers(database, nameMatch);
    }
    /*
     * Returns the TableIdentifier for table name in the connected database.
     */
    public TableIdentifier getTableIdentifier(String name) throws SQLException {
        ArrayList<TableIdentifier> tables = getTableIdentifiers(database, name);
        
        if (tables.isEmpty()) throw new SQLException("Table " + name + " does not exist in database " + database);
        
        if (tables.size() != 1) throw new SQLException("Table " + name + " not unique in database " + database);
        
        return tables.get(0);
    }
    /*
     * Provides information about a table derived from the JDBC metadata methods getColumns and getIndexInfo.
     */
    public class TableDefinition implements Iterable<Column> {
        private String          name;
        private String          schema;
        private String          catalog;
        private String          displayName;
        private String          parent;
        private int             maxDisplayLabel;
        
        private ArrayList<Column>                  columns;
        private HashMap<String, Integer>           index;
        private HashMap<String, ArrayList<Column>> indexes;
        
        private void updateMaxDisplayLabel(Column col) {
            String lab = col.getDisplayName();
            
            if (lab != null && lab.length() > maxDisplayLabel) maxDisplayLabel = lab.length();
        }
        private void setMaxDisplayLabel() {
           maxDisplayLabel = 0;
           columns.forEach(col -> updateMaxDisplayLabel(col));
        }
        private class ObjectIterator implements Iterator<Column> {
            int count = 0;

            @Override
            public boolean hasNext() {
                return count < columns.size();
            }
            @Override
            public Column next() {
                if (hasNext()) {
                    return columns.get(count++);
                } else {
                    /*
                     * This can only happen if caller has not obeyed hasNext or values
                     * have been removed from array while iterating. 
                     * 
                     * For now return null until suitable exception has been identified.
                     */
                    return null;
                }
            }
            @Override
            public void remove() {
                columns.remove(--count);
            }
        }
        @Override
        public Iterator<Column> iterator() {
            return new TableDefinition.ObjectIterator();
        }        
        public TableDefinition(TableIdentifier table) throws SQLException {
            this.name = table.getName();
            
            columns   = new ArrayList<>();
            index     = new HashMap<>();
            indexes   = getTableIndexes(table.getCatalog(), table.getSchema(), this.name);
                        
            ResultSet rs = connection.getMetaData().getColumns(table.getCatalog(), table.getSchema(), this.name, "%");

            while (rs.next()) {
                Column column = new Column(rs.getString("COLUMN_NAME"), this);
                
                column.setPosition(rs.getInt("ORDINAL_POSITION"));
                column.setType(rs.getInt("DATA_TYPE"));
                column.setTypeName(rs.getString("TYPE_NAME"));
                column.setColumnSize(rs.getInt("COLUMN_SIZE"));
                column.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                column.setAuto(toBoolean(rs, "IS_AUTOINCREMENT", false));
                column.setGenerated(toBoolean(rs, "IS_GENERATEDCOLUMN", false));
                column.setNullable(toBoolean(rs, "IS_NULLABLE", true));
                columns.add(column);
                index.put(column.getName().toLowerCase(), columns.size() - 1);
            }
            ArrayList<Column> pkCols = indexes.get("PRIMARY");
            
            if (pkCols != null) {
                /*
                 * Update table columns that form part of the primary key.
                 */
                for (Column col : pkCols) {
                    getColumn(col.getName()).setpKeyPosition(col.getPosition());
                }
            }
            setMaxDisplayLabel();
        }
        private int toIndex(String columnName) throws SQLException {
            columnName = columnName.toLowerCase();
            
            if (!this.index.containsKey(columnName)) throw new SQLException("Column " + columnName + " is not in table " + name);
            
            return this.index.get(columnName.toLowerCase());
        }
        public TableDefinition(String name) throws SQLException {
            this(getTableIdentifier(name));
            this.displayName = Utils.splitToWords(name);
        }
        public Column getColumn(int index) {
            return columns.get(index);
        }
        public ArrayList<Column> getColumns() {
            return columns;
        }
        public final Column getColumn(String name) throws SQLException {
            return getColumn(toIndex(name));
        }        
        public ArrayList<Column> getKeyColumns() {           
            Iterator<Column>  it = this.iterator();
            Column            col;
            ArrayList<Column> cols = new ArrayList<>();
            
            while (it.hasNext()) {
                col = it.next();
                
                if (col.getpKeyPosition() > 0) cols.add(col);
            }            
            return cols;
        }
        public Column getKeyColumn(int pKeyPosition) throws SQLException {           
            Iterator<Column> it = this.iterator();
            Column           col;
            
            while (it.hasNext()) {
                col = it.next();
                
                if (col.getpKeyPosition() == pKeyPosition) return col;
            }            
            throw new SQLException("Table " + name + " not have primary key column at position " + pKeyPosition);       
        }
        public String getName() {
            return name;
        }
        public HashMap<String, ArrayList<Column>> getIndexes() {
            return indexes;
        }
        public boolean isColumn(String name) {
            return index.get(name.toLowerCase()) != null;
        }
        public String getSchema() {
            return schema;
        }
        public String getCatalog() {
            return catalog;
        }
        public int getMaxDisplayLabel() {
            return maxDisplayLabel;
        }
        public String getDisplayName() {
            return displayName == null? Utils.splitToWords(name) : displayName;
        }
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        public void setParent(String tableName) throws SQLException {
            TableDefinition  owner = new TableDefinition(tableName);
                     
            Iterator<Column> it    = owner.iterator();
            Column           colo;
            Column           colm;
            
            parent = tableName;
            /*
             * Link each primary key column to that of the corresponding parent column.
             */
            
            while (it.hasNext()) {
                colo = it.next();
                
                if (!colo.isPrimeKeyColumn()) continue;
                
                colm = this.getKeyColumn(colo.getpKeyPosition());
                colm.setSource(tableName, colo.getName());
            }
        }
        public String getParent() {
            return parent;
        }
        public JSONObject toJson(boolean displayOnly) throws JSONException {
            Iterator<Column> it     = this.iterator();
            JSONObject       json   = new JSONObject();
            JSONObject       header = new JSONObject();
            JSONArray        cols   = new JSONArray();
            Column           col;
            
            header.add("Name",           getName());
            header.add("DisplayName",    getDisplayName());
            header.add("MaxColumnLabel", getMaxDisplayLabel());
            header.add("Parent",         getParent());
            
            json.add("Header",  header);
            json.add("Columns", cols);
            
            while (it.hasNext()) {
                col = it.next();
                
                if (displayOnly && !col.getDisplay()) continue;
                
                JSONObject colAttrs = new JSONObject();
                colAttrs.add("Name",          col.getName());
                colAttrs.add("Source",        col.getSource());
                colAttrs.add("Label",         col.getDisplayName());
                colAttrs.add("Type",          col.getTypeName());
                colAttrs.add("Position",      col.getPosition());
                colAttrs.add("PKeyPosition",  col.getpKeyPosition());
                colAttrs.add("Size",          col.getColumnSize());
                colAttrs.add("DecimalDigits", col.getDecimalDigits());
                colAttrs.add("PKeyColumn",    col.isPrimeKeyColumn());
                colAttrs.add("Modifiable",    col.isModifiable());
                colAttrs.add("Nullable",      col.isNullable());
                cols.add(colAttrs);
            }
            return json;
        }
        public JSONObject toJson() throws JSONException {
            return toJson(false);
        }
    }
    private Connection   connection          = null;
    private String       server              = null;
    private String       database            = null;
    private String       user                = null;
    private String       password            = null;
    private String       version             = null;
    private boolean      reportException     = true;
    private boolean      querying            = false;
    private Logger       log                 = new Logger();
    private boolean      autoTransaction     = false;
    private boolean      transactionStarted  = false;
    private String       protocol            = null;
    private int          updateResultSetType = ResultSet.TYPE_FORWARD_ONLY;
    private int          statementTimeout    = 0;
    private int          defaultIsolation    = 0;
    private char         propertyDelim       = ';';
    private char         nextDelim           = ';';
    private double       longStatementTime   = 0;
    private StringBuffer connectString;
    /*
     * The following was introduced as part of a work around for sql server. After an upgrade all connections via java
     * code had the status Dedicated Admin Connection (DAC), which meant only one connection was allowed. I've no idea why
     * this was the case, particulary as creating a DAC is not easy to do. The following is called for every method that
     * requires a connection.
     */
    private void openConnection() throws SQLException {
        if (connection != null && connection.isClosed()) connect();
    }
    private static String formatDate(Date dateTime, String format) {
        return new SimpleDateFormat(format).format(dateTime);
    }
    /*
     * The following convert a Date into value that can be passed as string that can be converted
     * to date using jdbc.
     */
    public static String getDateTimeString(Date dateTime, String protocol) {
        String format;

        switch (protocol) {
            case "sqlserver":
                format = "dd-MM-yyyy HH:mm:ss";
                break;
            default:
                format = "yyyy-MM-dd HH:mm:ss";

        }
        return formatDate(dateTime, format);
    }
    public static String getDateString(Date dateTime, String protocol) {
        String format;

        switch (protocol) {
            case "sqlserver":
                format = "dd-MM-yyyy";
                break;
            default:
                format = "yyyy-MM-dd";

        }
        return formatDate(dateTime, format);
    }

    public static String getTimeString(Date dateTime) {
        return formatDate(dateTime, "HH:mm:ss");
    }

    public static String delimitName(String name, String protocol) {
        String flds[] = name.split("\\.");
        String prefix = null;

        if (flds.length == 2) {
            /*
             * name is of the form A.B. Only need delimit the B part.
             */
            prefix = flds[0];
            name   = flds[1];
        }
        if (protocol.equalsIgnoreCase("sqlserver")) {
            if (name.equalsIgnoreCase("transaction")
                    || name.equalsIgnoreCase("database")
                    || name.equalsIgnoreCase("index")
                    || name.equalsIgnoreCase("sent")
                    || name.equalsIgnoreCase("end")
                    || name.equalsIgnoreCase("percent")) {
                name = '[' + name + ']';
            }
        } else if (protocol.equalsIgnoreCase("mysql")) {
            if (name.equalsIgnoreCase("usage")) {
                name = '`' + name + '`';
            }
        }
        return prefix != null ? prefix + '.' + name : name;
    }

    public String delimitName(String name) {
        return delimitName(name, protocol);
    }

    public void addConnectionProperty(String name, String value) {
        connectString.append(propertyDelim);
        connectString.append(name);
        connectString.append('=');
        connectString.append(value);
        propertyDelim = nextDelim;
    }

    public String getConnectionString() {
        return connectString.toString();
    }

    /*
     * MaxTime is the maximum number of seconds a statement can take before it is reported to the comment log. The report contains the timestamp of
     * completion, the SQL statement and the duration of the statement.
     *
     * If maxTime <= 0, no report is generated.
     */
    public void SetLongStatementTime(double maxTime) {
        longStatementTime = maxTime;
    }
    public String getDateString(Date dateTime) {
        return getDateString(dateTime, protocol);
    }
    public String getDateTimeString(Date dateTime) {
        return getDateTimeString(dateTime, protocol);
    }
    private void loadDriver(String driver, String defaultDriver) throws ClassNotFoundException {
        Class.forName(driver == null ? defaultDriver : driver);
    }

    private void startConnectionString(String protocol, String server, String database, String driver) {
        this.protocol = protocol;
        this.server = server;
        this.database = database;

        try {
            /*
             * loadDriver seems to be required for code run by tomcat, but not for code executed directly.
             * The disadvantage of using loadDriver seems to be that if the class name changes, the new version is
             * not picked up. Don't fully understand this, seems to be something to do with SPI and API.
             */
            switch (protocol) {
                case "sqlserver":
                    loadDriver(driver, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
                    /*
                     * Need to handle the port to allow access to a server that does not use the default port.
                     */
                    connectString = new StringBuffer("jdbc:sqlserver://" + server + ":1433");
                    addConnectionProperty("databaseName",           database);
                    addConnectionProperty("trustServerCertificate", "true");
                    break;
                case "jdbc:odbc":
                    loadDriver(driver, "sun.jdbc.odbc.JdbcOdbcDriver");
                    updateResultSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;
                    connectString = new StringBuffer("jdbc:odbc:Driver=" + server + ";" + database);
                    break;
                case "mysql":
                    loadDriver(driver, "com.mysql.cj.jdbc.Driver");
                    connectString = new StringBuffer("jdbc:" + protocol + "://" + server + "/" + database);
                    propertyDelim = '?';
                    nextDelim = '&';
                    break;
                case "postgresql":
                    loadDriver(driver, "org.postgresql.Driver");
                    connectString = new StringBuffer("jdbc:" + protocol + "://" + server + "/" + database);
                    break;
                default:
                    log.fatalError("JDBC protocol " + protocol + " not supported");
                    break;
            }
        } catch (ClassNotFoundException ex) {
            log.comment("Probably missing the jdbc jar file on the classpath for protocol " + protocol);
            log.fatalError(ex);
        }
    }
    private void autoStartTransaction(boolean write) throws SQLException {
        if (!write || connection.getAutoCommit() || transactionStarted) return;
        /*
         * It seems that JDBC autmatically starts a transaction autoCommit is false. 
         *
         * For now use this in case above is not correct.
         */
        transactionStarted = true;
    }
    public DatabaseSession() {

    }
    public DatabaseSession(String protocol, String server, String database, boolean autoTransaction) throws SQLException {
        startConnectionString(protocol, server, database, null);
        this.autoTransaction = autoTransaction;
    }
    public DatabaseSession(String protocol, String server, String database) throws SQLException {
        this(protocol, server, database, false);
        startConnectionString(protocol, server, database, null);
    }

    public void setUser(String user, String password) {
        if (user != null && user.trim().length() != 0) {
            addConnectionProperty("user", user);
        } else if (protocol.equals("sqlserver")) {
            addConnectionProperty("integratedSecurity", "true");
        }
        if (user != null && user.trim().length() != 0) {
            addConnectionProperty("password", password);
        }
    }

    public void connect() throws SQLException {
        connection       = DriverManager.getConnection(connectString.toString());
        defaultIsolation = connection.getTransactionIsolation();
        version          = connection.getMetaData().getDriverVersion();
        
        this.connection.setAutoCommit(!autoTransaction);
        
        /*
         * For PostgreSQL change string handling to standard.
         */
        if (protocol.equals("postgresql")) {
            executeUpdate("set standard_conforming_strings=on");
        }
    }

    public void connect(String protocol, String server, String database, String user, String password) throws SQLException {
        startConnectionString(protocol, server, database, null);
        setUser(user, password);
        connect();
    }

    public void logException(Exception exception) {
        if (reportException) {
            log.fatalError(exception);
        }
    }

    public boolean requiresStrongType() {
        return !protocol.equalsIgnoreCase("sqlserver");
    }

    public void open(Properties properties) throws SQLException {
        Enumeration<?> e = properties.propertyNames();
        String protName = "sqlserver";
        String dbServer = null;
        String dbName = null;
        String dbUser = null;
        String dbPassword = null;

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();

            if (key.equalsIgnoreCase("Protocol")) {
                protName = properties.getProperty(key);
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
        connect(protName, dbServer, dbName, dbUser, dbPassword);
    }

    public void startTransaction() throws SQLException {
        openConnection();
    }

    public void startTransaction(int isolationLevel) throws SQLException {
        openConnection();
        connection.setTransactionIsolation(isolationLevel);
        startTransaction();
    }

    public void commit() throws SQLException {
        openConnection();
        
        if (connection.getAutoCommit()) return;
        
        connection.commit();
        connection.setTransactionIsolation(defaultIsolation);
    }

    public void rollback() throws SQLException {
        openConnection();
        
        if (connection.getAutoCommit()) return;

        connection.rollback();
        connection.setTransactionIsolation(defaultIsolation);
    }

    public void setStatementTimeout(int seconds) {
        statementTimeout = seconds;
    }

    public void open(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        try {
            openConnection();
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(DatabaseSession.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
        }
    }

    public boolean isOpen() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    private class StatementWrapper {

        private Timer t = new Timer();
        private String sql;
        public Statement statement;

        StatementWrapper(String sql, Statement statement) throws SQLException {
            openConnection();
            t.setAutoReset(false);

            if (statement == null) {
                statement = connection.createStatement();
            }

            this.sql = sql;
            this.statement = statement;

            statement.setQueryTimeout(statementTimeout);
            querying = true;
        }

        StatementWrapper(String sql) throws SQLException {
            this(sql, null);
        }

        private void reportLongStatement() {
            if (t.getElapsed() > longStatementTime && longStatementTime > 0) {
                Report.comment(null, t.addElapsed("SQL Statement \"" + sql + "\""));
            }
        }

        void close(SQLException ex) throws SQLException {
            if (reportException) {
                synchronized (this) {
                    querying = false;
                }
                reportLongStatement();
                ex.getMessage();
                throw ex;
            }
        }

        void close() {
            synchronized (this) {
                reportLongStatement();
                querying = false;
                reportException = true;
            }
        }
    }

    private ResultSet executeUpdate(String sql, boolean getGeneratedKey) throws SQLException {
        ResultSet rs = null;
        StatementWrapper wr = new StatementWrapper(sql);
        autoStartTransaction(true);

        try {
            wr.statement.executeUpdate(sql, getGeneratedKey ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);

            if (getGeneratedKey) {
                rs = wr.statement.getGeneratedKeys();
            }
        } catch (SQLException ex) {
            wr.close(ex);
        }
        wr.close();

        return rs;
    }
    public int executeUpdate(String sql) throws SQLException {
        StatementWrapper wr = new StatementWrapper(sql);
        autoStartTransaction(true);
        return wr.statement.executeUpdate(sql, Statement.NO_GENERATED_KEYS);
    }

    public ResultSet executeUpdateGetKey(String sql) throws SQLException {
        ResultSet rs = null;
        StatementWrapper wr = new StatementWrapper(sql);

        autoStartTransaction(true);
        
        try {
            wr.statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);

            rs = wr.statement.getGeneratedKeys();
            
        } catch (SQLException ex) {
            wr.close(ex);
        }
        wr.close();

        return rs;
//        return executeUpdate(sql, true);
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        openConnection();
        
        if (connection == null) {
            return null;
        }

        ResultSet results = null;
        StatementWrapper wr = new StatementWrapper(sql);

        autoStartTransaction(true);
        
        try {
            results = wr.statement.executeQuery(sql);
        } catch (SQLException ex) {
            wr.close(ex);
        }
        wr.close();

        return results;
    }

    public ResultSet executeQuery(String sql, int setType) throws SQLException {
        openConnection();        
        autoStartTransaction(false);
        
        if (connection == null) {
            return null;
        }

        ResultSet results = null;
        StatementWrapper wr = null;

        try {
            wr = new StatementWrapper(sql, connection.createStatement(setType, ResultSet.CONCUR_READ_ONLY));
            results = wr.statement.executeQuery(sql);
            wr.close();
        } catch (SQLException ex) {
            if (wr != null) {
                wr.close(ex);
            }
        }
        return results;
    }

    public ResultSet updateQuery(String sql, int setType) throws SQLException {
        openConnection();
        
        if (connection == null) {
            return null;
        }

        ResultSet results = null;
        StatementWrapper wr = null;

        try {
            wr = new StatementWrapper(sql, connection.createStatement(setType, ResultSet.CONCUR_UPDATABLE));
            results = wr.statement.executeQuery(sql);
            wr.close();
        } catch (SQLException ex) {
            if (wr != null) {
                wr.close(ex);
            }
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
        return updateQuery("SELECT * FROM " + delimitName(table, protocol) + " WHERE 1=0", updateResultSetType);
    }

    public void killQuery() {
        synchronized (this) {
            if (querying) {
                try {
                    openConnection();
                    reportException = false;
                    connection.close();
                    connect(this.getProtocol(), this.getServer(), this.getDatabase(), this.user, this.password);
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
            headings = new ArrayList<>();
            data.add(headings);
        }
        while ((i++ < maxRows || maxRows == 0) && records.next()) {
            ArrayList<String> row = new ArrayList<>();

            for (int col = 0; col < records.getMetaData().getColumnCount(); col++) {
                if (i == 1 && headings != null) {
                    headings.add(records.getMetaData().getColumnName(col + 1));
                }

                row.add(records.getString(col + 1));
            }
            data.add(row);
        }
    }

    public ArrayList<String> getColumn(String sql, String column) {
        ArrayList<String> data = new ArrayList<>();

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
        openConnection();
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

            if (precision >= 0) {
                typeName += "," + precision;
            }

            typeName += ')';
        }

        return delimitName(name) + ' ' + typeName;
    }

    public StringBuffer initialiseCreateTable(String table) {
        return new StringBuffer("CREATE Table " + table + '(');
    }

    public void addColumn(StringBuffer createDDL, String column, int type, int size, int precision) throws SQLException {
        if (createDDL.charAt(createDDL.length() - 1) != '(') {
            createDDL.append(",\n");
        }

        createDDL.append(getColumnDefinition(column, type, size, precision));
    }

    public void createTable(StringBuffer createDDL) throws SQLException {
        createDDL.append(')');
        openConnection();
        autoStartTransaction(true);
        connection.createStatement().execute(createDDL.toString());
    }

    public boolean columnExists(String table, String column) throws SQLException {
        openConnection();
        ResultSet rs = connection.getMetaData().getColumns(null, null, table, column);
        return rs.next();
    }
    /*
     * To be removed.
     */
    @Deprecated
    public HashMap<String, Column> getColumns(String table) throws SQLException {
        HashMap<String, Column> columns;
        try ( ResultSet rs = connection.getMetaData().getColumns(null, null, table, "%")) {
            columns = new HashMap<>();
            while (rs.next()) {
                Column column = new Column(rs.getString("COLUMN_NAME"));

                column.setPosition(rs.getInt("ORDINAL_POSITION"));
                column.setType(rs.getInt("DATA_TYPE"));
                column.setTypeName(rs.getString("TYPE_NAME"));
                columns.put(column.getName(), column);
            }
        }
        return columns;
    }
    public String getVersion() {
        return version;
    }

    private double getSize(String size) {
        String fields[] = size.split(" ");
        double value;

        value = Double.parseDouble(fields[0]);

        if (fields.length > 1) {
            if (fields[1].equalsIgnoreCase("kb")) {
                value *= 1000;
            } else if (fields[1].equalsIgnoreCase("mb")) {
                value *= 1000000;
            } else if (fields[1].equalsIgnoreCase("gb")) {
                value *= 1000000000;
            }
        }
        return value;
    }

    public DatabaseStatistics getDatabaseStatistics(boolean updateUsage) throws SQLException {
        DatabaseStatistics statistics = new DatabaseStatistics();

        ResultSet db = executeQuery("EXEC sp_spaceused @updateusage = " + (updateUsage ? "true" : "false"));

        autoStartTransaction(false);
        
        if (db.next()) {
            statistics.name = db.getString("database_name");
            statistics.size = getSize(db.getString("database_size"));
            statistics.unallocated = getSize(db.getString("unallocated space"));
        }
        return statistics;
    }

    public Error getStandardError(SQLException exception) {
        String message = exception.getMessage();
        String state = exception.getSQLState();

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
            if (state.equals("23505")) {
                return Error.Duplicate;
            }
            if (state.equals("40P01")) {
                return Error.Deadlock;
            }
        } else if (getProtocol().equalsIgnoreCase("mysql")) {
            switch (exception.getErrorCode()) {
                case 1062:
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
