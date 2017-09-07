package com.cooksys.assessment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.server.Server;

public class Main {
	private static Logger log = LoggerFactory.getLogger(Main.class);
	public static Map<String, String> commands = fillCommands();

	public static void main(String[] args) {
		ExecutorService executor = Executors.newCachedThreadPool();
		
		Server server = new Server(8080, executor);
		
		Future<?> done = executor.submit(server);
		
		try {
			done.get();
			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException e) {
			log.error("Something went wrong :/", e);
		}
	}
	
	public static Map<String, String> fillCommands()
	{
		Map<String, String> commandMap = new LinkedHashMap<>();
		commandMap.put("disconnect", "disconnect from server");
		commandMap.put("users", "get list of users connected to server");
		commandMap.put("echo <message>", "repeat message back");
		commandMap.put("broadcast <message>", "send message to all users");
		commandMap.put("@<username> <message>", "send a message directly to a user");
		commandMap.put("<message>", "send message with previously used message command");
		commandMap.put("help", "print all commands");
		return commandMap;
		
	}
	
}
