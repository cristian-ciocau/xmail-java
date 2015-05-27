package com.xmail.main.XmailService;

/**
 * Created by cristian on 5/12/15.
 */
public class IpQueue {
    static int currentIpv4 = 0;
    static int currentIpv6 = 0;

    static String[] ipv4;
    static String[] ipv6;

    private static IpQueue instance = null;

    /**
     * IpQueue.IpQueue()
     *
     * Exists only to defeat instantiation.
     *
     */
    protected IpQueue() {
    }

    /**
     * IpQueue.getInstance()
     *
     * Returns the instance of this singleton class
     *
     * @return
     */
    public static synchronized IpQueue getInstance() {
        if(instance == null) {
            instance = new IpQueue();
        }
        return instance;
    }

    /**
     * IpQueue.init()
     *
     * Initialize this queue with the list of IP addresses
     *
     * @param ipv4List
     * @param ipv6List
     */
    public static void init(String[] ipv4List, String[] ipv6List) {
        ipv4 = ipv4List;
        ipv6 = ipv6List;

        currentIpv4 = 0;
        currentIpv6 = 0;
    }

    /**
     * IpQueue.getIpv4()
     *
     * Reserve an IPv4 for the IPv4 addresses list
     *
     * @return
     */
    public static synchronized String getIpv4() {
        String ip = ipv4[currentIpv4];

        currentIpv4++;
        if(currentIpv4 >= ipv4.length) currentIpv4 = 0;

        return ip;
    }

    /**
     * IpQueue.getIpv6()
     *
     * Reserve an IPv6 from the IPv6 addresses list
     *
     * @return
     */
    public static synchronized String getIpv6() {
        String ip = ipv6[currentIpv6];

        currentIpv6++;
        if(currentIpv6 >= ipv6.length) currentIpv6 = 0;

        return ip;
    }
}
