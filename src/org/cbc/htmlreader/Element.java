/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.htmlreader;

import java.util.HashMap;

/**
 *
 * @author CClose
 */
public class Element {

    public enum Type {
        Start,
        Text,
        End
    };
    private HashMap<String, String> attributes = new HashMap<String, String>();
    private Type   type = Type.Start;
    private String text = "";
    private boolean open = false;
    
    public void setType(Type type) {
        this.type = type;

        if (type == Type.Start) open = true;
        if (type == Type.End) open = false;
    }
    public Type getType() {
        return type;
    }
    public void addAttribute(String name, String value) {
        this.attributes.put(name.trim(), value.trim());
    }
    public HashMap<String, String> getAttributes() {
        return attributes;
    }
    public void setText(String text) {
        if (text.startsWith("</"))
            this.text = text.substring(2);
        else if (text.startsWith("<"))
            this.text = text.substring(1);
        else
            this.text = text;

        if (this.text.endsWith(">"))
            this.text = this.text.substring(0, this.text.length() - 1);
    }
    public String getText() {
        return text;
    }
    public boolean isOpen() {
        return open;
    }
    public void setOpen(boolean open) {
        this.open = open;
    }
    public String toString() {
        return type.toString() + ' ' + text + " atributes " + attributes.size() + " open " + open;
    }
    public boolean matchesStart(String name) {
        return type == Type.Start && text.equalsIgnoreCase(name);
    }
    public boolean matchesEnd(String name) {
        return type == Type.End && text.equalsIgnoreCase(name);
    }
}
