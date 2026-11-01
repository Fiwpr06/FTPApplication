package com.fiwpr06.ftpapplication.common.model;

/**
 * Ma phan hoi giao thuc FTP chuan RFC 959
 */
public final class FtpReplyCode {
    private FtpReplyCode() {}

    // 1xx: Positive Preliminary Reply
    public static final int RESTART_MARKER_REPLY = 110;
    public static final int SERVICE_READY_MINUTES = 120;
    public static final int FILE_STATUS_OK = 150; // Opening data connection

    // 2xx: Positive Completion Reply
    public static final int COMMAND_OK = 200;
    public static final int COMMAND_SUPERFLUOUS = 202;
    public static final int SYSTEM_STATUS = 211;
    public static final int DIRECTORY_STATUS = 212;
    public static final int FILE_STATUS = 213;
    public static final int HELP_MESSAGE = 214;
    public static final int NAME_SYSTEM_TYPE = 215; // UNIX Type: L8
    public static final int SERVICE_READY = 220; // Welcome message
    public static final int SERVICE_CLOSING = 221; // Goodbye
    public static final int DATA_CONNECTION_OPEN = 225;
    public static final int CLOSING_DATA_CONNECTION = 226; // Transfer complete
    public static final int ENTERING_PASSIVE_MODE = 227; // PASV
    public static final int USER_LOGGED_IN = 230;
    public static final int FILE_ACTION_OK = 250;
    public static final int PATHNAME_CREATED = 257;

    // 3xx: Positive Intermediate Reply
    public static final int USER_NAME_OK = 331; // Need password
    public static final int NEED_ACCOUNT = 332;
    public static final int FILE_ACTION_PENDING = 350;

    // 4xx: Transient Negative Completion Reply
    public static final int SERVICE_NOT_AVAILABLE = 421;
    public static final int CANT_OPEN_DATA_CONNECTION = 425;
    public static final int CONNECTION_CLOSED = 426;
    public static final int FILE_ACTION_NOT_TAKEN = 450;
    public static final int ACTION_ABORTED_LOCAL_ERROR = 451;
    public static final int INSUFFICIENT_STORAGE = 452;

    // 5xx: Permanent Negative Completion Reply
    public static final int SYNTAX_ERROR = 500;
    public static final int SYNTAX_ERROR_PARAMETERS = 501;
    public static final int COMMAND_NOT_IMPLEMENTED = 502;
    public static final int BAD_SEQUENCE_OF_COMMANDS = 503;
    public static final int NOT_LOGGED_IN = 530;
    public static final int NEED_ACCOUNT_FOR_STORING = 532;
    public static final int FILE_NOT_FOUND_OR_NO_ACCESS = 550;
    public static final int PAGE_TYPE_UNKNOWN = 551;
    public static final int STORAGE_ALLOCATION_EXCEEDED = 552;
    public static final int FILE_NAME_NOT_ALLOWED = 553;
}
