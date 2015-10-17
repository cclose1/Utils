/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.system;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 *
 * @author Chris
 */
public class SecurityHash {
    MessageDigest md          = null;
    private       String salt = null;
    
    private String bytetoString(byte[] input) {
        return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(input);
    }
    public void setAlgorithm(String algorithm) throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance(algorithm);
    }
    public String getRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[length / 2];
        random.nextBytes(bytes);
        
        return bytetoString(bytes);
    }
    /**
     * @param salt the salt to set
     */
    public void setSalt(String salt) {
        this.salt = salt;
        
        if (salt == null) {
            this.salt = getRandomString(40);
        }
    }
    /**
     * @return the salt
     */
    public String getSalt() {
        if (salt == null) setSalt(null);
        
        return salt;
    }
    public String getHash(String password) throws NoSuchAlgorithmException {
        if (md == null) setAlgorithm("SHA");
        
        return bytetoString(md.digest((getSalt() + password).getBytes()));
    }
}
