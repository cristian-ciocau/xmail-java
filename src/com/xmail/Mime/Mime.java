package com.xmail.Mime;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Created by cristian on 4/27/15.
 */
public class Mime {
    private static int boundaryCount = 0;

    private static String CRLF = "\r\n";

    private static String[][] mimeTypes = new String[][]{
            {"ez", "application/andrew-inset"},
            {"hqx", "application/mac-binhex40"},
            {"cpt", "application/mac-compactpro"},
            {"doc", "application/msword"},
            {"bin", "application/octet-stream"},
            {"dms", "application/octet-stream"},
            {"lha", "application/octet-stream"},
            {"lzh", "application/octet-stream"},
            {"exe", "application/octet-stream"},
            {"class", "application/octet-stream"},
            {"so", "application/octet-stream"},
            {"dll", "application/octet-stream"},
            {"oda", "application/oda"},
            {"pdf", "application/pdf"},
            {"ai", "application/postscript"},
            {"eps", "application/postscript"},
            {"ps", "application/postscript"},
            {"smi", "application/smil"},
            {"smil", "application/smil"},
            {"wbxml", "application/vnd.wap.wbxml"},
            {"wmlc", "application/vnd.wap.wmlc"},
            {"wmlsc", "application/vnd.wap.wmlscriptc"},
            {"bcpio", "application/x-bcpio"},
            {"vcd", "application/x-cdlink"},
            {"pgn", "application/x-chess-pgn"},
            {"cpio", "application/x-cpio"},
            {"csh", "application/x-csh"},
            {"dcr", "application/x-director"},
            {"dir", "application/x-director"},
            {"dxr", "application/x-director"},
            {"dvi", "application/x-dvi"},
            {"spl", "application/x-futuresplash"},
            {"gtar", "application/x-gtar"},
            {"hdf", "application/x-hdf"},
            {"js", "application/x-javascript"},
            {"skp", "application/x-koan"},
            {"skd", "application/x-koan"},
            {"skt", "application/x-koan"},
            {"skm", "application/x-koan"},
            {"latex", "application/x-latex"},
            {"nc", "application/x-netcdf"},
            {"cdf", "application/x-netcdf"},
            {"sh", "application/x-sh"},
            {"shar", "application/x-shar"},
            {"swf", "application/x-shockwave-flash"},
            {"sit", "application/x-stuffit"},
            {"sv4cpio", "application/x-sv4cpio"},
            {"sv4crc", "application/x-sv4crc"},
            {"tar", "application/x-tar"},
            {"tcl", "application/x-tcl"},
            {"tex", "application/x-tex"},
            {"texinfo", "application/x-texinfo"},
            {"texi", "application/x-texinfo"},
            {"t", "application/x-troff"},
            {"tr", "application/x-troff"},
            {"roff", "application/x-troff"},
            {"man", "application/x-troff-man"},
            {"me", "application/x-troff-me"},
            {"ms", "application/x-troff-ms"},
            {"ustar", "application/x-ustar"},
            {"src", "application/x-wais-source"},
            {"xhtml", "application/xhtml+xml"},
            {"xht", "application/xhtml+xml"},
            {"zip", "application/zip"},
            {"au", "audio/basic"},
            {"snd", "audio/basic"},
            {"mid", "audio/midi"},
            {"midi", "audio/midi"},
            {"kar", "audio/midi"},
            {"mpga", "audio/mpeg"},
            {"mp2", "audio/mpeg"},
            {"mp3", "audio/mpeg"},
            {"aif", "audio/x-aiff"},
            {"aiff", "audio/x-aiff"},
            {"aifc", "audio/x-aiff"},
            {"m3u", "audio/x-mpegurl"},
            {"ram", "audio/x-pn-realaudio"},
            {"rm", "audio/x-pn-realaudio"},
            {"rpm", "audio/x-pn-realaudio-plugin"},
            {"ra", "audio/x-realaudio"},
            {"wav", "audio/x-wav"},
            {"pdb", "chemical/x-pdb"},
            {"xyz", "chemical/x-xyz"},
            {"bmp", "image/bmp"},
            {"gif", "image/gif"},
            {"ief", "image/ief"},
            {"jpeg", "image/jpeg"},
            {"jpg", "image/jpeg"},
            {"jpe", "image/jpeg"},
            {"png", "image/png"},
            {"tiff", "image/tiff"},
            {"tif", "image/tif"},
            {"djvu", "image/vnd.djvu"},
            {"djv", "image/vnd.djvu"},
            {"wbmp", "image/vnd.wap.wbmp"},
            {"ras", "image/x-cmu-raster"},
            {"pnm", "image/x-portable-anymap"},
            {"pbm", "image/x-portable-bitmap"},
            {"pgm", "image/x-portable-graymap"},
            {"ppm", "image/x-portable-pixmap"},
            {"rgb", "image/x-rgb"},
            {"xbm", "image/x-xbitmap"},
            {"xpm", "image/x-xpixmap"},
            {"xwd", "image/x-windowdump"},
            {"igs", "model/iges"},
            {"iges", "model/iges"},
            {"msh", "model/mesh"},
            {"mesh", "model/mesh"},
            {"silo", "model/mesh"},
            {"wrl", "model/vrml"},
            {"vrml", "model/vrml"},
            {"css", "text/css"},
            {"html", "text/html"},
            {"htm", "text/html"},
            {"asc", "text/plain"},
            {"txt", "text/plain"},
            {"rtx", "text/richtext"},
            {"rtf", "text/rtf"},
            {"sgml", "text/sgml"},
            {"sgm", "text/sgml"},
            {"tsv", "text/tab-seperated-values"},
            {"wml", "text/vnd.wap.wml"},
            {"wmls", "text/vnd.wap.wmlscript"},
            {"etx", "text/x-setext"},
            {"xml", "text/xml"},
            {"xsl", "text/xml"},
            {"mpeg", "video/mpeg"},
            {"mpg", "video/mpeg"},
            {"mpe", "video/mpeg"},
            {"qt", "video/quicktime"},
            {"mov", "video/quicktime"},
            {"mxu", "video/vnd.mpegurl"},
            {"avi", "video/x-msvideo"},
            {"movie", "video/x-sgi-movie"},
            {"ice", "x-conference-xcooltalk"}

    };

    /**
     * Mime.generateBoundary()
     *
     * Generates boundary
     *
     * @param text
     * @return
     */
    public static String generateBoundary(String text) {
        return text + Integer.toString(++boundaryCount);
    }

    /**
     * Mime.encodeBase64()
     *
     * Encodes the given string to base64
     *
     * @param str
     * @return
     */
    public static String encodeBase64(String str) {
        final byte[] authBytes = str.getBytes(StandardCharsets.UTF_8);
        final String encoded = Base64.getEncoder().encodeToString(authBytes);

        return encoded;
    }

    /**
     * Mime.encodeBase64()
     *
     * Encodes the given string to base64
     *
     * @param data
     * @return
     */
    public static String encodeBase64(byte[] data) {
        final String encoded = Base64.getEncoder().encodeToString(data);

        return encoded;
    }

    /**
     * Mime.getChunks()
     *
     * Encodes the given string to base64
     *
     * @param str
     * @param chunkLength
     * @param end
     * @return
     */
    public static String getChunks(String str, int chunkLength, String end) {
        String[] chunks = str.split("(?<=\\G.{" + chunkLength + "})");

        return String.join(end, chunks);
    }

    /**
     * Mime.getChunks()
     *
     * Encodes the given string to base64
     *
     * @param str
     * @param chunkLength
     * @return
     */
    public static String getChunks(String str, int chunkLength) {
        return getChunks(str, chunkLength, "\r\n");
    }

    /**
     * Mime.getChunks()
     *
     * Encodes the given string to base64
     *
     * @param str
     * @return
     */
    public static String getChunks(String str) {
        return getChunks(str, 75, "\r\n");
    }

    /**
     * Mime.getMimeTypes()
     *
     * Return all Mime Types
     *
     * @return
     */
    public static String[][] getMimeTypes() {
        return mimeTypes;
    }

    /**
     * Mime.composeDeliveryStatus()
     *
     * Compose the Delivery-Status attachment (RFC 3464)
     *
     * @param originalTo
     * @param reportingMta
     * @param remoteMta
     * @param lastCode
     * @param lastMessage
     * @return
     */
    public static String composeDelivryStatus(String originalTo, String reportingMta, String remoteMta, int lastCode,
                                              String lastMessage) {

        String content = "";

        // We are sending bounces just for failures
        // RFC 3464
        String action = "failed";

        // We are sending for now just a permanent failure status with "Other undefined Status"
        // RFC 3463
        String status = "5.0.0";

        content += "Reporting-MTA: dns; " + reportingMta + CRLF + CRLF;

        content += "Final-Recipient: rfc822; " + originalTo + CRLF;
        content += "Action: " + action + CRLF;
        content += "Status: " + status + CRLF;

        // If the sender reached the remote MTA
        if(lastCode < 200) {
            // We support just the dns type (http://tools.ietf.org/html/rfc3464#section-2.1.2)
            String remoteMtaType = "dns";
            content += "Remote-MTA: " + remoteMtaType + "; " + remoteMta + CRLF;
        }

        // We support just the smtp type
        String diagnosticType = "smtp";

        // And the dns type if we had error in connection
        if(lastCode < 200) {
            diagnosticType = "dns";
        }
        content += "Diagnostic-Code: " + diagnosticType + "; " + lastMessage;

        return content;
    }

    /**
     * Mime.composeHumanReport()
     *
     * Compose the bounce message for humans
     *
     * @param originalTo
     * @param lastMessage
     * @return
     */
    public static String composeHumanReport(String originalTo, String lastMessage) {
        String humanReport= "The following message to <" + originalTo + "> was undeliverable." + CRLF;
        humanReport += "The reason for the problem:" + CRLF;
        humanReport += lastMessage;

        return humanReport;
    }
}

