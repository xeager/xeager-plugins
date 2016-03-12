package com.xeager.platform.plugins.cluster.tests;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastSender {
	
	public static void main (String[] args) {
		
		DatagramSocket socket = null;
		DatagramPacket outPacket = null;
		byte[] outBuf;
		final int PORT = 8888;

		try {
			socket = new DatagramSocket();
			long counter = 0;
			String msg;

			while (true) {
				msg = "This is multicast! " + counter;
				counter++;
				outBuf = msg.getBytes();

				// Send to multicast IP address and port
				InetAddress address = InetAddress.getByName("224.2.2.3");
				outPacket = new DatagramPacket(outBuf, outBuf.length, address,
						PORT);

				socket.send(outPacket);

				System.out.println("Server sends : " + msg);
				try {
					Thread.sleep(500);
				} catch (InterruptedException ie) {
				}
			}
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
}