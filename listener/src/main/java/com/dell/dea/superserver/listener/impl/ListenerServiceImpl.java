package com.dell.dea.superserver.listener.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import com.dell.dea.superserver.dispatcher.DispatcherService;
import com.dell.dea.superserver.listener.ListenerService;

public final class ListenerServiceImpl implements ListenerService, Runnable {
	private static final Logger LOGGER = Logger.getLogger(ListenerServiceImpl.class.getName());

	private DispatcherService dispatcher;
	private ExecutorService svc;
	private ExecutorService threadPool;
	private ServerSocketFactory ssf;
	private ServerSocket ss;
	private volatile boolean listening;
	private int timeout;
	private int port;

	public void start() throws IOException {
		assert threadPool != null;

		assert ssf != null;

		assert port != 0;
		assert port > 1024;

		LOGGER.info("Starting");
		ss = ssf.createServerSocket(port);
		svc = Executors.newSingleThreadExecutor();
		svc.execute(this);
		LOGGER.info("Started");
	}

	public void run() {
		LOGGER.info("Listening for connections");
		listening = true;
		try {
			ss.setSoTimeout(timeout);
			while (listening) {
				assert threadPool != null;
				assert dispatcher != null;
				assert ss != null;

				try {
					threadPool.execute(dispatcher.newHandler(ss.accept()));
				} catch (SocketTimeoutException e) {
					if (Thread.interrupted())
						break;
				}
			}
		} catch (RejectedExecutionException e) {
			LOGGER.severe(e.getLocalizedMessage());
		} catch (SocketException e) {
			LOGGER.severe(e.getLocalizedMessage());
		} catch (IOException e) {
			LOGGER.severe(e.getLocalizedMessage());
		} finally {
			closeServerSocket(ss);
			LOGGER.info("No longer listening for connections");
			listening = false;
		}
	}

	public void stop() {
		LOGGER.info("Stopping");
		stopExecutorService(svc, 1);
		stopExecutorService(threadPool, 5);
		closeServerSocket(ss);
		LOGGER.info("Stopped");
	}

	public boolean isListening() {
		return listening;
	}

	public void bindDispatcher(DispatcherService dispatcher, Map properties) throws IOException {
		if (svc == null) {
			LOGGER.info("Binding dispatcher");
			this.dispatcher = dispatcher;
		} else {
			LOGGER.info("Rebinding dispatcher");
			if (this.dispatcher != null)
				unbindDispatcher(this.dispatcher, properties);
			this.dispatcher = dispatcher;
			ss = ssf.createServerSocket(port);
			svc = Executors.newSingleThreadExecutor();
			svc.execute(this);
		}
		LOGGER.info("Dispatcher bound");
	}

	public void unbindDispatcher(DispatcherService dispatcher, Map properties) {
		LOGGER.info("Unbinding dispatcher");
		if (svc != null)
			stopExecutorService(svc, 1);
		this.dispatcher = null;
		LOGGER.info("Dispatcher unbound");
	}

	public void update(Map<String, ?> props) throws IOException {
		boolean update = false;

		if (props.containsKey("listenerport")) {
			int p = Integer.parseInt((String) props.get("listenerport"));
			update = true;
			setPort(p);
		}

		if (props.containsKey("accepttimeout")) {
			int t = Integer.parseInt((String) props.get("accepttimeout"));
			update = true;
			setTimeout(t);
		}

		if (update) {
			if (svc != null)
				stopExecutorService(svc, 1);
			ss = ssf.createServerSocket(port);
			svc = Executors.newSingleThreadExecutor();
			svc.execute(this);
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setServerSocketFactory(ServerSocketFactory ssf) {
		this.ssf = ssf;
	}

	public void setThreadPool(ExecutorService tp) {
		this.threadPool = tp;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	private void closeServerSocket(ServerSocket ss) {
		if (ss != null) {
			try {
				ss.close();
			} catch (IOException e) {
				LOGGER.severe(e.getLocalizedMessage());
			} finally {
				ss = null;
			}
		}
	}

	private void stopExecutorService(ExecutorService es, int timeout) {
		LOGGER.info("Stopping executor service [" + es + "]");
		listening = false;
		es.shutdown();
		try {
			if (!es.awaitTermination(timeout, TimeUnit.SECONDS)) {
				es.shutdownNow();
				if (!es.awaitTermination(timeout, TimeUnit.SECONDS)) {
					LOGGER.severe("ExecutorService shutdown failed");
				}
			}
		} catch (InterruptedException e) {
			es.shutdownNow();
		}
		LOGGER.info("Stopped executor service [" + es + "]");
	}
}
