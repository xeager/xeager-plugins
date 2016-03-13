package com.xeager.platform.plugins.cluster;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.xeager.platform.Json;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;

public class ClusterPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private static final Logger logger = Logger.getLogger (ClusterPlugin.class);

	private static final String Address = "address";
	private static final String Port 	= "port";
	
	interface Messages {
		byte [] Started 	= "START".getBytes ();
		byte [] Stopping 	= "STOP".getBytes ();
	}
	
	private Set<ClusterMember> members = new HashSet<ClusterMember> ();
	
	private DatagramSocket 	sender;
	private MulticastSocket receiver;
	
	private JsonObject group;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		join ();		
	}
	
	@Override
	public void kill () {
		try {
			send (Messages.Stopping);
		} catch (Exception e) {
			
		}
	}

	public JsonObject getGroup () {
		return group;
	}
	
	public void setGroup (JsonObject group) {
		this.group = group;
	}

	private void join () throws Exception {
		receiver = new MulticastSocket (Json.getInteger (group, Port, 9990));
		InetAddress address = InetAddress.getByName (Json.getString (group, Address));
		receiver.joinGroup (address);

		sender = new DatagramSocket ();
		
		send (Messages.Started);
		
		while (true) {
			onMessage ();
		}

	}
	
	private void onMessage () throws Exception {
		byte[] buffer = new byte [256];
		DatagramPacket packet = new DatagramPacket (buffer, buffer.length);
		receiver.receive (packet);
		
		String message = new String (buffer, 0, packet.getLength ());
		
		logger.info ("From: " + packet.getAddress ().getHostAddress () + ", Msg: " + message);
		
		if (Messages.Started.equals (message)) {
			members.add (new ClusterMember (packet.getAddress ().getHostAddress ()));
		} if (Messages.Stopping.equals (message)) {
			members.remove (packet.getAddress ().getHostAddress ());
		}

		logger.info ("Members " + members);

	}
	
	private void send (byte [] bytes) throws Exception {
		InetAddress address = InetAddress.getByName (Json.getString (group, Address));
		DatagramPacket packet = new DatagramPacket (
			bytes, bytes.length, address, Json.getInteger (group, Port, 9990)
		);
		sender.send (packet);
	}
	
}
