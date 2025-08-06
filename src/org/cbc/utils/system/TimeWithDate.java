/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cbc.utils.system;

import java.time.Instant;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.util.Date;

/**
 *
 * Facilitates working with both java.time and java.util.Date; 
 */
public class TimeWithDate {
    private ZoneId     zoneId;
    private Instant    instant;
    private OffsetTime offsetTime;

    /*
     * timestamp is time representing a local time.
     * 
     * Set the time instant from timestamp. The conversion corrects the instant if it is determined
     * from the timestamp date that it is offset from GMT.
     *
     * E.g.  If timestamp is 01-Jun-25 08:30 and the zoneId is for Europe/London the instant will
     *       01-Jun-25 07:30 and the offset in seconds will be 3600.
     *      
     */
    public final void setInstantFromLocal(Date timestamp) {
        instant    = Instant.ofEpochMilli(timestamp.getTime());
        offsetTime = OffsetTime.ofInstant(instant, zoneId);

        if (offsetTime.getOffset().getTotalSeconds() != 0) {
            /*
                 * Correct instant so that it is true GMT time rather timestamp assumed to be GMT
             */
            instant = instant.minusSeconds(offsetTime.getOffset().getTotalSeconds());
            offsetTime = OffsetTime.ofInstant(instant, zoneId);
        }
    }
    /*
     * timestamp is a timestring representing a GMT time.
     */
    public final void setInstantFromGMT(Date timestamp) {
        instant    = Instant.ofEpochMilli(timestamp.getTime());
        offsetTime = OffsetTime.ofInstant(instant, zoneId);
    }
    /*
     * timezone is the zone identifier such as Europe/London.
     */
    public TimeWithDate(String timeZone) {
        zoneId = ZoneId.of(timeZone);
        setInstantFromLocal(new Date());
    }
    public OffsetTime getOffsetTime() {
        return offsetTime;
    }
    public int getGMTOffset(Date timestamp) {
        setInstantFromLocal(timestamp);
        return offsetTime.getOffset().getTotalSeconds();
    }
    public int getGMTOffset() {
        return offsetTime.getOffset().getTotalSeconds();
    }
    public Date getDate() {
        return new Date(instant.toEpochMilli());
    }
    public Date getLocalDate() {
        return new Date(instant.toEpochMilli() + 1000 * getGMTOffset());
    }
    /*
     * locatTimestamp contains a time which a local time.
     *
     * Sets the instance to the GMT for localTime and returns it.
    */
    public Date toGMT(Date localTimestamp) {
        this.setInstantFromLocal(localTimestamp);

        return getDate();
    }
    /*
     * gmtTimestamp contains a time which a GMT time.
     *
     * Sets the instance to the GMT for gmtTimestamp and returns to local time it represents
    */
    public Date toLocal(Date gmtTimestamp) {
        this.setInstantFromGMT(gmtTimestamp);

        return getLocalDate();
    }
}
