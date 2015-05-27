package com.xmail.examples;

import com.xmail.Config;
import com.xmail.main.IO.FileUtils;

import java.io.File;

/**
 * Created by cristian on 5/26/15.
 */
public class DeleteAllMails {
    public static void main(String[] args) {

        // Remove the database
        FileUtils.deleteFile(Config.dbPath);

        // Get all emails from disk
        File folder = new File(Config.mailPath);
        File[] listOfFiles = folder.listFiles();

        // and remove them
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                FileUtils.deleteFile(listOfFiles[i]);
            }
        }

        System.out.println("All email queue has been erased.");
    }
}
