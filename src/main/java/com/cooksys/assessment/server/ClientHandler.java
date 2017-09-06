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

				switch (message.getCommand()) {
					case "connect":
						// Nobody with duplicate username
						if (users.addUser(message.getUsername(), mySocket) == true)
						{
							log.info("user <{}> connected", message.getUsername());
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
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						this.mySocket.close();
						users.removeUser(message.getUsername());
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						
						String response = mapper.writeValueAsString(message);
						System.out.println("\n\t" + response);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast":
						log.info("user <{}> broadcast message <{}>", message.getUsername(), message.getContents());
						users.broadcastMessage(message);
						break;
						
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
