package com.dell.dea.superserver.listener;

import java.io.IOException;

public interface ListenerService {
    void start() throws IOException;
    void stop();
}

