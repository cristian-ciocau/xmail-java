**xmail-java** - Java 1.8 MDA delivery library.

# Description
This small project aims to demonstrate how to properly deliver emails according to RFC standards and adaptive enough to accommodate non standard MTA's.

# How it works
1. The Composer class is used to compose and add an email to the queue.

2. A queue service that is contained within this library will pick any emails in the queue and try to deliver them.

2.1. The queue will attempt to deliver only when it has enough free slots to open a new thread.

2.2. If it is the first attempt to deliver an email it will choose the next available IP and a next available MX for delivery in order to balance the load between outbound IP's and recipient MX servers at high load.

2.2.1. If the email was queued before, it will check what wore the errors or previous deliveries and try to adapt to avoid them if possible.

2.2.2. In case of failure it will not just try to adapt itself but also pick another MX server for the recipient domain if available.

3. By default it will attempt to use **ESMTP** but with fallback to **SMTP** in case of error.

3.1. In case of ESMTP and **TLS** support it will attempt to secure the connection.

3.1.1. If a failure occurs the email will be re-queued and the next attempt will not try to STARTTLS.

4. If the MTA has advertised **SIZE** it will check if the email size is allowed by server.

4.1. If not, it will cancel the sending and it will send the bounce message.

4.1.2. If the email does not exceed the advertised size the MAIL command will be extended with the SIZE parameter to announce what size of email it wants to deliver.

5. After sending, it will check the response received from server. If the email could not be delivered, it will check if it was a connection problem, or a temporary or permanent error from MTA. If the error was just temporary (connection or received from MTA), the email will be queued

6. If the limit of attempts was reached, the email will be deleted from the queue and a bounce message will be sent.

7. If there was a permanent error or no MX's wore found or a connection to the MTA was not established the email will not be queued and the bounce message will be sent.

8. The thread will end here and it will notify the main thread about this.

9. If the SIGTERM or SIGINT will be received from the main thread, all the threads will be notified about this and the main process will stop after all current running jobs are gracefully finished.

# Highlits

1. [Composer Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/SMTP/Composer.java)
The email composer. It takes the recipient address, subject, message, any attachments and also accepts some headers.
It supports MIME format and it assumes that the message contains HTML markups. It will create also the text/plain of the message for compliance.
It also contains a method for creating the bounce messages which will be used to notify the sender in case the email delivery failed.

2. [Sender Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/SMTP/Sender.java)
The queue service.
It connects to the remote MTA and tries to deliver.
It defaults to ESMTP and sends EHLO and if STARTTLS advertised will also encrypt the connection.
If EHLO is not supported it will default to SMTP protocol and send HELO command.
It also checks the SIZE if advertised and sends it's own with the MAIL command to try to avoid any unnecessary traffic if the emails is too large.
This is a basic implementation to demonstrate how things should work.

3. [AdvancedSender Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/SMTP/AdvancedSender.java)
This is  an extension of the Sender Class and comes with Round Robin MX cycling, ordered by their priority.
We learned in time that sending the mail only on the first MX is not a good practice.
When delivering large amounts of emails we needed to balance the amount of connections to a single provider.
We also experienced bad recipient MX records and thus never take a rejection as final until you attempt once more to another MX.

4. [NotifyingThread](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/Threads/NotifyingThread.java) and [ThreadCompleteListener](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/Threads/ThreadCompleteListener.java) Classes
A nice method to keep the count of running threads specialised for a kind of job.
This is helpful if you want to have a given number of workers which will process a queue.

5. [IpQueue Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/XmailService/IpQueue.java)
A singleton static class used to implement a pool of outgoing (binding) IP addresses.
This is helpful if you have many IP addresses bound to your server and you want to use them balanced (Round Robin).

# How to test
In order to test **xmail-java**, you need to do few edits in [XmailConfig]():
1. Change the path where the SQLite database will be saved on disk ```public static String dbPath = "/Data/git/xmail-java/data/data.db";```. You should provide a read/write access to that file.
2. Change the remote SMTP port ```public static int port = 25;```. Some ISPs blocks the default 25 port. Some MTA provide an alternative port for SMTP, like port 26. You can also try the nonstandard submission 587 port (if the MTA doesn't require authenitcation on it).
3. Add the binded IP addresses to your machine (if you support this):
```
public static String[] outgoingIPv4 = new String[] {
  "0.0.0.0",
};
public static String[] outgoingIPv6 = new String[] {
  "::1",
};
public static boolean ipv6Enabled = false;
```
4. Edit the [ComposerTest](https://github.com/tntu/xmail-java/blob/master/tests/com/tests/ComposerTest.java) file.
```
String from = "from@example.com";

String to = "to@example.com";
String subject = "Cool Test message";
String message = "<p>Hi there!!</p><p>How are you?</p><p><br/></p><p>I hope to receive well this message</p>";
String headers = "From: <from@example.com>";
String[] attachments = new String[] {};
```
5. Run The com.tests/Main.java for few times and after you can start the service com.xmail/Main.java. You can also run the tests during the service process is running.

# Other requirements
This library requires log4j and activejdbc.
