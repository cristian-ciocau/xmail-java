package com.xmail.XmailService;

/**
 * Created by cristian on 5/12/15.
 */
public class IpQueue {
    static int currentIpv4 = 0;
    static int currentIpv6 = 0;

    static String[] ipv4;
    static String[] ipv6;

    public static void init(String[] ipv4List, String[] ipv6List) {
        ipv4 = ipv4List;
        ipv6 = ipv6List;

        currentIpv4 = 0;
        currentIpv6 = 0;
    }

    public static synchronized String getIpv4() {
        String ip = ipv4[currentIpv4];

        currentIpv4++;
        if(currentIpv4 > ipv4.length) currentIpv4 = 0;

        return ip;
    }

    public static synchronized String getIpv6() {
        String ip = ipv6[currentIpv6];

        currentIpv6++;
        if(currentIpv6 > ipv6.length) currentIpv6 = 0;

        return ip;
    }
}
