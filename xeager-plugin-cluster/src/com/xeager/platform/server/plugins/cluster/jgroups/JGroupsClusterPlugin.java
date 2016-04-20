package com.xeager.platform.server.plugins.cluster.jgroups;

import com.xeager.platform.cluster.ClusterSerializer;
import com.xeager.platform.cluster.ClusterTask;
import com.xeager.platform.cluster.serializers.ApiSpaceSerializer;
import com.xeager.platform.cluster.tasks.CreateApiSpaceTask;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;

public class JGroupsClusterPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private ApiServer server;
	
	private String cluster = "xeager-cluster";
	
	@Override
	public void init (final ApiServer server) throws Exception {
		this.server = server;
	
		// serializers
		this.server.registerSerializer (new ApiSpaceSerializer ());
		
		// tasks
		this.server.registerTask (new CreateApiSpaceTask ());
	}
	
	@Override
	public void kill () {
		server.getPeer ().disconnect ();
	}
	
	public ClusterSerializer getSerializer (String name) {
		return server.getSerializer (name);
	}
	
	public ClusterTask getTask (String name) {
		return server.getTask (name);
	}

	public String getCluster () {
		return cluster;
	}

	public void setCluster (String cluster) {
		this.cluster = cluster;
	}
	 
}
