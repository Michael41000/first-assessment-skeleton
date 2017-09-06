package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

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
					// Nobody with duplicate username
					if (users.addUser(message.getUsername(), mySocket) == true)
					{
						log.info("user <{}> connected", message.getUsername());
						message.setContents("ALERT: " + message.getUsername() + " has connected");
						users.broadcastMessage(message);
					}
					else
					{
						log.info("user <{}> denied, duplicate username", message.getUsername());
						message.setContents("Denied due to duplicate username.");
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						this.mySocket.close();
					}
				}
				else if (message.getCommand().equals("disconnect"))
				{
					log.info("user <{}> disconnected", message.getUsername());
					message.setContents("ALERT: " + message.getUsername() + " has disconnected");
					users.broadcastMessage(message);
					this.mySocket.close();
					users.removeUser(message.getUsername());
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
					log.info("users <{}> got list of users", message.getUsername());
					users.getUsers(message);
				}
				
						
				
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
