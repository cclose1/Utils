/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package foreignexchange;

import java.sql.SQLException;
import org.cbc.application.reporting.Thread;
import org.cbc.application.reporting.Report;
import org.cbc.application.reporting.Process;
import org.cbc.utils.system.CommandLineReader;
import org.cbc.utils.system.LogReport;

/**
 *
 * @author chris
 */
public class LoadRates extends CurrencyRates { 
    public class Reporter extends CurrencyRates.Error implements LogReport {
        private boolean debug = false;

        @Override
        public void comment(String message) {
            Report.comment(null, message);
        }

        @Override
        public void warning(String message) {
            Report.error(null, message);
        }

        @Override
        public void error(String message) {
            errors += 1;
            Report.error(null, message);
        }
        @Override
        public void fatalError(String message) {
            errors +=1;
            Report.error(null, message);
        }

        @Override
        public void fatalError(Exception exception) {
            errors += 1;
            Report.error(null, exception);
        }

        @Override
        public void fatalError(String message, Exception exception) {
            errors += 1;
            Report.error(null, message, exception, stackTrace);
        }

        @Override
        public void setTimePrefix(String format) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setDebug(boolean on) {
            debug = on;
        }

        @Override
        public boolean getBebug() {
            return debug;
        }
        
    }
    private Reporter log = new Reporter();
    
    public Reporter getReporter() {
        return log;
    }
    public LoadRates() {
        setErrorHandler(log);
    }
    
    public void getRates(String provider, String[] currencies) throws Exception {
        loadRateData(currencies);
    }
    public static void main(String[] args) throws InstantiationException {
        String            version      = "V1.0 Released 04-Feb-2019";       
        LoadRates         load         = new LoadRates();
        CommandLineReader cmd          = new CommandLineReader();
        DatabaseLogger    rateLogger   = new DatabaseLogger();
        String            currencies[] = null;
       
        load.getErrorHandler().reset();
        cmd.addParameter("Server");
        cmd.addQualifiedOption("User",         "Name");
        cmd.addQualifiedOption("Password",     "Password");
        cmd.addQualifiedOption("Currencies",   "List of source currencies", "GBP,USD,EUR,BTC-BTC,GBP,USD,EUR");
        cmd.addQualifiedOption("JDBCDriver",   "Driver class",              "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        cmd.addQualifiedOption("JDBCProtocol", "Protocol",                  "sqlserver");
        cmd.addQualifiedOption("ReportRoot",   "Reporting root",            "C:\\Logs");
        cmd.addQualifiedOption("ReportConfig", "Reporting config",          "C:\\Logs\\ARLoadRates.cfg");
        cmd.addQualifiedOption("Provider",     "Internet rate provider",    "CoinBase");
        cmd.addQualifiedOption("StackTrace",   "Exception stack trace",     "Y");
        
        try {
            cmd.setReporter(load.getReporter());
            cmd.load("LoadRates", version, args, false);      
            Process.setConfigFile(cmd.getString("ReportRoot"), cmd.getString("ReportConfig"));
            Thread.attach("LoadRates");
            load.getErrorHandler().setExceptionStackTrace(cmd.getBoolean("StackTrace"));
            Report.comment(null, "Loading rates for " + cmd.getString("currencies"));
            load.setProvider(cmd.getString("provider"));
            rateLogger.openDatabase(cmd.getString("JDBCProtocol"), cmd.getString("Server"), "Expenditure", cmd.getString("User"), cmd.getString("Password"));
            load.setLogRates(rateLogger);
                        
            for (String list : cmd.getString("currencies").split("-")) {
                currencies = list.split(",");
                
                if (currencies.length < 2) cmd.error("currencies", "Must be at least 2 currencies");
                
                load.getRates(cmd.getString("provider"), currencies);
            }
        } catch (SQLException ex) {
            load.getReporter().fatalError("Database error-", ex);
        } catch (CommandLineReader.CommandLineException ex) {
            load.getReporter().fatalError("Command line error-", ex);
        } catch (Exception ex) {
            load.getReporter().fatalError("Command line error-", ex);
        }
        Report.comment(null, "Rate load complete" + (load.getErrorHandler().errorsReported()? "- with errors. See error log" : ""));
        
        if (load.getErrorHandler().errorsReported()) {
            try {
                System.err.println(Report.errorText(null, "Errors reported loading " + cmd.getString("currencies"))); 
            } catch (CommandLineReader.CommandLineException ex) {
            }
        }
    }
}
