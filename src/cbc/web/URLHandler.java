/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cbc.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author Chris
 */
public class URLHandler {

    private URL                url;
    private HttpURLConnection  http;
    private HttpsURLConnection https;

    public void getURL(String url) throws MalformedURLException, IOException {
        this.url   = new URL(url);
        this.http  = (HttpURLConnection) this.url.openConnection();
        this.https = this.url.getProtocol().equalsIgnoreCase("https") ? (HttpsURLConnection) http : null;
    }
    public int getResponseCode() throws IOException {
        return http.getResponseCode();
    }
    public String getResponseMessage() throws IOException {
        return http.getResponseMessage();
    }
    public String getProtocol() {
        return url.getProtocol();
    }
    public String getResponse() throws IOException {
        String         line;
        StringBuilder  response = new StringBuilder();
        BufferedReader ins      = new BufferedReader(new InputStreamReader(http.getInputStream()));

        while ((line = ins.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }
}
