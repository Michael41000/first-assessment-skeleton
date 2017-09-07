package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.cooksys.assessment.model.Users;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket mySocket;
	private Users users;

	public ClientHandler(Socket mySocket, Users users) {
		super();
		this.mySocket = mySocket;
		this.users = users;
		
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(mySocket.getOutputStream()));

			while (!mySocket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				if (message.getCommand().equals("connect"))
				{
					log.info("user <{}> connected", message.getUsername());
					users.addUser(message, mySocket);
				}
				else if (message.getCommand().equals("disconnect"))
				{
					log.info("user <{}> disconnected", message.getUsername());
					users.removeUser(message);
				}
				else if (message.getCommand().equals("echo"))
				{
					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					String response = mapper.writeValueAsString(message);
					writer.write(response);
					writer.flush();
				}
				else if (message.getCommand().equals("broadcast"))
				{
					log.info("user <{}> broadcast message <{}>", message.getUsername(), message.getContents());
					users.broadcastMessage(message);
				}
				else if (message.getCommand().startsWith("@"))
				{
					log.info("user <{}> @ message <{}> to <{}>", message.getUsername(), message.getContents(), message.getCommand().substring(1));
					users.directMessage(message);
				}
				else if (message.getCommand().equals("users"))
				{
					log.info("user <{}> got list of users", message.getUsername());
					users.getUsers(message);
				}
				else if (message.getCommand().equals("help"))
				{
					log.info("user <{}> got list of commands", message.getUsername());
					String help = printHelp();
					message.setContents(help);
					String helpResponse = mapper.writeValueAsString(message);
					writer.write(helpResponse);
					writer.flush();
				}	
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}
	
	public String printHelp()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("\n  Commands:\n\n");
		String padding = "                              ";
		for (Map.Entry<String, String> entry : Server.commands.entrySet())
		{
			StringBuilder command = new StringBuilder();
			command.append("    ");
			command.append(entry.getKey());
			command.append(padding);
			builder.append(command.toString().substring(0, 30));
			builder.append("\t");
			builder.append(entry.getValue());
			builder.append("\n");
		}
		String help = builder.toString();
		return help;
	}

}
