/*
 * Copyright (c) 2017, Tony Opara
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this 
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * Neither the name of Google nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.refine.extension.database;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.util.text.TextEncryptor;

public class SimpleTextEncryptor implements TextEncryptor {

    private final StandardPBEStringEncryptor encryptor;

    @Override
    public String decrypt(String encryptedMessage) {
        return this.encryptor.decrypt(encryptedMessage);
    }

    @Override
    public String encrypt(String message) {
        return this.encryptor.encrypt(message);
    }

    public SimpleTextEncryptor(String passwordChars) {
        super();

        this.encryptor = new StandardPBEStringEncryptor();
        this.encryptor.setAlgorithm("PBEWithMD5AndDES");
        this.encryptor.setPasswordCharArray(passwordChars.toCharArray());
    }

    public void setPassword(final String password) {
        this.encryptor.setPassword(password);
    }

    /**
     * Sets a password, as a char[]
     * 
     * @since 1.8
     * @param password
     *            the password to be set.
     */
    public void setPasswordCharArray(final char[] password) {
        this.encryptor.setPasswordCharArray(password);
    }

}
