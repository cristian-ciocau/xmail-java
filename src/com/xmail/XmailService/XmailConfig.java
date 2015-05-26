package com.xmail.XmailService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cristian on 5/1/15.
 */
public class XmailConfig {

    public static String ehlo = "example.com";

    public static String bounceFrom = "postmaster@example.com";

    public static String dbPath = "/tmp/xmail-java/data/data.db";

    public static String mailPath = "/tmp/xmail-java/mail/";

    public static int port = 24;

/*
    public static int maxSmtpThreads = 10;
*/
    // Just for debug
    public static int maxSmtpThreads = 2;

    public static int loopTime = 10;

    public static String[] outgoingIPv4 = new String[] {
            "0.0.0.0",
    };

    public static String[] outgoingIPv6 = new String[] {
            "::1",
    };

    public static boolean ipv6Enabled = false;
/*
    public static List<Integer> retryTime = new ArrayList<Integer>() {{
        add(0, 15 * 60); // First retry = 15 minutes
        add(1, 180 * 60); // Second retry = 3 hours
        add(2, 180 * 60); // Second retry = 3 hours
        add(3, 360 * 60); // Second retry = 6 hours
        add(4, 720 * 60); // Second retry = 12 hours
    }};
*/

    // Just for debug
    public static List<Integer> retryTime = new ArrayList<Integer>() {{
        add(0, 20); // First retry = 20 sec
        add(1, 40); // Second retry = 40 sec
        add(2, 60); // Second retry = 60 sec
    }};

    public static int maxRetryCount = retryTime.size();
}
