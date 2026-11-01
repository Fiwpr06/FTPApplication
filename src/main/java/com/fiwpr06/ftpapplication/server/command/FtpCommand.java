package com.fiwpr06.ftpapplication.server.command;

import com.fiwpr06.ftpapplication.server.core.FtpSessionHandler;

import java.io.IOException;

/**
 * Giao dien xu ly lenh FTP theo Command Pattern
 */
@FunctionalInterface
public interface FtpCommand {
    void execute(FtpSessionHandler session, String argument) throws IOException;
}
