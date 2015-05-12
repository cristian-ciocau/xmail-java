package com.xmail.IO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by cristian on 5/1/15.
 */
public class FileUtils {
    /**
     * FileUtils.getFileContents()
     *
     * Returns the content of a file as a String.
     *
     * @param pathToFile
     * @return
     * @throws IOException
     */
    public static String getFileContents(String pathToFile) throws IOException {
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

    public static boolean deleteFile(String pathToFile) {
        File file = new File(pathToFile);
        return file.delete();
    }
}
