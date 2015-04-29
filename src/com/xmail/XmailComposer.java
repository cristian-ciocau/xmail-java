package com.xmail;

import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by cristian on 4/27/15.
 */
public class XmailComposer {

    String CRLF = "\r\n";

    public Map<String, String> compose(String to, String subject, String message, String headers, String[] attachments) {

        String content, html;
        Map<String, String> ret = new HashMap<String, String>();

        // Generating boundaries
        String boundary1 = Mime.generateBoundary("cristianciocau");
        String boundary2 = Mime.generateBoundary("cristianciocau");

        // Content parts start
        content = "This is a multi-part message in MIME format." + CRLF + CRLF;
        content += "--" + boundary1 + CRLF;
        content += "Content-Type: multipart/alternative;" + CRLF;
        content += " boundary=\"" + boundary2 + "\"" + CRLF + CRLF;

        // Text part headers
        content += "--" + boundary2 + CRLF;
        content += "Content-Type: text/plain;" + CRLF;
        content += " charset=\"UTF-8\"" + CRLF;
//        content += "Content-Transfer-Encoding: base64" + CRLF + CRLF;
        content += "Content-Transfer-Encoding: quoted-printable" + CRLF + CRLF;

        // Text part content
        //content += chunk_split(base64_encode(trim(makePlain(message)))) + CRLF;
//        content +=  Mime.getChunks(Mime.encodeBase64(makePlain(message))) + CRLF;
        content +=  Mime.getChunks(makePlain(message)) + CRLF;

        // HTML part headers
        content += "--" + boundary2 + CRLF;
        content += "Content-Type: text/html;" + CRLF;
        content += " charset=\"UTF-8\"" + CRLF;
//        content += "Content-Transfer-Encoding: base64" + CRLF + CRLF;
        content += "Content-Transfer-Encoding: quoted-printable" + CRLF + CRLF;

        // HTML part content
        html  = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">" + CRLF;
        html += "<html>" + CRLF;
        html += "<body>" + CRLF;
        html += message + CRLF;
        html += "</body>" + CRLF;
        html += "</html>" + CRLF;
//        content += Mime.getChunks(Mime.encodeBase64(html)) + CRLF;
        content += Mime.getChunks(html) + CRLF;

        // Content parts end
        content += "--" + boundary2 + "--" + CRLF + CRLF;

        // Add attachments if any
        if(attachments.length > 0) {

            // Loop attachments list
            for(String filePath: attachments) {

                String attachmentContent = "";

                // Check path is valid file
                File f = new File(filePath);
                if(f.exists() && f.isFile()) {
                    // Get file info
                    String ext = FilenameUtils.getExtension(filePath);
                    String name = FilenameUtils.getBaseName(filePath);
                    String type = getExtensionMime(ext);

                    // Attachment headers
                    attachmentContent += "--" + boundary1 + CRLF;
                    attachmentContent += "Content-Type: " + type + ";" + CRLF;
                    attachmentContent += " name=\"" + name + "\"" + CRLF;
                    attachmentContent += "Content-Transfer-Encoding: base64" + CRLF;
                    attachmentContent += "Content-Disposition: attachment;" + CRLF;
                    attachmentContent += " filename=\"" + name + "\"" + CRLF + CRLF;

                    // Attachment read
                    byte[] fileData = new byte[(int) f.length()];
                    try {
                        DataInputStream dis = new DataInputStream(new FileInputStream(f));
                        dis.readFully(fileData);
                        dis.close();
                    }
                    catch (IOException e) {
                        // skip this attachment
                        continue;
                    }

                    // Attachment content and encoding
                    attachmentContent += Mime.getChunks(Mime.encodeBase64(fileData)) + CRLF + CRLF;

                    // if everything is okay, add the attachment
                    content += attachmentContent;

                }
            }
        }

        content += "--" + boundary1 + "--" + CRLF + CRLF;

        // Add MIME headers
        headers = headers.trim() + CRLF;
        headers += "Subject: " + subject + CRLF;
        headers += "To: " + to + CRLF;
        headers += "Date: " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()) + CRLF;
        headers += "MIME-Version: 1.0" + CRLF;
        headers += "Content-Type: multipart/mixed;" + CRLF;
        headers += " boundary=\"" + boundary1 + "\"" + CRLF;

        ret.put("headers", headers);
        ret.put("content", content);

        return ret;
    }

    public String makePlain(String source) {

        // change new lines to \n only
        source = source.replace("\r", "");

        HashMap<String, String> searchReplace = new HashMap<String, String>();

        searchReplace.put("\\r", ""); // take out CR newlines
        searchReplace.put("\\n|\\t", " "); // take out LF newlines and tabs (\n replaced with space)
        searchReplace.put("[ ]{2,}", " "); // runs of space
        searchReplace.put("(?i)<br\\s*/?>", "\n"); // HTML line breaks
        searchReplace.put("(?i)<hr[^>]*>", "\n----------------------------------\n"); // <hr>
        searchReplace.put("(?i)(<table[^>]*>|</table>)", "\n\n"); // <table> and </table>
        searchReplace.put("(?i)(<tr[^>]*>|</tr>)", "\n"); // <tr> and </tr>
        searchReplace.put("(?i)<td[^>]*>(.*?)</td>", "\t\t$1\n"); // <td> and </td>
        searchReplace.put("(?i)&(nbsp|#160);", " "); // Non-breaking space
        searchReplace.put("(?i)&(quot|rdquo|ldquo|#8220|#8221|#147|#148);", "\""); // Double quotes
        searchReplace.put("(?i)&(apos|rsquo|lsquo|#8216|#8217);", "'"); // Simple quotes
        searchReplace.put("(?i)&gt;", ">"); // Greater than
        searchReplace.put("(?i)&lt;", "<"); // Less than
        searchReplace.put("(?i)&(amp|#38)", "&"); // Ampersand
        searchReplace.put("(?i)&(copy|#169);", "(c)"); // Copyright
        searchReplace.put("(?i)&(trade|#8482|#153);", "(tm)"); // Trade mark
        searchReplace.put("(?i)&(reg|#174);", "(r)"); // Registered mark
        searchReplace.put("(?i)&(mdash|#151|#8212);", "--"); // Mdash
        searchReplace.put("(?i)&(ndash|minus|#8211|#8722);", "-"); // Ndash
        searchReplace.put("(?i)&(bull|#149|#8226);", "*"); // Bullet
        searchReplace.put("(?i)&(pound|#163);", "GBP"); // Pound sign
        searchReplace.put("\u00A3", "GBP"); // Pound sign
        searchReplace.put("(?i)&(euro|#8364);", "EUR"); // Euro sign
        searchReplace.put("(?i)&#?[a-z0-9]+;", ""); // Unknown/unhandled entities

        for(Map.Entry<String, String> entry : searchReplace.entrySet()) {
            source = source.replaceAll(entry.getKey(), entry.getValue());
        }

        // <th> and </th>
        // ...

        // uppercase headers
        // ...

        // HTML entity decode
        // ...

        // Strip tags
        // ...

        // post strip replace
        searchReplace = new HashMap<String, String>();
        searchReplace.put("\\n\\s+\\n", "\n\n"); // fix multiple spaces and new lines
        searchReplace.put("[\\n]{3,}", "\n\n"); // fix multiple spaces and new lines
        searchReplace.put("(?m)^[ ]+", ""); // fix multiple spaces and new lines
        searchReplace.put("[ ]{2,}", ""); // fix multiple spaces and new lines
        searchReplace.put("\\t", " "); // fix tabs
        searchReplace.put("[ ]{5}", "     "); // fix 5 spaces

        for(Map.Entry<String, String> entry : searchReplace.entrySet()) {
            source = source.replaceAll(entry.getKey(), entry.getValue());
        }

        // find and add links at the end
        // ...

        // wrap and trim
        source = Mime.getChunks(source, 75, "\n");
        source = source.trim();

        // change new lines back to \r\n only
        source = source.replace("\n", "\r\n");

        return source;
    }

    private String getExtensionMime(String ext) {
        // Default return octet-stream
        String ret = "application/octet-stream";

        // Get MIME types
        String[][] mimeTypes = Mime.getMimeTypes();

        // Return MIME type for extension
        for(int i = 0; i < mimeTypes.length; i++) {
            if(mimeTypes[i][0] == ext) {
                ret = mimeTypes[i][1];
                break;
            }
        }

        return ret;
    }
}
