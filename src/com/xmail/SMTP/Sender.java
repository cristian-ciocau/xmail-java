package com.xmail.SMTP;

import org.apache.log4j.Logger;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Christian on 4/26/2015.
 */
public class Sender {
    private final int SERVER_NOT_FOUND = 101;
    private final int ACCESS_DENIED = 102;
    private final int READ_ERROR = 103;
    private final int WRITE_ERROR = 104;
    private final int TLS_ERROR = 105;
    private final int SIZE_ERROR = 106;


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
    boolean usingTls = true;

    // Server supported commands
    boolean isTls = false;
    boolean isSize = false;
    long mailSize = 0;

    // internal
    private Socket smtpSocket = null;
    private SSLSocket smtpSSLSocket = null;
    private BufferedReader inSocket = null;
    private BufferedWriter outSocket = null;
    private String CRLF = "\r\n";

    // logger
    final static Logger logger = Logger.getRootLogger();

    /**
     * Sender.sendMail()
     *
     *
     *
     * @param to
     * @param data
     * @param mx
     * @param sourceIP
     * @return
     */
    public boolean sendMail(String to, String data, String mx, String sourceIP) {

        int retCode;
        String sizeCommand = "";
        boolean heloUsed = false;

        // Open Socket
        if(!connect(mx, sourceIP)) return false;

        // an one-pass loop, used just to make code nicer
        while(true) {

            // Read the welcome message
            if (smtpRead(220, "WELCOME") != 220) break;

            // Send Enhanced HELLO
            if (!smtpWrite("EHLO " + ehlo + CRLF)) return false;
            retCode = smtpRead(250, "EHLO");
            if (retCode == 500 || retCode == 501 || retCode == 502 || retCode == 550) {
                // EHLO not implemented... try HELO
                heloUsed = true;

                if (!smtpWrite("HELO " + ehlo + CRLF)) return false;
                if (smtpRead(250, "HELO") != 250) break;
            }
            // Other EHLO error
            else if (retCode != 250) break;

            // Check here if STARTTLS is enabled and if we can use it
            if (usingTls && isTls && !heloUsed) {

                // Send the STARTTLS command
                if (!smtpWrite("STARTTLS" + CRLF)) return false;
                if (smtpRead(220, "STARTTLS") != 220) break;

                // Wrap the socket to work on TLS
                if (!startTls()) return false;

                // Send the EHLO message again
                if (!smtpWrite("EHLO " + ehlo + CRLF)) return false;
                if (smtpRead(250, "EHLO") != 250) break;
            }

            // if the server advertised SIZE in EHLO
            if (isSize) {
                // The mail is bigger than server advertised
                if (mailSize > 0 && mailSize < data.length()) {
                    lastCode = SIZE_ERROR;
                    lastMessage = "Mail too large.";
                    break;
                }

                // Add the SIZE to MAIL command
                sizeCommand = " SIZE " + Integer.toString(data.length());
            }

            // Send sender
            if (!smtpWrite("MAIL FROM: <" + from + ">" + sizeCommand + CRLF)) return false;
            if (smtpRead(250, "MAIL FROM") != 250) break;

            // Send recipient
            if (!smtpWrite("RCPT TO: <" + to + ">" + CRLF)) return false;
            if (smtpRead(250, "RCPT TO") != 250) break;

/*
            // Send data start command
            if(!smtpWrite("DATA" + CRLF)) return false;
            if(smtpRead(354, "DATA") != 354) break;

            // Send the e-mail data
            if(!smtpWrite(data + CRLF)) break;

            // Send a dot to show we're finished
            if(!smtpWrite("." + CRLF)) return false; // this line sends a dot to mark the end of message
            if(smtpRead(250, "DOT") != 250) break;
*/

            // Ok, we've sent all emails...
            break;
        }
        // so everything breaks the loop finishes with QUIT

        // Send QUIT
        if (!smtpWrite("QUIT" + CRLF)) return false;
        smtpRead(221, "QUIT");

        close();

        return true;
    }

    /**
     * Sender.connect()
     *
     * Initiate a TCP connection for SMTP exchange
     *
     * @param mx the remote IP address
     * @param sourceIP the local IP address
     * @return true | false
     */
    private boolean connect(String mx, String sourceIP) {
        try {

            smtpSocket = new Socket();
            smtpSocket.setReuseAddress(true);
            smtpSocket.bind(new InetSocketAddress(sourceIP, 0));
            smtpSocket.connect(new InetSocketAddress(mx, port));
            inSocket = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
            outSocket = new BufferedWriter(new OutputStreamWriter(smtpSocket.getOutputStream()));

        } catch (UnknownHostException e) {
            logger.error("SOCKET OPEN: Trying to connect to unknown host: " + e);
            lastCode = SERVER_NOT_FOUND;
            lastMessage = "Connect: Unknown host.";
            return false;
        }
        catch (IOException e) {
            logger.error("SOCKET OPEN: IOException: " + e);
            lastCode = SERVER_NOT_FOUND;
            lastMessage = "Connect: IOError";
            return false;
        }

        return true;
    }

    /**
     * Sender.close()
     *
     * Closes everything
     */
    private void close() {
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
     * Sender.smtpRead()
     *
     * Reads from socket and parses the response
     *
     * @param expected the SMTP code we except to receive
     * @param key used for logging
     * @return the SMTP code read
     */
    private int smtpRead(int expected, String key) {
        int tries = 0;
        String response;

        Pattern sizeRegex = Pattern.compile("(?i)250-SIZE[ ]+([0-9]+)");

        try {
            while((response = inSocket.readLine()) != null) {

                // Log each line of response
                logger.debug(key + ": " + response);

                // To avoid a deadlock break the loop after 20 as this should not happen ever
                tries++;
                if (tries > 20) {
                    logger.error("Too many lines received from server.");
                    lastCode = READ_ERROR;
                    lastMessage = "Too many lines received from server.";
                    return 0;
                }

                // Check for STARTTLS command
                if(response.contains("250-STARTTLS")) isTls = true;

                // Check for SIZE command
                Matcher mSize = sizeRegex.matcher(response);
                if(mSize.find()) {
                    isSize = true;
                    mailSize = Long.parseLong(mSize.group(1));
                }

                // Get the last line?
                if(response.indexOf(" ") == 3) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("SOCKET READ: IOException:  " + e);
            lastCode = READ_ERROR;
            lastMessage = "Socket read IOException: " + e;
            return 0;
        }
        catch (NullPointerException e) {
            logger.error("SOCKET Read: NullPointerException:  " + e);
            lastCode = READ_ERROR;
            lastMessage = "Socket read: NullPointerException: " + e;
            return 0;
        }

        if (response == null) {
            logger.error("ERROR RESPONSE: Could not get mail server response codes.");
            lastCode = READ_ERROR;
            lastMessage = "Could not get mail server response codes.";
            return 0;
        }

        // Log  error if expected code not received
        if (Integer.parseInt(response.substring(0, 3)) != expected) {
            logger.error("Ran into problems sending Mail. Received: " + response.substring(0, 3) +
                    ".. but expected: " + Integer.toString(expected));
            lastCode = Integer.parseInt(response.substring(0, 3));
            lastMessage = response.substring(3);
            close();
        }

        // Access denied... Quit
        if (Integer.parseInt(response.substring(0, 3)) == 451) {
            logger.error("ERROR QUIT: Server declined access. Quitting.");
            lastCode = ACCESS_DENIED;
            lastMessage = "Server declined access. Quitting.";
        }

        lastCode = Integer.parseInt(response.substring(0, 3));
        lastMessage = response.substring(3);
        return lastCode;
    }

    /**
     * Sender.smtpWrite()
     *
     * Writes a buffer to socket
     *
     * @param buffer the data that should be sent
     * @return true | false
     */
    public boolean smtpWrite(String buffer) {
        try {
            outSocket.write(buffer);
            outSocket.flush();
        } catch (IOException e) {
            logger.error("SOCKET WRITE: IOException:  " + e);
            lastCode = WRITE_ERROR;
            lastMessage = "Socket write exception: " + e;
            return false;
        }

        return true;
    }

    /**
     * Sender.startTls()
     *
     * Wraps a regular socket to a smarter one with TLS encryption
     *
     * @return true | false
     */
    private boolean startTls() {
        try {
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            smtpSSLSocket = (SSLSocket) sslsocketfactory.createSocket(
                    smtpSocket,
                    smtpSocket.getInetAddress().getHostAddress(),
                    smtpSocket.getPort(),
                    true);

            smtpSSLSocket.setUseClientMode(true);

            smtpSSLSocket.startHandshake();

            inSocket = new BufferedReader(new InputStreamReader(smtpSSLSocket.getInputStream()));
            outSocket = new BufferedWriter(new OutputStreamWriter(smtpSSLSocket.getOutputStream()));
        } catch (IOException e) {
            // Some certificates will appear to be invalid if the authority which issued them
            // is not present in $JAVA_HOME/jre/lib/security/cacerts
            // For example, the certificates emitted by: https://www.startssl.com/
            // You have to manually trust StartSSL(tm): https://eknet.org/main/dev/startcom-keystore.html#sec-2

            logger.error(e.getMessage());

            lastCode = TLS_ERROR;
            lastMessage = "Error starting TLS.";

            return false;
        }

        return true;
    }

    /**
     * Sender.getLastCode()
     *
     * Returns last received SMTP code
     *
     * @return last received SMTP code
     */
    public int getLastCode() {
        return lastCode;
    }

    /**
     * Sender.getLastMessage()
     *
     * Returns last error message
     *
     * @return last error message
     */
    public String getLastMessage() {
        return lastMessage;
    }

    /**
     *
     */
    public void disableTls() {
        usingTls = false;
    }
}
