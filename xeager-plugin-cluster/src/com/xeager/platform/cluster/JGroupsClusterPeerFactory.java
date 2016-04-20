package com.xeager.platform.cluster;

import com.xeager.platform.server.plugins.cluster.jgroups.JGroupsClusterPlugin;

public class JGroupsClusterPeerFactory implements ClusterPeerFactory {

	private static final long serialVersionUID = -3737094109703023310L;

	private JGroupsClusterPlugin plugin;
	
	public JGroupsClusterPeerFactory (JGroupsClusterPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public ClusterPeer create () throws ClusterException {
		try {
			return new JGroupsClusterPeer (plugin, plugin.getCluster ());
		} catch (Exception e) {
			throw new ClusterException (e.getMessage (), e);
		}
	}


}
