package appmonk.tricks;

/*
 * Copyright (C) 2009, 2010 Sebastian Delmont <sd@notso.net>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 * 
 */

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

public class TextTricks {
    public static String join(List<String> list, String separator) {
        StringBuffer join = new StringBuffer();
        int count = list.size();
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                // nothing
            }
            else {
                join.append(separator);
            }
            join.append(list.get(i));
        }
        return join.toString();
    }
    
    public static String joinWithPrefix(String prefix, Object[] list, String separator) {
        StringBuffer join = new StringBuffer();
        int count = list.length;
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                // nothing
            }
            else {
                join.append(separator);
            }
            join.append(prefix);
            join.append(list[i]);
        }
        return join.toString();
    }

    // ["a", "b", "c"] => "a, b and c"
    public static String listToParagraph(List<String> list) {
        return listToParagraph(list, ", ", " and ");
    }

    public static String listToParagraph(List<String> list, String comma, String and) {
        StringBuffer para = new StringBuffer();
        int count = list.size();
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                // nothing
            }
            else if (i == count - 1) {
                para.append(and);
            }
            else {
                para.append(comma);
            }
            para.append(list.get(i));
        }
        return para.toString();
    }
    
    public static MessageDigest md5MessageDigest = null;
    public static String md5Hash(String value) {
        if (md5MessageDigest == null) {
            try {
                md5MessageDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
        }
        
        md5MessageDigest.reset();
        md5MessageDigest.update(value.getBytes(), 0, value.length());

        String hash = new BigInteger(1, md5MessageDigest.digest()).toString(16);
        while (hash.length() < 32)
            hash = "0" + hash;
        return hash;
    }
    
    public static final Pattern REGEXP_PHONE_NUMBER_USA = Pattern.compile("^\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}$");
    public static final Pattern REGEXP_SIMPLE_EMAIL = Pattern.compile("^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,8}$");

    public static boolean isValidPhoneNumber(String phoneNumber) {
        return REGEXP_PHONE_NUMBER_USA.matcher(phoneNumber).matches();
    }
    
    public static boolean isValidEmail(String email) {
        return REGEXP_SIMPLE_EMAIL.matcher(email).matches();
    }

}
