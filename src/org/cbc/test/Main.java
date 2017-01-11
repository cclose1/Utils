package org.cbc.test;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cbc.json.JSONException;
import org.cbc.json.JSONFormat;
import org.cbc.json.JSONObject;
import org.cbc.json.JSONReader;
import org.cbc.json.JSONValue;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Chris
 */
public class Main {

    private static void testLoad(JSONReader reader, boolean strictQuotes, boolean ordered) throws JSONException {
        JSONValue  value  = JSONValue.load(reader, strictQuotes, ordered);
        JSONObject object = value.getObject();
        
        object.add("float1",  new JSONValue((float)1.2378, 2));
        object.add("double1", new JSONValue(156.2378, 3));
        System.out.println(value.toString(new JSONFormat(true)));
    }
    public static void main(String[] args) {
        try {
            JSONReader tr;
            
            if (args.length != 0)
                tr = new JSONReader(new FileInputStream(new File(args[0])));
            else
                tr = new JSONReader("{\"a\" :[1,2, 3,\"b\"],\"b\":23}");
            
            testLoad(tr, true, true);
            
            JSONReader.Token t;
            
            while ((t =tr.next(null)) != null) {
                System.out.println(t.toString());
            }
        } catch (JSONException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}        
    