package com.xmail.SMTP;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cristian on 5/7/15.
 */
public class SMTP {

    public static final int SUCCESS = 0;
    public static final int SERVER_NOT_FOUND = -1;
    public static final int MAILBOX_NOT_EXISTS = -2;
    public static final int TEMPORARY_ERROR = -3;
    public static final int PERMANENT_ERROR = -4;
    public static final int UNKNOWN_ERROR = -5;

    public static int parseReturnCode(int returnCode) {

        switch (returnCode) {
            // Server not found
            case 101:
                return SERVER_NOT_FOUND;

            // Delivered
            case 250:
            case 251:
            case 252:
                return SUCCESS;

            // The mailbox specified in the address does not exist.
            case 550:
            case 551:
                return MAILBOX_NOT_EXISTS;

                // Server asked to close connection
            case 102:

                // Transmission Error
            case 103:
            case 104:

                // Connection Timeout
            case 421:

                // Quota exceeded
            case 422:

            case 431:
            case 432:

                // The connection was dropped during the transmission.
            case 442:

            case 450:
            case 451:
            case 452:

                // This is a local error with the sending server
            case 471:

                // queue
                return TEMPORARY_ERROR;

            // The host server for the recipient’s domain name cannot be found (DNS error)
            case 512:

                // Size matters
            case 523:

                // Authentication required or spam
            case 530:

                // Spam?
            case 541:

            case 552:
            case 553:

                // Spam?
            case 554:

                // I have been told not to work with you !!!
            case 571:

                // do no try again (ever)
                return PERMANENT_ERROR;

            // Other reason
            default:
                // queue
                return UNKNOWN_ERROR;
        }
    }
}
