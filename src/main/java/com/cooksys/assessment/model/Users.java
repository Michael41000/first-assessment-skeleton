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
	
	public boolean addUser(String username, Socket socket)
	{
		if (users.containsKey(username))
		{
			return false;
		}
		else
		{
			users.put(username, socket);
			return true;
		}
	}
	
	public boolean removeUser(String username)
	{
		if (users.containsKey(username))
		{
			users.remove(username);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void broadcastMessage(Message message)
	{
		for (Map.Entry<String, Socket> entry : users.entrySet())
		{
			ObjectMapper mapper = new ObjectMapper();
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

	public void directMessage(Message message) {
		String username = message.getCommand().substring(1);;
		
		if (!users.containsKey(message.getCommand().substring(1)))
		{
			message.setContents("No user with username: " + username);
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

	public void getUsers(Message message) {
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
			builder.append(s);
			builder.append("\n");
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
