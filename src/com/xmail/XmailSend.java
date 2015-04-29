package com.xmail;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Christian on 4/26/2015.
 */
public class XmailSend {
    public int port = 25;

    public String ehlo = "localhost";
    public String from = "root@localhost";

    public Map<String, String> log;

    Socket smtpSocket = null;
    BufferedReader inSocket = null;
    BufferedWriter outSocket = null;
    String CRLF = "\r\n";

    /**
     *
     * @return
     */
    public boolean send(String to, String content, String headers) {
        String mx, data = headers + CRLF + content;

        // init
        init();

        String[] emailParts = to.split("@");

        // Get MX IP
        mx = getRandMXIP(emailParts[1]);

        // Open Socket
        connect(mx);

        // Read the welcome message
        if (smtpRead(220, "WELCOME") != 220)return false;

        // Send Enhanced HELO
        if (!smtpWrite("EHLO " + ehlo + CRLF)) return false;
        if (smtpRead(250, "HELO") != 250) return false;

        // Send sender
        if(!smtpWrite("MAIL FROM: <" + from  + ">"  + CRLF)) return false;
        if(smtpRead(250, "MAIL FROM") != 250) return false;

        // Send recipient
        if(!smtpWrite("RCPT TO: <" + to + ">" + CRLF)) return false;
        if(smtpRead(250, "RCPT TO") != 250) return false;

        // Send data start command
        if(!smtpWrite("DATA" + CRLF)) return false;
        if(smtpRead(354, "DATA") != 354) return false;

        // Send the e-mail data
        if(!smtpWrite(data + CRLF)) return false;

        // Send a dot to show we're finished
        if(!smtpWrite("." + CRLF)) return false; // this line sends a dot to mark the end of message
        if(smtpRead(250, "DOT") != 250) return false;

        // Send QUIT
        if (!smtpWrite("QUIT" + CRLF)) return false;
        smtpRead(221, "QUIT");

        close();

        return true;
    }

    /**
     *
     */
    public void init() {
        log  = new HashMap<String, String>();
    }

    /**
     *
     */
    public void connect(String mx) {
        try {
            smtpSocket = new Socket(mx, port);
            inSocket = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
            outSocket = new BufferedWriter(new OutputStreamWriter(smtpSocket.getOutputStream()));

        } catch (UnknownHostException e) {
            log.put("SOCKET OPEN", "Trying to connect to unknown host: " + e);
        }
        catch (IOException e) {
            log.put("SOCKET OPEN", "IOException: " + e);
        }
    }

    /**
     *
     */
    public void close() {
        try {
            inSocket.close();
            outSocket.close();
            smtpSocket.close();
        } catch (UnknownHostException e) {
            log.put("SOCKET CLOSE", "Trying to connect to unknown host: " + e);
        } catch (IOException e) {
            log.put("SOCKET CLOSE", "IOException:  " + e);
        }
    }

    /**
     *
     * @param expected
     * @param key
     * @return
     */
    public int smtpRead(int expected, String key) {
        int tries = 0;
        String response;

        log.put(key, "");

        try {
            while((response = inSocket.readLine()) != null) {

                // Log each line of response
                log.put(key, log.get(key) + response);

                // To avoid a deadlock break the loop after 20 as this should not happen ever
                tries++;
                if (tries > 20) {
                    return 0;
                }

                // Log  error if expected code not received
                if (Integer.parseInt(response.substring(0, 3)) != expected) {
                    log.put("ERROR CODES", "Ran into problems sending Mail. Received: " + response.substring(0, 3) +
                            ".. but expected: " + Integer.toString(expected));
                    close();
                    break;
                }

                // Access denied... Quit
                if (Integer.parseInt(response.substring(0, 3)) == 451) {
                    log.put("ERROR QUIT", "Server declined access. Quitting.");
                    break;
                }

                // Get the last line?
                if(response.indexOf(" ") == 3) {
                    break;
                }
            }
        } catch (IOException e) {
            log.put("SOCKET CLOSE", "IOException:  " + e);
            return 0;
        }

        if (response == null) {
            log.put("ERROR RESPONSE", "Could not get mail server response codes.");
            return 0;
        }

        return Integer.parseInt(response.substring(0, 3));
    }

    /**
     *
     * @param buffer
     * @return
     */
    public boolean smtpWrite(String buffer) {
        try {
            outSocket.write(buffer);
            outSocket.flush();
        } catch (IOException e) {
            log.put("SOCKET WRITE", "IOException:  " + e);
            return false;
        }

        return true;
    }

    private String getRandMXIP(String domain) {
        return "67.210.232.51";
    }

    private String getRandMX(String domain) {
        return "mailwhere.com";
    }

}
