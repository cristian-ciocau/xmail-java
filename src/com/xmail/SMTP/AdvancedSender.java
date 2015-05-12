package com.xmail.SMTP;

import com.xmail.Dns.Dns;
import com.xmail.XmailService.XmailConfig;

import javax.naming.NamingException;

/**
 * Created by cristian on 5/12/15.
 */
public class AdvancedSender extends Sender {
    int ipIndex = 0;
    boolean ipv6Used = true;
    int mxIndex = 0;

    /**
     * Sender.send()
     *
     * Send an e-mail using Round Robin MXs
     *
     * @param to
     * @param data
     * @return
     */
    public int send(String to, String data) {
        String[] emailParts = to.split("@");
        String domain;

        if(emailParts.length == 2) {
            domain = emailParts[1];

            // Get all MXes
            try {

                String[][] mxs = Dns.getMX(domain);

                if(mxIndex > mxs.length) mxIndex = 0;

                for(; mxIndex < mxs.length; mxIndex++) {

                    if(XmailConfig.ipv6Enabled && ipv6Used) {
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
     * Sender.send2IP()
     *
     * Send a mail to all IP addresses of a MX
     *
     * @param to recipient
     * @param data all email data (headers + body)
     * @param ips the list of IP addresses where to try
     * @param sourceIP the source IP address
     * @return SMTP code
     */
    private int send2IP(String to, String data, String[] ips, String sourceIP) {

        if(ipIndex > ips.length) ipIndex = 0;

        for(; ipIndex < ips.length; ipIndex++) {
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

    /**
     * AdvancedSender.getIpIndex()
     *
     * Returns the current index of IP in Round Robin queue
     *
     * @return
     */
    public int getIpIndex() {
        return ipIndex;
    }

    /**
     * AdvancedSender.getMxIndex()
     *
     * Returns the current index of MX in Round Robin queue
     *
     * @return
     */
    public int getMxIndex() {
        return mxIndex;
    }

    /**
     * AdvancedSender.isIpv6Used()
     *
     * Returns if the last connection was open on an IPv6 socket or not
     *
     * @return
     */
    public boolean isIpv6Used() {
        return ipv6Used;
    }

    /**
     * AdvancedSender.setIpIndex()
     *
     * Set the current index for IP
     *
     * @param index
     */
    public void setIpIndex(int index) {
        ipIndex = index;
    }

    /**
     * AdvancedSender.setMxIndex()
     *
     * Set the current index for MX
     *
     * @param index
     */
    public void setMxIndex(int index) {
        mxIndex = index;
    }

    /**
     *
     * AdvancedSender.setIpv6Used()
     *
     * Force to use IPv6
     *
     * @param usage
     */
    public void setIpv6Used(boolean usage) {
        ipv6Used = usage;
    }
}
