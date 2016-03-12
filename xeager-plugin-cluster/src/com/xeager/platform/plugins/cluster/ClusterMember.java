package com.xeager.platform.plugins.cluster;

import java.io.Serializable;

public class ClusterMember implements Serializable {

	private static final long serialVersionUID = -8864579092986147436L;

	private String address;
	
	public ClusterMember (String address) {
		this.address = address;
	}
	
	public String getAddress () {
		return address;
	}
	
}
