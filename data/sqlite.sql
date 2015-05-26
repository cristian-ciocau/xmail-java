CREATE TABLE queued_mails (
    id             INTEGER     PRIMARY KEY,
    mail_from      STRING      NOT NULL,
    mail_to        STRING      NOT NULL,
    email_path     STRING      NOT NULL,
    date_added     DATETIME    NOT NULL,
    date_processed DATETIME    NOT NULL,
    status         INTEGER     NOT NULL
                               DEFAULT (0),
    retry          INTEGER (1) NOT NULL
                               DEFAULT (0),
    mx_ctr         INTEGER (4) NOT NULL
                               DEFAULT (0),
    ip_ctr         INTEGER (4) NOT NULL
                               DEFAULT (0),
    bind_ipv4      STRING (15) NOT NULL
                               DEFAULT (''),
    bind_ipv6      STRING (39) NOT NULL
                               DEFAULT (''),
    ipv6_used      INTEGER (1) DEFAULT (0) 
                               NOT NULL,
    last_code      INT (3)     NOT NULL
                               DEFAULT (0),
    last_message   STRING      NOT NULL
                               DEFAULT ('') 
);