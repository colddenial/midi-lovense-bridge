package org.openstatic;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PendingURLFetch implements Runnable
{
    private String url;
    private String response;
    
    public PendingURLFetch(String url)
    {
        this.url = url;
        this.response = null;
    }
    
    /** Convert a Map Object into a query string **/
    public static String mapToQuery(Map<String, String> table)
    {
        if (table != null)
        {
            try
            {
                StringBuilder sb = new StringBuilder("?");
                Set<String> keyset = table.keySet();
                Iterator<String> i = keyset.iterator();
                while(i.hasNext())
                {
                    String key = i.next();
                    String value = table.get(key);
                    sb.append(key + "=" + URLEncoder.encode(value, "UTF-8"));
                    if (i.hasNext())
                        sb.append("&");
                }
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return "";
    }
    
    /** Read the contents of an InputStream into a String **/
    private static String readInputStreamToString(InputStream is)
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int inputByte;
            while ((inputByte = is.read()) > -1)
            {
                baos.write(inputByte);
            }
            is.close();
            return new String(baos.toByteArray());
        } catch (Exception e) {
            System.err.println("readInputStreamToString " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
    
    public void run()
    {
        try
        {
            URL url_object = new URL(this.url);
            HttpURLConnection con = (HttpURLConnection) url_object.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(15000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "MidiLovenseBridge 1.0");
            con.connect();
            int response_code = con.getResponseCode();
            if (response_code == 200)
            {
                InputStream is = con.getInputStream();
                this.response = readInputStreamToString(is);
            }
        } catch (Exception e) {
            //e.printStackTrace(System.err);
        }
    }
    
    public String getResponse()
    {
        return this.response;
    }
    
    public String toString()
    {
        return this.url;
    }
}
