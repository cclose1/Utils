/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cbc.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.cbc.application.reporting.Report;

/**
 *
 * @author chris
 */
public class HttpClient {
    CloseableHttpClient   httpclient = HttpClients.createDefault();   
    CloseableHttpResponse response   = null;
    HttpEntity            entity     = null;
    String                ctype      = null;
    String                encoding   = null;
    URIBuilder            uri        = new URIBuilder();
    
    private boolean checkContent(boolean errorIfNone) throws IOException {
        if (entity != null) return true;
        
        if (errorIfNone) {
            throw new IOException("No content available. Last get " + uri.toString());
        }
        return true;
    }
    public void setURI(String uri) throws URISyntaxException {
        this.uri = new URIBuilder(uri);        
    }
    public void setScheme(String scheme) {
        uri.setScheme(scheme);
    }
    public void setHost(String host) {
        uri.setHost(host);
    }
    public void setPath(String path) {
        uri.setPath(path);
    }
    public void setPort(int port) {
        uri.setPort(port);
    }
    public void clearParameters() {
        uri.clearParameters();
    }
    public void addParameter(String name, String value) {
        uri.addParameter(name, value);
    }
    public int get() throws IOException, URISyntaxException {
        HttpGet httpget  = new HttpGet(uri.build());
        Header  ct       = null;

        response = httpclient.execute(httpget);
        entity   = response.getEntity();
        ct       = entity.getContentType();
         
        if (ct != null) {
            String[] params = ct.getValue().split(";");
            
            switch (params.length) {
                case 0:
                    ctype    = "htm/text";
                    encoding = "utf-8";
                    break;
                case 1:
                    ctype    = params[0];
                    encoding = "utf-8";
                    break;
                case 2:
                    ctype    = params[0];
                    encoding = params[1];
                    break;
                default:
                    throw new IOException("URL " + uri.toString() + " return unexpected Content-Type " + ct.getValue());
            }
        }        
        return response.getStatusLine().getStatusCode();
    }
    public void get(String url) throws URISyntaxException, IOException {
        uri = new URIBuilder(url);
        
        get();
    }
    public boolean contentAvailable() {        
        return entity != null;
    }
    public InputStream contentStream() throws IOException {
        checkContent(true);
        
        return entity.getContent();
    }
    public String contentString() throws IOException {
        InputStream         is         = entity.getContent();
        final int           bufferSize = 1024;
        final char[]        buffer     = new char[bufferSize];
        final StringBuilder out        = new StringBuilder();
        Reader              in         = new InputStreamReader(is, "UTF-8");
        
        checkContent(true);
        
        for (;;) {
            int rsz = in.read(buffer, 0, buffer.length);
            
            if (rsz < 0) {
                break;
            }
            out.append(buffer, 0, rsz); 
        }
        return out.toString();
    }
    public String getContentType() {
        return ctype;
    }
    public String getEncoding() {
        return encoding;
    }
    public String getUrl() {
        return uri.toString();
    }
    public StatusLine getStatusLine() {
        return response.getStatusLine();
    }
    public void close() {
        if (response != null) try {
            response.close();
        } catch (IOException ex) {
            Report.error(null, "Closing URL", ex);
        }        
        response = null;
        entity   = null;
        ctype    = null;
        encoding = null;
    }
}
