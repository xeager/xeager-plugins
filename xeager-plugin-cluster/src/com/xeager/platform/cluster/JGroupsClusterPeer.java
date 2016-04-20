package com.xeager.platform.cluster;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import com.xeager.platform.json.JsonObject;
import com.xeager.platform.cluster.ClusterSerializer;
import com.xeager.platform.cluster.ClusterTask;
import com.xeager.platform.server.plugins.cluster.jgroups.JGroupsClusterPlugin;

public class JGroupsClusterPeer extends ReceiverAdapter implements ClusterPeer {

	private static final long serialVersionUID = -3737094109703023310L;

	private JGroupsClusterPlugin plugin;
	private JChannel channel;
	
	public JGroupsClusterPeer (JGroupsClusterPlugin plugin, String group) throws Exception {
		
		this.plugin = plugin;
		
		this.channel = new JChannel ();
		
		channel.setReceiver (this);
		channel.connect (group);

	}
	
	@Override
	public void receive (final Message message) {
		byte [] bytes = message.getBuffer ();
		
		ClusterSerializer 	serializer 	= plugin.getSerializer 	(get (bytes, 0, 12));
		ClusterTask 		task 		= plugin.getTask 		(get (bytes, 12, 12));
		
		task.execute (serializer.toObject (bytes, 24, bytes.length));
	}
	
	@Override
	public String id () {
		return channel.getName ();
	}

	@Override
	public void send (ClusterMessage message) throws ClusterException {
		try {
			channel.send (
				new Message (
					null, null, 
					message.serializer ().toBytes (message.object ())
				)
			);
		} catch (Exception e) {
			throw new ClusterException (e.getMessage (), e);
		}
	}

	@Override
	public JsonObject describe () {
		return null;
	}
	
	private String get (byte [] bytes, int start, int len) {
		byte [] serBytes = new byte [len];
		for (int i = start; i < serBytes.length; i++) {
			serBytes [i] = bytes [i];
		}
		return new String (serBytes).trim ();
	}

	@Override
	public void disconnect () {
		channel.disconnect ();
		channel.close ();
	}
	
}
