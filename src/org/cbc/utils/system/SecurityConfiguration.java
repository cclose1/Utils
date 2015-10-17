/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cbc.utils.system;

/**
 *
 * @author Chris
 */
public interface SecurityConfiguration {
    boolean getLogRequest();
    boolean getLogReply();
    boolean getLoginRequired();
    String  getHashAlgorithm();
}
