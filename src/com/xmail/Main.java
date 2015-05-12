package com.xmail;

import com.xmail.XmailService.XmailService;
import org.apache.log4j.PropertyConfigurator;

public class Main {

    public static void main(String[] args) {

        PropertyConfigurator.configure("classes/log4j.properties");

        XmailService xmail = new XmailService();
        xmail.start();
    }
}
