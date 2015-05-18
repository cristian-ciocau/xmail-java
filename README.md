**xmail-java** - Java 1.8 email delivery library.

# Description
This small project aims to demonstrate how to properly deliver emails according to RFC standards and adaptive enough to accommodate non standard MTA's.

# How it works
1. The email is composed by the Composer class. It will saved on disk and added to queue.
2. The service will check the queue
3. If there are not enough number of free slots, the email will be ignored and the main thread will sleep for a while
4. Otherwise, a new thread will be started and the email will be passed for processing
5. It will check the number of delivery attempts. If it is not the first time of sending, it will use the same outgoing IP address and the same MX for MTA.
6. If the email was queued before, it will check if the last error was because of TLS. If so, it will send this time without TLS.
7. It will get all MX records and all IP addresses for that MXes and it will start to send. The MX  records it will be soterted by priority.
If one connection fails, it will try the next IP. If no more IP left for current MX, it will try the next MX
8. The sender it will send first the **EHLO** greeting. If the server doesn't support EHLO, it will use the fall back **HELO**.
9. If the server supports **TLS**, it will try to use the secured connection.
10. If the MTA supports **SIZE**, it will check if the email size is allowed by server. If not, it will cancel the sending and it will send the bounce message. Otherwise, the parameter SIZE it will be attached to MAIL command.
11. After sending, it will check the response received from server. If the email could not be delivered, it will check if it was a connection problem, or a temporary or permanent error from MTA. If the error was just temporary (connection or received from MTA), the email will be queued
12. If the limit of attempts was reached, the email will be deleted from the queue and a bounce message will be sent
13. If there was a permanent error or no MXes was found or nobody answered as MTA, the email will not be queued and the bounce message will be sent.
14. The thread will end here and it will notify the main thread about this.
15. If the **SIGTERM** or **SIGINT** will be received from the main thread, all the threads will be notified about this and the main process will stop after all current running jobs will be finished.

# Highlits

1. [Composer Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/SMTP/Composer.java)
This simply composes an email. It takes the recipient address, the subject, the message and, optionally, the attachment and the other headers. It supports **MIME** format and it assumes that the message contains HTML markups. It will create also the text/plain of the message, because now days spam policies are nicer with the emails containing the message transferred in both content types: text/plain and text/html. It also contains a method for creating the **Bounce message** which can be used to notice the sender that the email delivery failed in a SendMail service.

2. [Sender Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/SMTP/Sender.java)
The core of the sending mail process. It connects to the remote MTA and start to deal the mail transfer. It supports the TLS and fall back HELO command. It also deal with the SIZE parameter of MAIL command. This is a basic implementation, but it can be developed to support bulk email delivery on the same connection, using many RCPT commands.

3. [AdvancedSender Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/SMTP/AdvancedSender.java)
This is  an extension of the Sender Class and comes with Round Robin MX cycling, ordered by their priority. We learned during the time that sending the mail only on the first MX is not a good practice. We delivered bulk messages and we needed to balance the amount of connections for some email providers (gmail, yahoo). We also experienced bad configured MX records of our clients. Some of them used two MTAs advertised in MX records and just one of had the user for who we wanted to deliver the email.

4. [NotifyingThread](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/Threads/NotifyingThread.java) and [ThreadCompleteListener](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/Threads/ThreadCompleteListener.java) Classes
A nice method to keep the count of running threads specialized for a kind of job. This is helpful if you want to have a given number of workers which will process a queue.

5. [IpQueue Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/XmailService/IpQueue.java)
A singleton static class used to implement a pool of outgoing (binding) IP addresses. This is helpful if you have many IP addresses bound to your server and you want to use them balanced (Round Robin)

# How to test
...

# Other requirements
...
