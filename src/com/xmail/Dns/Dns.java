package com.xmail.Dns;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.NamingException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by cristian on 4/28/15.
 */
public class Dns {

    /**
     *
     * @param domainName
     * @return
     * @throws NamingException
     */
    public static String[][] getMX(String domainName) throws NamingException {

        Attribute attributeMX = getDNSRecords(domainName, "MX");

        if (attributeMX == null) {
            String[][] ret = new String[1][2];
            ret[0][0] = "0";
            ret[0][1] = domainName;
            return ret;
        }

        String[][] pvhn = new String[attributeMX.size()][2];
        for (int i = 0; i < attributeMX.size(); i++) {
            pvhn[i] = ("" + attributeMX.get(i)).split("\\s+");
        }

        Arrays.sort(pvhn, new Comparator<String[]>() {
            public int compare(String[] o1, String[] o2) {
                return (Integer.parseInt(o1[0]) - Integer.parseInt(o2[0]));
            }
        });

        String[][] sortedHostNames = new String[pvhn.length][2];
        for (int i = 0; i < pvhn.length; i++) {
            sortedHostNames[i][0] = pvhn[i][0];
            sortedHostNames[i][1] = pvhn[i][1].endsWith(".") ?
                    pvhn[i][1].substring(0, pvhn[i][1].length() - 1) : pvhn[i][1];
        }
        return sortedHostNames;
    }

    /**
     *
     * @param domainName
     * @return
     * @throws NamingException
     */
    public static String[] getA(String domainName) throws NamingException {
        Attribute attributeA = getDNSRecords(domainName, "A");

        if (attributeA == null) {
            return new String[] {""};
        }

        String[] ret = new String[attributeA.size()];
        for (int i = 0; i < attributeA.size(); i++) {
            ret[i] = "" + attributeA.get(i);
        }
        return ret;
    }

    /**
     *
     * @param domainName
     * @return
     * @throws NamingException
     */
    public static String[] getAAAA(String domainName) throws NamingException {
        Attribute attributeA = getDNSRecords(domainName, "AAAA");

        if (attributeA == null) {
            return new String[] {""};
        }

        String[] ret = new String[attributeA.size()];
        for (int i = 0; i < attributeA.size(); i++) {
            ret[i] = "" + attributeA.get(i);
        }
        return ret;
    }

    /**
     *
     * @param domainName
     * @param record
     * @return
     * @throws NamingException
     */
    static Attribute getDNSRecords(String domainName, String record) throws NamingException {

        InitialDirContext iDirC = new InitialDirContext();
        Attributes attributes = iDirC.getAttributes("dns:/" + domainName, new String[] {record});
        Attribute attributeRecord = attributes.get(record);

        return attributeRecord;
    }
}
