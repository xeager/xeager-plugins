package com.xeager.platform.cache.impls;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xeager.platform.json.JsonObject;

public class Bucket implements Serializable {

	private static final long serialVersionUID = -1719730271749501903L;
	
	private static final String Ttl 	= "ttl";
	private static final String Count 	= "count";
	private static final String Entries = "entries";
	
	private Map<String, Object> values = new ConcurrentHashMap<String, Object> ();
	
	private long ttl;
	
	public Bucket (long ttl) {
		this.ttl = ttl;
	}
	
	public Object get (String key) {
		return values.get (key);
	}
	
	public void put (String key, Object value, long ttl) {
		values.put (key, value);
	}
	
	public void delete (String key) {
		values.remove (key);
	}
	
	public void clear () {
		values.clear ();
	}

	public JsonObject toJson (int start, int page) {
		JsonObject o = (JsonObject)new JsonObject ().set (Ttl, ttl).set (Count, values.size ());
		if (values.isEmpty ()) {
			return o;
		}
		if (page < 1) {
			return o;
		} 
		
		JsonObject entries = new JsonObject ();
		o.set (Entries, entries);
		
		int counter = 0;
		for (String k : values.keySet ()) {
			if ((counter - start) > page) {
				break;
			}
			if (counter < start) {
				continue;
			}
			counter++;
			entries.set (k, values.get (k));
		}
		return o;
	}
	
	public JsonObject toJson () {
		return toJson (0, 0);
	}
	
}
