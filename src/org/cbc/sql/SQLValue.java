/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.sql;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.cbc.utils.data.DatabaseSession;

/**
 *
 * @author chris
 */
public class SQLValue {

    private SQLBuilder.ValueType type;

    private String  txtValue;
    private double  dblValue;
    private int     intValue;
    private Date    datValue;
    private boolean isQuoted;
    private boolean isFieldName;
    private String  protocol = "sqlserver";

    private SimpleDateFormat fmtTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public SQLValue(String value) {
        txtValue  = value;
        this.type = SQLBuilder.ValueType.Text;
        isQuoted  = true;
    }

    public SQLValue(int value) {
        intValue = value;
        type = SQLBuilder.ValueType.Integer;
    }

    public SQLValue(double value) {
        dblValue = value;
        type = SQLBuilder.ValueType.Double;
    }

    public SQLValue(Date value) {
        datValue = value;
        type = SQLBuilder.ValueType.Date;
        isQuoted = true;
    }

    public void setIsQuoted(boolean yes) {
        isQuoted = yes;
    }
    public boolean getIsQuoted() {
        return isQuoted;
    }
    public void setIsFieldName(boolean yes) {
        isFieldName = yes;
    }
    public boolean getIsFieldName() {
        return isFieldName;
    }
    public String getValue(String protocol) {
        String text = "";

        switch (type) {
            case Text:
                text = !isFieldName || txtValue == null ? txtValue : DatabaseSession.delimitName(txtValue, protocol);
                break;
            case Double:
                text = "" + dblValue;
                break;
            case Integer:
                text = "" + intValue;
                break;
            case Date:
                text = fmtTimestamp.format(datValue);
                break;
        }
        return text == null ? null : isQuoted ? '\'' + DatabaseSession.escape(text) + '\'' : text;
        
    }
    public String getValue() {
        return getValue(protocol);
    }
}
