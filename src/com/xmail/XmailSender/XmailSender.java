package com.xmail.XmailSender;

import com.xmail.Dns.Dns;
import com.xmail.SMTP.SMTP;
import com.xmail.XmailService.XmailConfig;
import org.apache.log4j.Logger;

import javax.naming.NamingException;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Christian on 4/26/2015.
 */
public class XmailSender {

    // config
    public int port = 25;
    public String ehlo = "localhost";
    public String from = "root@localhost";
    public String bindingIPv4 = "0.0.0.0";
    public String bindingIPv6 = "::1";

    // error management
    int lastCode = 0;
    String lastMessage = "";

    // additional information
    int ipIndex = 0;
    boolean ipv6Used = false;
    int mxIndex = 0;

    // internal
    private Socket smtpSocket = null;
    private BufferedReader inSocket = null;
    private BufferedWriter outSocket = null;
    private String CRLF = "\r\n";

    // logger
    final static Logger logger = Logger.getRootLogger();


    public int send(String to, String data) {
        String[] emailParts = to.split("@");
        String domain;

        if(emailParts.length == 2) {
            domain = emailParts[1];

            // Get all MXes
            try {

                String[][] mxs = Dns.getMX(domain);

                for(mxIndex = 0; mxIndex < mxs.length; mxIndex++) {

                    if(XmailConfig.ipv6Enabled) {
                        ipv6Used = true;
                        String[] ips6 = Dns.getAAAA(mxs[mxIndex][1]);
                        int ret = send2IP(to, data, ips6, bindingIPv6);

                        if(ret == SMTP.SERVER_NOT_FOUND || ret == SMTP.MAILBOX_NOT_EXISTS) continue;
                        return ret;
                    }

                    // Try to IPv4
                    ipv6Used = false;
                    String[] ips4 = Dns.getA(mxs[mxIndex][1]);
                    int ret = send2IP(to, data, ips4, bindingIPv4);

                    if(ret == SMTP.SERVER_NOT_FOUND || ret == SMTP.MAILBOX_NOT_EXISTS) continue;
                    return ret;

                }

                // not delivered?
            }
            catch (NamingException e) {
            }
        }
        else {
            // TO DO: implement local delivery (ie RCPT TO: root)
        }

        return SMTP.UNKNOWN_ERROR;
    }

    /**
     *
     * @return
     */
    public boolean sendMail(String to, String data, String mx, String sourceIP) {

        // Open Socket
        if(!connect(mx, sourceIP)) return false;

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
    public boolean connect(String mx, String sourceIP) {
        try {
            smtpSocket = new Socket();
            smtpSocket.setReuseAddress(true);
            smtpSocket.bind(new InetSocketAddress(sourceIP, 0));
            smtpSocket.connect(new InetSocketAddress(mx, port));
            inSocket = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
            outSocket = new BufferedWriter(new OutputStreamWriter(smtpSocket.getOutputStream()));

        } catch (UnknownHostException e) {
            logger.error("SOCKET OPEN: Trying to connect to unknown host: " + e);
            lastCode = 101;
            lastMessage = "Connect: Unknown host.";
            return false;
        }
        catch (IOException e) {
            logger.error("SOCKET OPEN: IOException: " + e);
            lastCode = 101;
            lastMessage = "Connect: IOError";
            return false;
        }

        return true;
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
            logger.error("SOCKET CLOSE: Trying to connect to unknown host: " + e);
        } catch (IOException e) {
            logger.error("SOCKET CLOSE: IOException:  " + e);
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

        try {
            while((response = inSocket.readLine()) != null) {

                // Log each line of response
                logger.debug(key + ": " + response);

                // To avoid a deadlock break the loop after 20 as this should not happen ever
                tries++;
                if (tries > 20) {
                    logger.error("Too many lines received from server.");
                    lastCode = 103;
                    lastMessage = "Too many lines received from server.";
                    return 0;
                }

                // Log  error if expected code not received
                if (Integer.parseInt(response.substring(0, 3)) != expected) {
                    logger.error("Ran into problems sending Mail. Received: " + response.substring(0, 3) +
                            ".. but expected: " + Integer.toString(expected));
                    lastCode = Integer.parseInt(response.substring(0, 3));
                    lastMessage = response.substring(3);
                    close();
                    break;
                }

                // Access denied... Quit
                if (Integer.parseInt(response.substring(0, 3)) == 451) {
                    logger.error("ERROR QUIT: Server declined access. Quitting.");
                    lastCode = 102;
                    lastMessage = "Server declined access. Quitting.";
                    break;
                }

                // Get the last line?
                if(response.indexOf(" ") == 3) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("SOCKET READ: IOException:  " + e);
            lastCode = 103;
            lastMessage = "Socket read IOException: " + e;
            return 0;
        }
        catch (NullPointerException e) {
            logger.error("SOCKET Read: NullPointerException:  " + e);
            lastCode = 103;
            lastMessage = "Socket read: NullPointerException: " + e;
            return 0;
        }

        if (response == null) {
            logger.error("ERROR RESPONSE: Could not get mail server response codes.");
            lastCode = 103;
            lastMessage = "Could not get mail server response codes.";
            return 0;
        }

        lastCode = Integer.parseInt(response.substring(0, 3));
        lastMessage = response.substring(3);
        return lastCode;
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
            logger.error("SOCKET WRITE: IOException:  " + e);
            lastCode = 104;
            lastMessage = "Socket write exception: " + e;
            return false;
        }

        return true;
    }

    private int send2IP(String to, String data, String[] ips, String sourceIP) {

        for(ipIndex = 0; ipIndex < ips.length; ipIndex++) {
            boolean status = sendMail(to, data, ips[ipIndex], sourceIP);
            if(status) return SMTP.SUCCESS;

            // parse the last code
            int lastCode = SMTP.parseReturnCode(getLastCode());

            if(lastCode == SMTP.SERVER_NOT_FOUND) continue;
            else return lastCode;
        }

        // If tried all IPs without success...
        return SMTP.SERVER_NOT_FOUND;
    }

    public int getLastCode() {
        return lastCode;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public int getIpIndex() {
        return ipIndex;
    }

    public int getMxIndex() {
        return mxIndex;
    }

    public boolean isIpv6Used() {
        return ipv6Used;
    }
}
