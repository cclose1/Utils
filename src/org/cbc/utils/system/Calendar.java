/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.utils.system;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 * @author Chris
 * 
 * Implements an incrementMonth function to add a number of months to a calendar date. Unlike
 * roll this changes the Year field if the increment takes the date outside the current year.
 * 
 * Note: The setMonth and setMonth methods convert month range from 0 to 11 to 1 to 12.
 */
public class Calendar extends GregorianCalendar {
    private static final long serialVersionUID = 1L;
    
    public static int daysBetween(Date d1, Date d2){
        return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
    }
    public Calendar() {  
        super();
    }
    public Calendar(long time) {
        super();
        this.setTimeInMillis(time);
    }
    public Calendar(Date date) {
        this(date.getTime());
    }
    public void setTime(long time) {
        this.setTimeInMillis(time);        
    }
    /**
     * 
     * @param months The number of months to be added to the calendar date. Negative values are allowed.
     * 
     * Adds months to the current calendar date leaving, if possible, the lower fields unchanged. If the day of month is
     * not valid for the new date, it is set to the maximum value allowed for the new date. The following shows examples.
     * 
     *  Date      Months   New Date
     * 
     *  31-Jan-14      1   28-Feb-14
     *  02-Dec-14      3   02-Mar-15
     *  01-Jan-14     -1   01-Dec-13
     * 
     * If the date has a time, it is unchanged.
     * 
     * Note: The result is undefined, in the sense that this case has not been tested for, if the increment 
     *       causes the date to pass the Julian Gregorian calendar switch.
     * 
     */
    public void incrementMonth(int months) {
        int year  = get(GregorianCalendar.YEAR);
        int month = get(GregorianCalendar.MONTH) + months;
        int day   = get(GregorianCalendar.DAY_OF_MONTH);
        
        year += month / 12;
        month = month % 12;
        
        if (month < 0) {
            year--;
            month += 12;
        }
        set(GregorianCalendar.DAY_OF_MONTH, 1);
        set(GregorianCalendar.YEAR, year);
        set(GregorianCalendar.MONTH, month);
        set(GregorianCalendar.DAY_OF_MONTH, day > getActualMaximum(GregorianCalendar.DAY_OF_MONTH)? getActualMaximum(GregorianCalendar.DAY_OF_MONTH) : day);  
    }
    /**
     * 
     * @param days Number of days, can be negative, to be added to the current calendar date.
     * 
     * Adds days to the current calendar date, leaving time unchanged.
     */
    public void incrementDay(long days) {
        setTimeInMillis(getTimeInMillis() + 1000 * 24 * 60 * 60 * days);
        this.computeFields();
    }
    /**
     * 
     * @param year 
     * 
     * Changes current calendar year to year, using set with date field GregorianCalendar.YEAR.
     */
    public void setYear(int year) {
        set(GregorianCalendar.YEAR, year);
    }
    /**
     * 
     * @param month 
     * 
     * Changes current calendar month to month, using set with date field GregorianCalendar.MONTH, with the
     * exception that the month range is 1 to 12 rather than 0 to 11.
     */
    public void setMonth(int month) {
        set(GregorianCalendar.MONTH, month - 1);
    }
    /**
     * 
     * @param day 
     * 
     * Changes current calendar day to day, using set with date field GregorianCalendar.DAY_OF_MONTH.     
     */
    public void setDay(int day) {
        set(GregorianCalendar.DAY_OF_MONTH, day);
    }
    /**
     * @return Year of current calendar date.
     */
    public int getYear() {
        return get(GregorianCalendar.YEAR);
    }
    /**
     * 
     * @return Month of current calendar date. The value is in the range 1 to 12 rather 0 to 11 as would
     * be returned by the get of the GregorianCalendar.
     */
    public int getMonth() {
        return get(GregorianCalendar.MONTH) + 1;
    }
    /**
     * 
     * @return Day of month of current calendar date.
     */
    public int getDay() {
        return get(GregorianCalendar.DAY_OF_MONTH);
    }
}
