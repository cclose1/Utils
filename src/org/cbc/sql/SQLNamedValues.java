/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.sql;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.cbc.utils.data.DatabaseSession;

/**
 *
 * @author chris
 */
public class SQLNamedValues {

    private SimpleDateFormat fmtTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public class NamedValue {
        private SQLBuilder.ValueType type;
        private String               name;
        private String               operator;
        private String               txtValue;
        private double               dblValue;
        private int                  intValue;
        private Date                 datValue;
        private boolean              isQuoted = false;
        
        private NamedValue(String name, String value, String operator) {
            this.name     = name;
            this.operator = operator;
            this.type     = SQLBuilder.ValueType.Text;
            this.txtValue = value;
        }
        protected NamedValue(String name, String value, boolean isQuoted) {
            this(name, value, null);
            this.isQuoted = isQuoted;
        }
        protected NamedValue(String name, String value) {
            this(name, value, true);
        }
        protected NamedValue(String name, int value) {
            this(name, null, null);
            intValue = value;
            type     = SQLBuilder.ValueType.Integer;
        }
        protected NamedValue(String name, double value) {
            this(name, null, null);
            dblValue = value;
            type     = SQLBuilder.ValueType.Double;
        }
        protected NamedValue(String name, Date value) {
            this(name, null, null);
            datValue = value;
            type     = SQLBuilder.ValueType.Date;
            isQuoted = true;
        }
        private void setOperator(String operator) {
            this.operator = operator;
        }
        public String getName() {
            return name;
        }            
        public String getOperator() {
            return operator;
        }
        public String getValue() {
            String text = "";

            switch (type) {
                case Text:
                    text = txtValue;
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
            return text == null? null : isQuoted? '\'' + DatabaseSession.escape(text) + '\'' : text;
        }
    }
    private ArrayList<NamedValue> namedValues = new ArrayList<>(0);
    
    public void add(String name) {
        namedValues.add(new NamedValue(name, null, null));
    }
    public void add(String name, String value) {
        namedValues.add(new NamedValue(name, value));        
    }
    public void add(String name, String value, String operator) {
        NamedValue val = new NamedValue(name, value);
        
        val.setOperator(operator);
        namedValues.add(val);        
    }
    public void add(String name, int value) {
        namedValues.add(new NamedValue(name, value));        
    }
    public void add(String name, int value, String operator) {
        NamedValue val = new NamedValue(name, value);
        
        val.setOperator(operator);
        namedValues.add(val);        
    }
    public void add(String name, double value) {
        namedValues.add(new NamedValue(name, value));        
    }
    public void add(String name, double value, String operator) {
        NamedValue val = new NamedValue(name, value);
        
        val.setOperator(operator);
        namedValues.add(val);        
    }
    public void add(String name, Date value) {
        namedValues.add(new NamedValue(name, value));        
    }
    public void add(String name, Date value, String operator) {
        NamedValue val = new NamedValue(name, value);
        
        val.setOperator(operator);
        namedValues.add(val);        
    }
    public ArrayList<NamedValue> getNamedValues() {
        return namedValues;
    }
}
