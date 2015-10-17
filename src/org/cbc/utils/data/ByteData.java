/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.data;

/**
 *
 * @author CClose
 */
public class ByteData {
    protected byte[] data;
    private int base = 0;
    
    public static int getInt(byte[] data, int offset, int length) {
        int value = 0;
        
        for (int i = 0; i < length && offset + i < data.length; i++) value = 256 * value + (data[offset + i] & 255);
        
        return value;
    }
    public ByteData(byte[] data) {
        this.data = data;
    }
    public ByteData(byte[] data, int base) {
        this.data = data;
        this.base = base;
    }
    public int getLength() {
        return data == null? 0 : data.length - base;
    }
    public int getInt(int offset, int length) {
        return getInt(data, base + offset, length);
    }
    public byte getByte(int offset) {
        return data[base + offset];
    }
}
