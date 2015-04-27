package com.company;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Created by cristian on 4/27/15.
 */
public class Mime {
    private static int boundaryCount = 0;

    private static String CRLF = "\r\n";

    public static String generateBoundary(String text) {
        return text + Integer.toString(++boundaryCount);
    }

    public static String encodeBase64(String str) {
        final byte[] authBytes = str.getBytes(StandardCharsets.UTF_8);
        final String encoded = Base64.getEncoder().encodeToString(authBytes);

        return encoded;
    }

    public static String getChunks(String str, int chunkLength, String end) {
        String[] chunks = str.split("(?<=\\G.{" + chunkLength + "})");

        return String.join(end, chunks);
    }

    public static String getChunks(String str, int chunkLength) {
        return getChunks(str, chunkLength, "\r\n");
    }

    public static String getChunks(String str) {
        return getChunks(str, 76, "\r\n");
    }
}
