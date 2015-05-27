package com.xmail.main.IO;

import java.io.*;

/**
 * Created by cristian on 5/1/15.
 */
public class FileUtils {

    /**
     * FileUtils.getFileContents()
     *
     * Returns the content of a file as a String
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
            if (br != null) br.close();
        }

        return content;
    }

    /**
     * FileUtils.putFileContents()
     *
     * Save the content to file
     *
     * @param file accepts File class
     * @param content
     * @param append
     * @return
     */
    public static boolean putFileContents(File file, String content, boolean append) {
        try {

            if(!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), append);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        }
        catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * FileUtils.putFileContents()
     *
     * Save the content to file
     *
     * @param pathToFile accepts the path to file
     * @param content
     * @param append
     * @return
     */
    public static boolean putFileContents(String pathToFile, String content, boolean append) {

        File file = new File(pathToFile);
        return putFileContents(file, content, append);
    }

    /**
     * FileUtils.putFileContents()
     *
     * Save the content to file
     *
     * @param file
     * @param content
     * @return
     */
    public static boolean putFileContents(File file, String content) {
        return putFileContents(file, content, false);
    }

    /**
     * FileUtils.putFileContents()
     *
     * Save the content to file
     *
     * @param pathToFile
     * @param content
     * @return
     */
    public static boolean putFileContents(String pathToFile, String content) {
        return putFileContents(pathToFile, content, false);
    }

    /**
     * FileUtils.deleteFile()
     *
     * Remove a file from disk
     *
     * @param pathToFile
     * @return
     */
    public static boolean deleteFile(String pathToFile) {
        File file = new File(pathToFile);
        return deleteFile(file);
    }

    /**
     * FileUtils.deleteFile()
     *
     * Remove a file from disk
     *
     * @param file
     * @return
     */
    public static boolean deleteFile(File file) {
        return file.delete();
    }
}
