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

/**
 *
 * @author chris
 */
public interface LogRates {
    /**
     * 
     * @param currencies List of currency codes to be logged.
     */
    public void setRatesFilter(HashSet<String> currencies);
    /**
     *
     * @param timestamp Data read timestamp. Can be null.
     * @param provider  Source of rates data.
     * @param from      Source currency rate.
     * @param rates     Has map of rates key on the destination currency.
     * 
     * @throws java.sql.SQLException
     */
    public void record(Date timestamp, CurrencyRates.Provider provider, String from, HashMap<String, Double> rates) throws SQLException;
    
    /**
     *
     * @param timestamp Data read timestamp. Can be null.
     * @param from      Source currency rate.
     * @param rates     Has map of rates key on the destination currency.
     */
    public void record(Date timestamp, String from, HashMap<String, Double> rates) throws SQLException;    
}
