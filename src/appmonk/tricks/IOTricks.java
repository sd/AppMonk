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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;

public class IOTricks {

    // "http://example.com/icons/icon.jpg" => "http-example.com-icons-icon.jpg"
    // Used when caching images to local storage
    public static String sanitizeFileName(String url) {
        String fileName = url.replaceAll("[:\\/\\?\\&\\|]+", "-");
        if (fileName.length() > 128) {
            fileName = appmonk.tricks.TextTricks.md5Hash(fileName);
        }
        return fileName;
    }
    
    public static void saveObject(Object object, File fileName) throws IOException {
        File tempFileName = new File(fileName.toString() + "-tmp");

        FileOutputStream fileOut = new FileOutputStream(tempFileName);
        BufferedOutputStream buffdOut = new BufferedOutputStream(fileOut, 32 * 1024);
        ObjectOutputStream objectOut = new ObjectOutputStream(buffdOut);
        
        objectOut.writeObject(object);
        
        objectOut.close();
        buffdOut.close();
        fileOut.close();
        
        fileName.delete();
        tempFileName.renameTo(fileName);
    }
    
    public static Object loadObject(File fileName) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(fileName);
        BufferedInputStream buffdIn = new BufferedInputStream(fileIn, 32 * 1024);
        ObjectInputStream objectIn = new ObjectInputStream(buffdIn);
        
        Object object = objectIn.readObject();
        
        objectIn.close();
        buffdIn.close();
        fileIn.close();
        
        return object;
    }
    
    public static int copyStreamToStream(InputStream in, OutputStream out) throws IOException {
        return copyStreamToStream(in, out, 1024);
    }

    public static int copyStreamToStream(InputStream in, OutputStream out, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int total = 0;
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
            total += len;
        }
        return total;
    }

    public static int copyStreamToStringBuffer(InputStream in, StringBuffer sb) throws IOException {
        return copyStreamToStringBuffer(in, sb, 1024);
    }
    
    public static int copyStreamToStringBuffer(InputStream in, StringBuffer sb, int bufferSize) throws IOException {
        Reader inReader = new InputStreamReader(in, "UTF-8");
        
        char[] buffer = new char[bufferSize];
        int total = 0;
        int len;
        while ((len = inReader.read(buffer)) > 0) {
            sb.append(buffer, 0, len);
            total += len;
        }
        return total;
    }
    
    public static String StreamToString(InputStream in) {
        StringBuffer sb = new StringBuffer();
        try {
            copyStreamToStringBuffer(in, sb);
        }
        catch (IOException e) {
            return null;
        }
        return sb.toString();
    }

    public static byte[] StreamToBytes(InputStream in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            copyStreamToStream(in, out);
        }
        catch (IOException e) {
            return null;
        }
        return out.toByteArray();
    }
    
    public static int countBytesInStream(InputStream in) {
        try {
            byte[] buffer = new byte[32 * 1024];
            int total = 0;
            int len;
            while ((len = in.read(buffer)) > 0) {
                total += len;
            }
            return total;
        }
        catch (IOException e) {
            return 0;
        }
    }

}
