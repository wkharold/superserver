package com.dell.dea.superserver.dispatcher;

import java.net.Socket;

public interface DispatcherService {
    Runnable newHandler(Socket socket);
}
