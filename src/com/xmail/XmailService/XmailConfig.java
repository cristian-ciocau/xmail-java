package com.xmail.XmailService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cristian on 5/1/15.
 */
public class XmailConfig {
    public static String ehlo = "localhost";

    public static String dbPath = "/Data/git/xmail-java/data/data.db";

    public static int maxSmtpThreads = 1;

    public static int loopTime = 10;

    public static List<Integer> retryTime = new ArrayList<Integer>() {{
        add(0, 15); // First retry = 15 minutes
        add(1, 180); // Second retry = 3 hours
        add(2, 180); // Second retry = 3 hours
        add(3, 360); // Second retry = 6 hours
        add(4, 720); // Second retry = 12 hours
    }};

    public static int maxRetryCount = retryTime.size();
}
