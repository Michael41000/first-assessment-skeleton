package com.cooksys.assessment.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Users;

public class Server implements Runnable {
	private Logger log = LoggerFactory.getLogger(Server.class);
	
	
	private int port;
	private ExecutorService executor;
	
	public Server(int port, ExecutorService executor) {
		super();
		this.port = port;
		this.executor = executor;
	}

	public void run() {
		log.info("server started");
		ServerSocket ss;
		Users users;
		try {
			ss = new ServerSocket(this.port);
			users = new Users();
			while (true) {
				Socket socket = ss.accept();
				ClientHandler handler = new ClientHandler(socket, users);
				
				executor.execute(handler);
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
