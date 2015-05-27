**xmail-java** - Java 1.8 MSA (Mail Submission Agent) library.

# Description
This small project aims to demonstrate how to properly deliver emails according to RFC standards and adaptive enough 
to accommodate non standard MTA configurations.

# How it works
1. The Composer class is used to compose and add an email to the queue.

2. A queue service that is contained within this library will pick any emails in the queue and try to deliver them.
    1. The queue will attempt to deliver only when it has enough free slots to open a new thread
    2. If it is the first attempt to deliver an email it will choose the next available IP and a next available MX for
    delivery in order to balance the load between outbound IP's and recipient MX servers at high load.
        1. If the email was queued before, it will check what wore the errors or previous deliveries and try to adapt 
        to avoid them if possible.
        2. In case of failure it will not just try to adapt itself but also pick another MX server for the recipient 
        domain if available.

3. By default it will attempt to use **ESMTP** but with fall back to **SMTP** in case of error.
    1. In case of ESMTP and **TLS** support it will attempt to secure the connection.
    2. If a failure occurs the email will be re-queued and the next attempt will not try to **STARTTLS**.

4. If the MTA has advertised **SIZE** it will check if the email size is allowed by server.
    1. If not, it will cancel the sending and it will send the bounce message.
    2. If the email does not exceed the advertised size the MAIL command will be extended with the **SIZE** parameter 
    to announce what size of email it wants to deliver.

5. After sending, it will check the response received from server. If the email could not be delivered, it will check 
if it was a connection problem, or a temporary or permanent error from MTA. If the error was just temporary 
(connection or received from MTA), the email will be queued

6. If the limit of attempts was reached, the email will be deleted from the queue and a bounce message will be sent.

7. If there was a permanent error or no MXs were found or a connection to the MTA was not established the email will 
not be queued and the bounce message will be sent.

8. The thread will end here and it will notify the main thread about this.

9. If the **SIGTERM** or **SIGINT** will be received from the main thread, all the threads will be notified about 
this and the main process will stop after all current running jobs are gracefully finished.

# Highlights

1. [Composer Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/main/SMTP/Composer.java)
    The email composer. It takes the recipient address, subject, message, any attachments and also accepts some headers.
    
    It supports MIME format and it assumes that the message contains HTML markups. It will create also the text/plain 
    of the message for compliance.
    
    It also contains a method for creating the bounce messages which will be used to notify the sender 
    in case the email delivery failed.

2. [Sender Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/main/SMTP/Sender.java)
    The queue service.
    
    It connects to the remote MTA and tries to deliver.
    
    It defaults to ESMTP and sends EHLO and if STARTTLS advertised will also encrypt the connection.
    
    If EHLO is not supported it will default to SMTP protocol and send HELO command.
    
    It also checks the SIZE if advertised and sends it's own with the MAIL command to try to avoid any unnecessary 
    traffic if the emails is too large.
    
    This is a basic implementation to demonstrate how things should work.

3. [AdvancedSender Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/main/SMTP/AdvancedSender.java)
    This is  an extension of the Sender Class and comes with Round Robin MX cycling, ordered by their priority.
    
    We learned in time that sending the mail only on the first MX is not a good practice.
    
    When delivering large amounts of emails we needed to balance the amount of connections to a single provider.
    
    We also experienced bad recipient MX records and thus never take a rejection as final until you attempt once 
    more to another MX.

4. [IpQueue Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/main/XmailService/IpQueue.java)
    A singleton static class used to implement a pool of outgoing (binding) IP addresses.
    
    This is helpful if you have many IP addresses bound to your server and you want to use them balanced (Round Robin).
  
# IntelliJ - Before Run
1. Make sure you edited [Config.java](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/Config.java)
2. Run a Maven goal as follow: 
    1. **View** -> **Tool Windows** -> **Maven Projects**
    2. Select **Xmail Java** -> **Plugins** -> **activejdbc-instrumentation** -> **activejdbc-instrumentation:instrument**
    3. Click on **Run Maven Build** (the green play button) in order to instrument [QueuedMails](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/main/XmailService/Models/QueuedMails.java) model.
    
    This is needed each time you will edit [QueuedMails](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/main/Models/QueuedMails.java) model.
    
    If you will receive the next error: 
    ```[FATAL_ERROR] Cannot start Maven: Specified Maven home directory (Bundled (Maven 3)) does not exist. <a href="#">Configure Maven home</a>```
    you have to check your IntelliJ configuration for Maven. **File** -> **Settings** -> **Build, Execution, Deployment** ->
    **Build Tools** -> **Maven** -> Maven Home Directory.
    
    For a better understanding of Maven Goal for ActiveJDBC, please:
    * watch this video: [ActiveJDBC + IntelliJ Idea + Instrumentation](https://www.youtube.com/watch?v=OHXJXzZNKCU) or     
    
    * read the documentation: [Javalite.io instrumentation for IntelliJ](http://javalite.io/instrumentation#video-intellij-idea-instrumentation).

# How to test
1. In order to test **Xmail Java**, you need to do few edits in [Config](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/Config.java):
    1. Change the **EHLO** and email addresses according to your needs.
    ```
        public static String ehlo = "ceakki.eu";
    
        public static String bounceFrom = "postmaster@example.com";
    ```
    
    2. Change the remote SMTP port ```public static int port = 25;```. Some ISPs blocks the default 25 port. 
    Some MTA provide an alternative port for SMTP, like port 26.
    
    3. Add the bound IP addresses to your machine (if you support this):
    ```
    public static String[] outgoingIPv4 = new String[] {
        "0.0.0.0",
    };
    public static String[] outgoingIPv6 = new String[] {
        "::1",
    };
    public static boolean ipv6Enabled = false;
    ```

2. Edit next lines in [AddTestMail](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/examples/AddTestMail.java)
    ```
        String to = "to@example.com";
        String from = "from@example.com";
    ```
3. Run The com.xmail/examples/AddTestMail.java for few times.
4. Now you can start the service com.xmail/main/Main.java. 
5. You can also run the program from step (3) during the service process is running.

**Note:**
During the running process, if you will receive the next error:
```class org.javalite.activejdbc.InitException: you are trying to work with models, but no models are found. 
Maybe you have no models in project, or you did not instrument the models. 
It is expected that you have a file activejdbc_models.properties on classpath```
you have to run the Maven Goal described upper

# Dependencies
The needed external libraries are already saved in the pom.xml file, so you don't have to bother of them. 
Anyway, here is the list of them, if you are curious:
* [ActiveJDBC](http://javalite.io/activejdbc)

* [SQLite JDBC Driver by Xerial](https://bitbucket.org/xerial/sqlite-jdbc)

* [Apache log4j](http://logging.apache.org/log4j/1.2/)

* [Apache Commons IO](https://commons.apache.org/proper/commons-io/)