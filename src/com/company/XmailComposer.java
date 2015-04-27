package com.company;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by cristian on 4/27/15.
 */
public class XmailComposer {

    String CRLF = "\r\n";

    public Map<String, String> compose(String to, String subject, String message, String headers) {

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
        content += "Content-Transfer-Encoding: base64" + CRLF + CRLF;

        // Text part content
        //content += chunk_split(base64_encode(trim(makePlain(message)))) + CRLF;
        content +=  Mime.getChunks(Mime.encodeBase64(makePlain(message))) + CRLF;

        // HTML part headers
        content += "--" + boundary2 + CRLF;
        content += "Content-Type: text/html;" + CRLF;
        content += " charset=\"UTF-8\"" + CRLF;
        content += "Content-Transfer-Encoding: base64" + CRLF + CRLF;

        // HTML part content
        html  = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">" + CRLF;
        html += "<html>" + CRLF;
        html += "<body>" + CRLF;
        html += message + CRLF;
        html += "</body>" + CRLF;
        html += "</html>" + CRLF;
        content += Mime.getChunks(Mime.encodeBase64(html)) + CRLF;

        // Content parts end
        content += "--" + boundary2 + "--" + CRLF + CRLF;

        // TO DO: Attachments

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

    public String makePlain(String str) {
        return str.trim();
    }
}
