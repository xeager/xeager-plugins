package com.xeager.platform.cache.impls;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalCache implements Serializable {
	
	private static final long serialVersionUID = -6790675915953439241L;
	
	private Map<String, SpaceCache> spaces = new ConcurrentHashMap<String, SpaceCache> ();

	public SpaceCache get (String namespace) {
		return spaces.get (namespace);
	}
	public void add (String namespace) {
		spaces.put (namespace, new SpaceCache ());
	}
	public void remove (String namespace) {
		spaces.remove (namespace);
	}
	public boolean exists (String namespace) {
		return spaces.containsKey (namespace);
	}

}
