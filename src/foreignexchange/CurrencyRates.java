/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package foreignexchange;

import cbc.web.HttpClient;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import org.cbc.application.reporting.Report;
import org.cbc.utils.system.DateFormatter;
import org.cbc.json.JSONException;
import org.cbc.json.JSONNameValue;
import org.cbc.json.JSONObject;
import org.cbc.json.JSONReader;
import org.cbc.json.JSONValue;

/**
 *
 * @author chris
 */
public class CurrencyRates {
    /*
     * Used to force error conditions in testing.
     */
    protected class Test {
        int       status;
        String    content;
        Exception exception;
        
        public Test(int status, String content, Exception exception) {
            this.status    = status;
            this.content   = content;
            this.exception = exception;
        }
    }
    protected Test test   = null;
    
    protected void setTest(int status, String content, Exception exception) {
        test = new Test(status, content, exception);
    }
    public enum Provider {CoinBase, CurrencyConverter};
    
    public interface ErrorHandler {
        public void reset();
        
        public void setExceptionStackTrace(boolean on);
        
        public void setURLReadError(HttpClient client, String reason, int Status, Exception exception);
        
        public void setDataFormatError(String content, Exception exception);
        
        public void setRateLogerError(String source, String target, Exception exception);
        
        public boolean errorsReported();
    }

    private   HttpClient client = new HttpClient();
    private   Provider   provider;
   
    private boolean reverseLookup = false;
    private int     maxAge        = 100;     //Age in seconds after which a reread of rate is made

    private String fmtTime(Date timestamp) {
        DateFormatter fmt = new DateFormatter();
        
        return fmt.format(timestamp, "dd-MMM-yy HH:mm:ss");
    }
    protected class Error implements ErrorHandler {
        int     errors     = 0;
        boolean stackTrace = false;
        
        private void setError(String msg, Exception exception) {
            errors += 1;
            Report.error(null, msg, exception, stackTrace);
        }
        @Override
        public void reset() {
            errors = 0;
        }
        @Override
        public void setExceptionStackTrace(boolean on) {
            stackTrace = on;
        }
        @Override
        public void setURLReadError(HttpClient client, String reason, int status, Exception exception) {
            String msg = "Get URL " + client.getUrl() + " failed-" + reason;
            
            if (status != 200) msg += " HTTP status " + status;
            
            setError(msg, exception);
        }

        @Override
        public void setDataFormatError(String content, Exception exception) {
            setError("Invalid JSON data. Content-" + content, exception);
        }
        @Override
        public void setRateLogerError(String source, String target, Exception exception) {
            setError("Databasse Rate Logger. Source " + source + " target " + target, exception);
        }

        @Override
        public boolean errorsReported() {
            return errors != 0;
        }
    }
    private ErrorHandler error    = new Error();
    private LogRates     logRates = null;
    
    private class Key {
        public String from;
        public String to;
        
        public int hashCode() {
            int hash = 1;
            
            hash = 31 * hash + ((from == null) ? 0 : from.hashCode());
            hash = 31 * hash + ((to   == null) ? 0 : to.hashCode());
            
            return hash;
        }
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Key)) return false;
            if (this == o) return true;
            
            Key k = (Key) o;
            
            return from.equals(k.from) && to.equals(k.to);
        }
        public Key(String from, String to) {
            this.from = from;
            this.to   = to;
        }
        public void swap() {
            String value = from;
            
            from = to;
            to   = value;
        }
    }
    public class Statistics {
        private Date created;
        private Date updated;
        private Date read;
        private int  reads        = 1;
        private int  updates      = 1;
        private int  currentReads = 1;
        /**
         * @return the created
         */
        public Date getCreated() {
            return created;
        }
        /**
         * @return the updated
         */
        public Date getUpdated() {
            return updated;
        }
        /**
         * @return the read
         */
        public Date getRead() {
            return read;
        }
        /**
         * @return the reads
         */
        public int getReads() {
            return reads;
        }
        /**
         * @return the updates
         */
        public int getUpdates() {
            return updates;
        }
        /**
         * @return the currentReads
         */
        public int getCurrentReads() {
            return currentReads;
        }
        
        protected Statistics() {
            created = new Date();
            updated = created;
            read    = created;
        }
        public String toString() {
            return
                    "Created "  + fmtTime(getCreated()) + 
                    " read "    + fmtTime(getRead()) + 
                    " updated " + fmtTime(getUpdated()) +
                    " reads "   + getReads() +
                    " updates " + getUpdates() +
                    " current " + getCurrentReads();
        }
    }
    private class Value {
        double     rate;
        Statistics stats;
        
        protected Value(double rate) {
            this.rate = rate;
            stats     = new Statistics();
        }
        protected void update(double rate) {
            this.rate          = rate;
            stats.updated      = new Date();
            stats.updates     += 1;
            stats.currentReads = 1;
        }
        protected void read() {
            stats.read          = new Date();
            stats.currentReads += 1;
            stats.reads        += 1;
        }
        public String toString() {
            return "rate " + rate + " " + stats.toString();
        }                
    }
    private HashMap<Key, Value> rates = new HashMap<Key, Value>();
    
    public class Rate {
        /**
         * @return the from
         */
        public String getFrom() {
            return from;
        }

        /**
         * @return the to
         */
        public String getTo() {
            return to;
        }

        /**
         * @return the rate
         */
        public double getRate() {
            return rate;
        }

        /**
         * @return the stats
         */
        public Statistics getStats() {
            return stats;
        }
        private String     from;
        private String     to;
        private double     rate;
        private Statistics stats;
                
        protected Rate(Key key, Value value) {
            from  = key.from;
            to    = key.to;
            rate  = value.rate;
            stats = value.stats;
        }
        protected void swap() {
            String value = getFrom();
            
            from = getTo();
            to   = value;
            rate = getRate() == 0? getRate() : 1/getRate();
        }    
        public int hashCode() {
            int hash = 1;
            
            hash = 31 * hash + ((getFrom() == null) ? 0 : getFrom().hashCode());
            hash = 31 * hash + ((getTo()   == null) ? 0 : getTo().hashCode());
            
            return hash;
        }
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Key)) return false;
            if (this == o) return true;
            
            Rate c = (Rate) o;
            
            return getFrom().equals(c.getFrom()) && getTo().equals(getTo());
        }
        public String toString() {
            return "From " + getFrom() + " to " + getTo() + " rate " + getRate() + " stats " + getStats().toString();
        }
    }
    private Rate findRate(Key key) {
        Value value = rates.get(key);
        
        if (value != null) {
            value.read();
            return new Rate(key, value);
        }        
        return null;
    }
    private void loadURL(String scheme, String host, String path) {
        client.setScheme(scheme);
        client.setHost(host);
        client.setPath(path);        
    }
    private JSONValue get(JSONObject json, String key) throws JSONException {
        JSONValue value = json.get(key);
        
        if (value == null) throw new JSONException(key, "No value available");
        
        return value;
    } 
    private HashMap<String, Double> getCurrencyConverter(Key key, String content) throws JSONException {
        HashMap<String, Double> values = new HashMap<String, Double>();
        
        JSONObject json = JSONValue.load(new JSONReader(content), true, true).getObject();
        
        json = get(json, key.from + "_" + key.to).getObject();
        values.put(key.to, get(json, "val").getDouble());
        
        return values;
    }
    private HashMap<String, Double> getCoinBase(Key key, String content) throws JSONException {
        HashMap<String, Double> values = new HashMap<>();
    
        JSONObject json = JSONValue.load(new JSONReader(content), true, true).getObject();
        
        json = get(json, "data").getObject();
        
        if (!get(json, "currency").getString().equals(key.from)) {
            throw new JSONException("currency", " value is " + get(json, "currency").getString() + " expected " + key.from);
        }
        for (JSONNameValue v : get(json, "rates").getObject()) {
            values.put(v.getName(), v.getValue().getDouble());
        }
        return values;
    } 
    private String getContent() {
        int    status  = 0;
        String content = null;
        
        try {
            if (test != null) {
                if (test.exception != null) throw test.exception;
                
                status  = test.status;
                content = test.content;                
            } else {
                status  = client.get();
                content = client.contentString();
                client.close();
            }
            
            if (status != 200) {
                error.setURLReadError(client, "Returned", status, null);
                return null;
            }
        } catch (Exception ex) {
            client.close();
            error.setURLReadError(client, "Threw exception", status, ex);
        }
        return content;
    }
    private HashMap<String, Double> getRateData(Key key, LogRates logger) {        
        HashMap<String, Double> values = null;
        
        client.clearParameters();
        
        switch (provider) {
            case CurrencyConverter:
                client.addParameter("q", key.from + "_" + key.to);
                client.addParameter("compact", "y");
                break;
            case CoinBase:
                client.addParameter("currency", key.from);
                break;
        }
        String content = getContent();
        
        if (content == null) return null;
        
        try {
            switch (provider) {
                case CurrencyConverter:
                    values = getCurrencyConverter(key, content);
                    break;
                case CoinBase:
                    values = getCoinBase(key, content);
                    break;
            }
            if (logger != null) {
                try {
                    logger.record(null, provider, key.from, values);
                } catch (SQLException ex) {
                    error.setRateLogerError(key.from, key.to, ex);
                }
            }
            if (!values.containsKey(key.to)) throw new JSONException("Currency " + key.to + " no rate data");
        } catch (JSONException ex) {
            error.setDataFormatError(content, ex);
        }
        return values;
    }
    private double getRateData(Key key) {        
        HashMap<String, Double> values = getRateData(key, logRates);
        double                  value  = -1;
        
        if (values != null) value = values.get(key.to);
        return value;
    }
    public void loadRateData(String source, String target) {
        logRates.setRatesFilter(null);
        getRateData(new Key(source, target), logRates);
    }
    public void loadRateData(String currencies[]) throws Exception {
        HashSet<String> filter = new HashSet<String>();
        
        if (currencies.length < 2) throw new Exception("loadRateData requires at least 2 currencies");
        
        for (int i = 1; i < currencies.length; i++) filter.add(currencies[i]);
        
        logRates.setRatesFilter(filter);
        getRateData(new Key(currencies[0], currencies[1]), logRates);
    }
    public void setErrorHandler(ErrorHandler handler) {
        error = handler == null? new Error() : handler;
    }
    public ErrorHandler getErrorHandler() {
        return error;
    }
    public void setLogRates(LogRates logger) {
        logRates = logger;
    }  
    public void setProvider(Provider provider, String scheme, String host, String path) {
        this.provider = provider;
        loadURL(scheme, host, path);
    }
    public void setProvider(Provider provider) {
        switch (provider) {
            case CurrencyConverter:
                loadURL("http", "free.currencyconverterapi.com", "/api/v6/convert");
                break;
            case CoinBase:
                loadURL("https", "api.coinbase.com", "/v2/exchange-rates");
                break;
        }        
        this.provider = provider;
    }
    public void setProvider(String provider) {
        setProvider(Provider.valueOf(provider));
    }
    public CurrencyRates(Provider provider, String scheme, String host, String path) {
        setProvider(provider, scheme, host, path);
    }
    public CurrencyRates(Provider provider) {
        setProvider(provider);
    }
    public CurrencyRates() {
        setProvider(Provider.CoinBase);
    }
    public boolean isReverseLookup() {
        return reverseLookup;
    }
    public void setReverseLookup(boolean reverseLookup) {
        this.reverseLookup = reverseLookup;
    }
    public Rate getRate(String from, String to, int maxAge) {
        Date ts   = new Date();
        Key  key  = new Key(from, to);        
        Rate rate = findRate(key);
        
        error.reset();
        
        if (rate == null && reverseLookup) {
            key.swap();
            
            rate = findRate(key);
            key.swap();     //Swap back in case we have to reread if after maxAge
            
            if (rate != null) rate.swap();
        }
        if (rate != null && (ts.getTime() - rate.getStats().getUpdated().getTime()) < 1000 * maxAge) return rate;

        double value = getRateData(key);
        Value data = rates.get(key);

        if (value < 0) {
            return rate;
        }

        if (data == null) {
            data = new Value(value);
            rates.put(key, data);
        } else {
            data.update(value);
        }
        rate = new Rate(key, data);
        
        return rate;
    }
    
    public Rate getRate(String from, String to) {
        return getRate(from, to, maxAge);
    }
}
