package com.xmail.IO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by cristian on 5/1/15.
 */
public class FileUtils {
    public static String getFileContent(String pathToFile) throws IOException {
        BufferedReader br = null;
        String content = "";

        try {

            String sCurrentLine;

            br = new BufferedReader(new FileReader(pathToFile));

            while ((sCurrentLine = br.readLine()) != null) {
                content += sCurrentLine + "\r\n";
            }

        } finally {
            if (br != null)br.close();
        }

        return content;
    }
}
