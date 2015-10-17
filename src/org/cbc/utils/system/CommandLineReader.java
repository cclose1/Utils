/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.system;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 * This class provides method for defining and reading command line parameters, i.e the values following main method used to invoke
 * the programme, e.g.
 * <code>java -jar prog.jar par1 par2</code>
 * Parameters are space separated values which are passed to the main in the args parameter.
 * <p>This class recognises mandatory and optional parameters and provides methods for defining the parameters expected by the programme and
 * methods for reading them.
 * <p>Mandatory parameters are defined by their position ignoring the optional parameters. Optional parameters are introduced by an identifier
 * starting with the character - and they can also have a qualifier. If an optional parameter has a qualifier it must be provided. The optional
 * parameters can be provided in any order and
 * @author cclose
 */
public class CommandLineReader {
    private ArrayList<Parameter> parameters    = new ArrayList<Parameter>();
    private Parameter            lastMandatory = null;
    private boolean              commandValid  = true;

    private void commandError(String message) {
        commandValid = false;
        log.error(message);
    }
    /**
     *
     */
    public class CommandLineException extends Exception {
        private static final long serialVersionUID = 1L;
        private Parameter         parameter = null;
        private String            message   = null;

        /**
         *
         * @param parameter
         * @param message
         */
        protected CommandLineException(Parameter parameter, String message) {
            this.parameter = parameter;
            this.message   = message;
        }
        public String getMessage() {
            return "Parameter " + parameter.name + " " + message;
        }
    }
    private class Arguments {
        private BufferedReader reader   = null;
        private String         args[]   = null;
        private int            index    = 0;
        private int            ch;
        private boolean        inQuotes = false;

        protected class Argument {
            protected boolean isOption = false;
            protected String  value    = null;

            protected Argument(String value) {
                this.value = value;
            }
        }        
        private boolean isWhiteSpace() {
            if (ch == '\r' || ch == '\n') {
                inQuotes = false;
                return true;
            }
            return (ch == ' ' || ch == '\t') && !inQuotes;
        }
        private String getField() throws IOException {
            StringBuffer field = null;
            
            while ((ch = reader.read()) != -1) {
                if (isWhiteSpace()) {
                    if (field != null) break;
                } else {
                    
                    if (field == null) field = new StringBuffer();
                    
                    if (inQuotes && ch == '"')
                        inQuotes = false;
                    else if (!inQuotes && ch == '"') 
                        inQuotes = true;
                    else
                        field.append((char)ch);
                }
            }
            return field == null? null : field.toString();
        }
        protected Arguments(String args[]) {
            if (args.length != 0 && args[0].equalsIgnoreCase("-p")) {
                try {
                    ArrayList<String> fields = new ArrayList<String>();
                    String            field  = null;

                    reader = new BufferedReader(new FileReader(args[1]));

                    while ((field = getField()) != null) {
                        fields.add(field);
                    }
                    reader.close();
                    
                    for (int i = 2; i < args.length; i++) fields.add(args[i]);

                    this.args = new String[(fields.size())];
                    fields.toArray(this.args);
                } catch (FileNotFoundException ex) {
                    log.fatalError("Parameter file " + args[1] + " not found");
                } catch (IOException ex) {
                    log.fatalError("Reading parameter file " + args[1] + " error-" + ex.getMessage());
                }
            } else {
                this.args = args;
            }
        }
        protected Argument next() {
            if (index >= args.length) return null;

            Argument arg = new Argument(args[index++]);

            if (arg.value.startsWith("\\-"))
                arg.value = arg.value.substring(1);
            else if (arg.value.startsWith("-")) {
                arg.isOption = true;
                arg.value    = arg.value.substring(1);
            }
            return arg;
        }
    }
    private class Parameter {
        private String  name;
        private String  qualifierDescription = null;
        private String  defaultValue         = null;
        private String  fields[]             = null;
        private boolean optional             = false;
        private boolean caseSensitive        = false;
        private String  separator            = null;
        private boolean set                  = false;
        private int     minIndex             = -1;
        private int     maxIndex             = -1;

        public boolean match(String name) {
            if (name.startsWith("-")) name = name.substring(1);

            if (caseSensitive) return this.name.startsWith(name);

            return this.name.toLowerCase().startsWith(name.toLowerCase());
        }
        public Parameter(String name, boolean caseSensitive, boolean optional, String description) {
            this.name                 = name;
            this.optional             = optional;
            this.qualifierDescription = description == null || description.indexOf(' ') == -1? description : '\'' + description + '\'';
            this.caseSensitive        = caseSensitive;
        }
        public void setValue(String value) {
            set = true;

            if (separator == null) {
                fields = new String[1];
                fields[0] = value;
            } else {
                fields = value.split(separator);

                if (fields.length <  minIndex || (fields.length > maxIndex && maxIndex != -1)) {
                    StringBuilder message = new StringBuilder();

                    message.append("Qualifier ");
                    message.append(name);
                    message.append(" has ");
                    message.append(fields.length);
                    message.append(" fields(s); but requires ");
                    
                    if (maxIndex == -1) {
                        message.append("at least ");
                        message.append(minIndex);
                    } else if (minIndex == maxIndex)
                        message.append(minIndex);
                    else {
                        message.append("between ");
                        message.append(minIndex);
                        message.append(" and ");
                        message.append(maxIndex);
                    }
                    message.append(" fields(s)");
                    commandError(message.toString());
                }
            }
        }
        public void addSeparatorUsage(StringBuffer usage) {
            if (separator != null) {
                if (qualifierDescription == null) {
                    usage.append('[');
                    usage.append(separator);
                    usage.append("...]");
                } else {
                    usage.append('!');
                    usage.append(qualifierDescription);
                }
            }
        }
    }
    /**
     *
     */
    protected Logger log = new Logger();

    private int getParameterIndex(String name, boolean exact) {
        int matches = 0;
        int match   = 0;

        for (int i = 0; i < parameters.size(); i++) {
            if (exact && parameters.get(i).name.equals(name)) return i;
            if (parameters.get(i).match(name)) {
                matches += 1;
                match    = i;
            }
        }
        if (matches == 1) return match;
        if (matches == 0) return -1;

        return -matches;
    }
    private Parameter getParameter(String name) {
        int i = getParameterIndex(name, true);

        if (i < 0) log.fatalError("Parameter " + name + " is not defined");

        return parameters.get(i);
    }
    private Parameter createParameter(String name, boolean caseSensitive, boolean optional, String description) {
        for (Parameter parameter : parameters) {
            if (caseSensitive && parameter.name.equals(name) ||
               !caseSensitive && parameter.name.equalsIgnoreCase(name))
                log.fatalError("Parameter " + name + " equivalent to " + parameter.name);
        }
        Parameter parameter = new Parameter(name, caseSensitive, optional, description);

        parameters.add(parameter);

        return parameter;
    }
    /**
     *
     */
    public CommandLineReader() {
    }
    public void addParameter(String name, String description) {
        lastMandatory = createParameter(name, false, false, description);
    }
    public void addParameter(String name) {
        addParameter(name, null);
    }
    public void addOption(String name, boolean caseSensitive) {
        createParameter(name, caseSensitive, true, null);
    }
    public void addOption(String name) {
        createParameter(name, false, true, null);
    }
    /**
     *
     * @param name
     * @param optional
     * @param caseSensitive
     */
    public void addParameter(String name, boolean optional, boolean caseSensitive) {
        if (optional)
            addOption(name, caseSensitive);
        else
            addParameter(name);
    }
    public void addDatabaseConnection(String name) {
        addParameter(name, "[Protocol=Value,][Driver=Driver Class,]Server=DBServer,Database=Name,[User=LoginName,][Password=password]");
        setFields(name, ',', 2, 6);
    }
    /**
     *
     * @param name
     * @param caseSensitive
     * @param description
     * @param defaultValue
     */
    public void addQualifiedOption(String name, boolean caseSensitive, String description, String defaultValue) {
        createParameter(name, caseSensitive, true, description).defaultValue = defaultValue;
    }
    public void addQualifiedOption(String name, String description, String defaultValue) {
        addQualifiedOption(name, false, description, defaultValue);
    }
    /**
     *
     * @param name
     * @param description
     */
    public void addQualifiedOption(String name, String description) {
        addQualifiedOption(name, false, description, null);
    }
    /**
     *
     * @param name
     * @param separator
     * @param min
     * @param max
     */
    public void setFields(String name, char separator, int min, int max) {
        Parameter parameter = getParameter(name);

        if (parameter.optional && parameter.qualifierDescription == null)
            log.fatalError("Cannot set fields for " + parameter.name + " because it is optional and not qualified");

        parameter.minIndex  = min;
        parameter.maxIndex  = max;
        parameter.separator = "" + separator;
    }
    /**
     *
     * @param name
     * @param separator
     */
    public void setFields(String name, char separator) {
        setFields(name, separator, 1, -1);
    }
    /**
     *
     * @param command
     * @param version
     * @param args
     * @param appendLast
     */
    public void load(String command, String version, String[] args, boolean appendLast) {
        Arguments          arguments = new Arguments(args);
        Arguments.Argument arg;

        commandValid = true;

        if (appendLast) {
            if (lastMandatory == null) log.fatalError("Append last requires at least 1 mandatory parameter");

            if (lastMandatory.separator != null) log.fatalError("Cannot append last if final mandatory parameter has fields");
        }
        StringBuffer usage = new StringBuffer("Usage: " + command);

        for (Parameter p : parameters) {
            if (p.optional) {
                usage.append(" [-");
                usage.append(p.name);

                if (p.qualifierDescription != null) {
                    usage.append(' ');
                    usage.append(p.qualifierDescription);
                    p.addSeparatorUsage(usage);
                }
                if (p.defaultValue != null) {
                    usage.append('(');
                    usage.append(p.defaultValue);
                    usage.append(')');
                }
                usage.append(']');
            } else {
                usage.append(' ');
                usage.append(p.name);

                if (appendLast && p == lastMandatory)
                    usage.append("[ ...]");
                else
                    p.addSeparatorUsage(usage);
            }
        }
        if (args.length == 0) {
            if (version != null) log.comment("Version " + version);

            log.comment(usage.toString());
            System.exit(0);
        }
        while ((arg = arguments.next()) != null) {
            if (arg.isOption) {
                int i = getParameterIndex(arg.value, false);

                if (i == -1) 
                    commandError("Option -" + arg.value + " is not recognised");
                else if (i < 0) 
                    commandError("Option -" + arg.value + " matches more than 1 option");
                else {
                    Parameter p = parameters.get(i);

                    if (p.qualifierDescription != null) {
                        Arguments.Argument a = arguments.next();

                        if (a == null || a.isOption) {
                            commandError("Option -" + arg.value + " requires a qualifier");

                            // Step back pointer, so we can report more errors. Note: The command will fail.

                            if (a != null) arguments.index--;
                        } else
                            p.setValue(a.value);
                    } else
                        p.set = true;
                }
            } else {
                /*
                 * This is a mandatory parameter so allocate it to the next unset mandatory parameter.
                 */
                boolean   found = false;

                for (Parameter p : parameters) {
                    if (!p.optional && !p.set) {
                        p.setValue(arg.value);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (appendLast && lastMandatory != null) {
                        String copy[] = new String[lastMandatory.fields.length + 1];
                        System.arraycopy(lastMandatory.fields, 0, copy, 0, lastMandatory.fields.length);
                        copy[copy.length - 1] = arg.value;
                        lastMandatory.fields  = copy;
                    } else
                        commandError("Value -" + arg.value + "- is after final mandatory parameter");
               }
            }
        }
        for (Parameter p : parameters) {
            /*
             * If this is an optional qualified parameter that has not been supplied and it has a default, apply it.
             */
            if (p.optional
                    && p.qualifierDescription != null
                    && !p.set
                    && p.defaultValue != null) p.setValue(p.defaultValue);
        }
        if (!commandValid) {
            log.fatalError(usage.toString());
        }
    }
    /* *
     *
     * @param name
     * @return
     */
    public String[] getFields(String name) {
        return getParameter(name).fields;
    }
    /**
     *
     * @param name
     * @return
     */
    public Properties getProperties(String name) {
        Properties properties = null;

        String[] fields = getFields(name);
        
        if (fields != null) {
            properties = new Properties();

            for (String pair : fields) {
                String[] nv = pair.split("=", 2);

                properties.put(nv[0], nv.length == 1? null : nv[1]);
            }
        }
        return properties;        
    }
    /**
     * 
     * @param name
     * @return
     */
    public ArrayList<String> getFieldsArray(String name) {
        String[]          fields = getFields(name);
        ArrayList<String> array  = new ArrayList<String>();

        array.addAll(Arrays.asList(fields));

        return array;
    }
    /**
     *
     * @param name
     * @return
     */
    public int getFieldCount(String name) {
        Parameter p = getParameter(name);

        return p.fields == null? 0 : p.fields.length;
    }
    /**
     *
     * @param name
     * @return
     */
    public boolean isDefined(String name) {
        return getParameterIndex(name, true) != 0;
    }
    /**
     *
     * @param name
     * @return
     */
    public boolean isPresent(String name) {
        return getParameter(name).set;
    }
    /**
     *
     * @param name
     * @param index
     * @param allowNull
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public String getString(String name, int index, boolean allowNull) throws CommandLineException {
        Parameter p = getParameter(name);

        if (p.fields == null) {
            if (allowNull) return null;

            throw new CommandLineException(p, "Not present");
        }
        if (index >= p.fields.length)
            if (allowNull)
                return null;
            else
                log.fatalError("Index " + index + " on parameter " + p.name + " is greater than " + (p.fields.length - 1));

        return p.fields[index];
    }
    /**
     *
     * @param name
     * @param index
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public String getString(String name, int index) throws CommandLineException {
        return getString(name, index, false);
    }
    /**
     *
     * @param name
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public String getString(String name) throws CommandLineException {
        return getString(name, 0);
    }
    /**
     *
     * @param name
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public char getChar(String name, int index) throws CommandLineException {
        String value = getString(name, index);

        if (value.equals("\\t")) return '\t';

        if (value == null || value.length() != 1)
             throw new CommandLineException(getParameter(name), "Must be a single character");
        return value.charAt(0);
    }
    /**
     *
     * @param name
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public char getChar(String name) throws CommandLineException {
        return getChar(name, 0);
    }
    /**
     *
     * @param name
     * @param index
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public int getInt(String name, int index) throws CommandLineException {
        try {
            return Integer.parseInt(getString(name, index));
        } catch (NumberFormatException ex) {
             throw new CommandLineException(getParameter(name), "Can't convert to integer");
        }
    }
    /**
     *
     * @param name
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public int getInt(String name) throws CommandLineException {
        return getInt(name, 0);
    }
    /**
     *
     * @param name
     * @param index
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public double getDouble(String name, int index) throws CommandLineException {
        try {
            return Double.parseDouble(getString(name, index));
        } catch (NumberFormatException ex) {
             throw new CommandLineException(getParameter(name), "Can't convert to double");
        }
    }
    /**
     *
     * @param name
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public double getDouble(String name) throws CommandLineException {
        return getDouble(name, 0);
    }
    public float getFloat(String name, int index) throws CommandLineException {
        try {
            return Float.valueOf(getString(name, index));
        } catch (NumberFormatException ex) {
             throw new CommandLineException(getParameter(name), "Can't convert to double");
        }
    }
    /**
     *
     * @param name
     * @return
     * @throws com.csc.pe.utils.system.CommandLineReader.CommandLineException
     */
    public float getFloat(String name) throws CommandLineException {
        return getFloat(name, 0);
    }
    public boolean getBoolean(String name, int index, boolean allowNull) throws CommandLineException {
        String value = getString(name, index, allowNull);

        return (value != null &&
                (value.equalsIgnoreCase("yes") ||
                 value.equalsIgnoreCase("y")   ||
                 value.equalsIgnoreCase("true")||
                 value.equalsIgnoreCase("t")));
    }
    public boolean getBoolean(String name, boolean allowNull) throws CommandLineException {
        return getBoolean(name, 0, allowNull);
    }
    public boolean getBoolean(String name) throws CommandLineException {
        return getBoolean(name, 0, true);
    }
    public String toString() {
        StringBuilder str = new StringBuilder();

        for (Parameter p : parameters) {
            str.append(p.name + ' ');

            if (p.fields == null)
                str.append("0");
            else {
                str.append("" + p.fields.length + ' ' + p.fields[0]);
            }
            str.append('\n');
        }
        return str.toString();
    }
    public void error(String name, String message) throws CommandLineException {
        throw new CommandLineException(getParameter(name), message);
    }
}
