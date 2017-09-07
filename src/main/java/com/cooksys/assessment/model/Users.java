package com.cooksys.assessment.model;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Users {

	Map<String, Socket> users;
	Object lock;
	
	public Users()
	{
		users = new HashMap<String, Socket>();
	}
	
	public synchronized void addUser(Message message, Socket socket)
	{
		try {
			ObjectMapper mapper = new ObjectMapper();
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			// Nobody with duplicate username
			if (!users.containsKey(message.getUsername()))	
			{
				users.put(message.getUsername(), socket);
				broadcastMessage(message);
			}
			else
			{
				message.setContents("Denied due to duplicate username.");
				message.setError(true);
				String response = mapper.writeValueAsString(message);
				writer.write(response);
				writer.flush();
				socket.close();
			} 
		}
		catch (IOException e) {
			System.out.println("Something went wrong :/\n" + e);
		}
		
	}
	
	public synchronized void removeUser(Message message)
	{
		try {
			if (users.containsKey(message.getUsername()))
			{
				broadcastMessage(message);
				users.get(message.getUsername()).close();
				users.remove(message.getUsername());
			}
		} catch (IOException e) {
			System.out.println("Something went wrong :/\n" + e);
		}
	}
	public synchronized boolean contains(String username)
	{
		return users.containsKey(username);
	}
	
	public synchronized void broadcastMessage(Message message)
	{
		ObjectMapper mapper = new ObjectMapper();
		for (Map.Entry<String, Socket> entry : users.entrySet())
		{
			if (!message.getUsername().equals(entry.getKey()))
			{
				PrintWriter writeToUser;
				try {
					writeToUser = new PrintWriter(new OutputStreamWriter(entry.getValue().getOutputStream()));
					String broadcastMessage = mapper.writeValueAsString(message);
					writeToUser.write(broadcastMessage);
					writeToUser.flush();
				} catch (IOException e) {
					System.out.println("Something went wrong :/\n" + e);
				}
				
			}
		}
	}

	public synchronized void directMessage(Message message) {
		String username = message.getCommand().substring(1);;
		
		if (!users.containsKey(message.getCommand().substring(1)))
		{
			message.setContents("No user with username: " + username);
			message.setError(true);
			username = message.getUsername();
		}
		
		ObjectMapper mapper = new ObjectMapper();
		PrintWriter writeToUser;
		try {
			writeToUser = new PrintWriter(new OutputStreamWriter(users.get(username).getOutputStream()));
			String broadcastMessage = mapper.writeValueAsString(message);
			writeToUser.write(broadcastMessage);
			writeToUser.flush();
		} catch (IOException e) {
			System.out.println("Something went wrong :/\n" + e);
		}
	}

	public synchronized void getUsers(Message message) {
		ObjectMapper mapper = new ObjectMapper();
		PrintWriter writeToUser;
		List<String> userListNames = new ArrayList<String>();
		for (Map.Entry<String, Socket> entry : users.entrySet())
		{
			userListNames.add(entry.getKey());
		}
		Collections.sort(userListNames);
		StringBuilder builder = new StringBuilder();
		for (String s : userListNames)
		{
			builder.append("\n");
			builder.append(s);
		}
		String userListMessage = builder.toString();
		message.setContents(userListMessage);
		
		try {
			writeToUser = new PrintWriter(new OutputStreamWriter(users.get(message.getUsername()).getOutputStream()));
			String userList = mapper.writeValueAsString(message);
			writeToUser.write(userList);
			writeToUser.flush();
		} catch (IOException e) {
			System.out.println("Something went wrong :/\n" + e);
		}
		
	}


	
	
	

}
