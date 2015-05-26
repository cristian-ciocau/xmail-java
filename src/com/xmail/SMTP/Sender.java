package com.xmail.SMTP;

import org.apache.commons.io.IOExceptionWithCause;
import org.apache.log4j.Logger;
import org.omg.CORBA.SystemException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.UnexpectedException;
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
     * Sends the email to the given MX using the outgoing sourceIP
     *
     * @param to
     * @param data
     * @param mx
     * @param sourceIP
     * @return
     */
    public boolean sendMail(String to, String data, String mx, String sourceIP) {

        // Open Socket
        if(!connect(mx, sourceIP)) return false;

        // Talk to SMTP server
        smtpDialogue(to, data);

        // keep last error / message
        int tempCode = getLastCode();
        String tempMessage = getLastMessage();

        // everything will finish with QUIT (if possible)
        if(smtpWrite("QUIT" + CRLF)) smtpRead(221, "QUIT");

        // Close Socket
        close();

        // and return
        if(tempCode == 250) {
            lastCode = 250;
            lastMessage = "Mail sent OK";
            return true;
        }
        else {
            lastCode = tempCode;
            lastMessage = tempMessage;
            return false;
        }
    }

    /**
     * Sender.smtpDialogue()
     *
     * This contains all needed SMTP commands used to send a mail.
     * It is used to keep the code clean and to offer the opportunity
     * to send a QUIT command if other command failed (i.e. MAIL, RCPT).
     *
     * @param to
     * @param data
     * @return
     */
    private boolean smtpDialogue(String to, String data) {
        int retCode;
        String sizeCommand = "";
        boolean heloUsed = false;

        // Read the welcome message
        if(smtpRead(220, "WELCOME") != 220) return false;

        // Send Enhanced HELO
        if(!smtpWrite("EHLO " + ehlo + CRLF)) return false;
        retCode = smtpRead(250, "EHLO");
        if (retCode == 500 || retCode == 501 || retCode == 502 || retCode == 550) {
            // EHLO not implemented... try HELO
            heloUsed = true;

            if(!smtpWrite("HELO " + ehlo + CRLF)) return false;
            if(smtpRead(250, "HELO") != 250) return false;
        }
        // Other EHLO error
        else if (retCode != 250) return false;

        // Check here if STARTTLS is enabled and if we can use it
        if (usingTls && isTls && !heloUsed) {

            // Send the STARTTLS command
            if(!smtpWrite("STARTTLS" + CRLF)) return false;
            if(smtpRead(220, "STARTTLS") != 220) return false;

            // Wrap the socket to work on TLS
            if (!startTls()) return false;

            // Send the EHLO message again
            if(!smtpWrite("EHLO " + ehlo + CRLF)) return false;
            if (smtpRead(250, "EHLO") != 250) return false;
        }

        // if the server advertised SIZE in EHLO
        if (isSize && !heloUsed) {
            // The mail is bigger than server advertised
            if (mailSize > 0 && mailSize < data.length()) {
                lastCode = SIZE_ERROR;
                lastMessage = "Mail too large.";
                return false;
            }

            // Add the SIZE to MAIL command
            sizeCommand = " SIZE=" + Integer.toString(data.length());
        }

        // Send sender
        if(!smtpWrite("MAIL FROM: <" + from + ">" + sizeCommand + CRLF)) return false;
        if (smtpRead(250, "MAIL FROM") != 250) return false;

        // Send recipient
        if(!smtpWrite("RCPT TO: <" + to + ">" + CRLF)) return false;
        if (smtpRead(250, "RCPT TO") != 250) return false;

        // Send data start command
        if(!smtpWrite("DATA" + CRLF)) return false;
        if (smtpRead(354, "DATA") != 354) return false;

        // Send the e-mail data
        if(!smtpWrite(data + CRLF, false)) return false;

        // Send a dot to show we're finished
        if(!smtpWrite("." + CRLF)) return false;
        if (smtpRead(250, "DOT") != 250) return false;

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
     * @param key used for logging
     * @return the SMTP code read
     */
    private int smtpRead(int expected, String key) {
        int tries = 0;
        String responseLine, response = "";

        Pattern sizeRegex = Pattern.compile("(?i)250-SIZE[ ]+([0-9]+)");

        try {
            while((responseLine = inSocket.readLine()) != null) {
                response += responseLine;

                // Log each line of response
                logger.debug("Read " + key + ": " + responseLine);

                // To avoid a deadlock break the loop after 20 as this should not happen ever
                tries++;
                if (tries > 20) {
                    logger.error("Too many lines received from server.");
                    lastCode = 554;
                    lastMessage = "Mail loop detected";
                    throw new IOException();
                }

                // Check for STARTTLS command
                if(responseLine.contains("250-STARTTLS")) isTls = true;

                // Check for SIZE command
                Matcher mSize = sizeRegex.matcher(responseLine);
                if(mSize.find()) {
                    isSize = true;
                    mailSize = Long.parseLong(mSize.group(1));
                }

                // Get the last line?
                if(responseLine.indexOf(" ") == 3) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("SOCKET READ: IOException:  " + e);
            lastCode = READ_ERROR;
            lastMessage = "Error reading from MTA.";
            return READ_ERROR;
        }
        catch (NullPointerException e) {
            logger.error("SOCKET Read: NullPointerException:  " + e);
            lastCode = READ_ERROR;
            lastMessage = "Error reading from MTA.";
            return READ_ERROR;
        }

        // If we didn't get the expected code
        if(Integer.parseInt(response.substring(0, 3)) != expected) {
            logger.error(String.format("Ran into problems sending Mail. Received: %3d... but expected: %3d",
                    Integer.parseInt(response.substring(0, 3)),
                    expected) );
        }

        // If Access denied... Quit
        if (Integer.parseInt(response.substring(0, 3)) == 451) {
            logger.error("ERROR QUIT: Server declined access. Quitting.");
            lastCode = ACCESS_DENIED;
            lastMessage = "Server declined access. Quitting.";
            return ACCESS_DENIED;
        }

        lastCode = Integer.parseInt(response.substring(0, 3));
        lastMessage = response;
        return lastCode;
    }

    /**
     * Sender.smtpWrite()
     *
     * Writes a buffer to socket
     *
     * @param buffer the data that should be sent
     */
    public boolean smtpWrite(String buffer, boolean log) {
        try {
            outSocket.write(buffer);
            outSocket.flush();

            if(log) {
                logger.info("Write: " + buffer.substring(0, buffer.length() - 2));
            }
        } catch (IOException e) {
            logger.error("SOCKET WRITE: IOException:  " + e);
            lastCode = WRITE_ERROR;
            lastMessage = "Socket write exception: " + e;
            return false;
        }

        return true;
    }

    /**
     * Sender.smtpWrite()
     *
     * Overloaded method
     *
     * @param buffer
     * @return
     */
    public boolean smtpWrite(String buffer) {
        return smtpWrite(buffer, true);
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
     * Sender.disableTls()
     *
     * Disables the TLS command (used by the email queue mechanism)
     *
     */
    public void disableTls() {
        usingTls = false;
    }
}
