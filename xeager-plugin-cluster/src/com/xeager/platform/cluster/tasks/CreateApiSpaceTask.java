package com.xeager.platform.cluster.tasks;

import com.xeager.platform.cluster.ClusterTask;

public class CreateApiSpaceTask implements ClusterTask {

	@Override
	public void execute (Object o) {

	}

	@Override
	public String name () {
		return CreateApiSpaceTask.class.getSimpleName ();
	}

}
