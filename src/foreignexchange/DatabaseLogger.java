/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package foreignexchange;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import org.cbc.application.reporting.Trace;
import org.cbc.sql.SQLInsertBuilder;
import org.cbc.utils.data.DatabaseSession;

/**
 *
 * @author chris
 */
public class DatabaseLogger implements LogRates {
    private HashSet<String> filter  = null;
    private DatabaseSession session = null;
    
    public DatabaseLogger() {
    }
    public DatabaseLogger(DatabaseSession session) {
        this.session = session;
    }
    public void openDatabase(String protocol, String server, String database, String user, String password) throws SQLException {
        Trace t = new Trace("openDatabase");

        session = new DatabaseSession(protocol, server, database);

        if (user != null) {
            t.report('C', "Database " + user);
            session.setUser(user, password);
            session.connect();
            t.report('C', "Connection string " + session.getConnectionString());
        }
        t.exit();
    }
    public void openDatabase(String protocol, String server) throws SQLException {
        openDatabase(protocol, server, null, null, null);
    }
    @Override
    public void setRatesFilter(HashSet<String> currencies) {
        filter = currencies;
    }
    @Override
    public void record(Date timestamp, CurrencyRates.Provider provider, String from, HashMap<String, Double> rates) throws SQLException {
        Trace t = new Trace("openDatabase");
        
        SQLInsertBuilder builder = new SQLInsertBuilder("CurrencyRate", session.getProtocol());
        
        if (timestamp == null) timestamp = new Date();
        
        for (HashMap.Entry<String, Double> entry : rates.entrySet()) {
            if (filter == null || (filter.contains(entry.getKey()) && !entry.getKey().equals(from))) {                
                builder.addField("Created", timestamp);
                builder.addField("Source",  from);
                builder.addField("Target",  entry.getKey());
                builder.addField("Rate",    entry.getValue());
                
                if (provider != null) builder.addField("provider", provider.toString());
                
                session.executeUpdate(builder.build());
                builder.clear();
            }
        }
        t.exit();
    } 
    @Override
    public void record(Date timestamp, String from, HashMap<String, Double> rates) throws SQLException {
        record(timestamp, null, from, rates);
    } 
}
