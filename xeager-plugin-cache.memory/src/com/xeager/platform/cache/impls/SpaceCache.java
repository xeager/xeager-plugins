package com.xeager.platform.cache.impls;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xeager.platform.cache.Cache;
import com.xeager.platform.json.JsonArray;
import com.xeager.platform.json.JsonObject;

public class SpaceCache implements Cache {

	private static final long serialVersionUID = -1719730271749501903L;
	
	private static final String Buckets 	= "buckets";
	private static final String BucketId 	= "id";

	private Map<String, Bucket> buckets = new ConcurrentHashMap<String, Bucket> ();
	
	public SpaceCache () {
	}

	@Override
	public void clear (String bucket) {
		Bucket b = buckets.get (bucket);
		if (b == null) {
			return;
		}
		b.clear ();
	}

	@Override
	public void create (String bucket, long ttl) {
		buckets.put (bucket, new Bucket (ttl));
	}

	@Override
	public void delete (String bucket, String key) {
		Bucket b = buckets.get (bucket);
		if (b == null) {
			return;
		}
		b.delete (key);
	}

	@Override
	public void drop (String bucket) {
		buckets.remove (bucket);
	}

	@Override
	public boolean exists (String bucket) {
		return buckets.containsKey (bucket);
	}

	@Override
	public JsonObject get (String bucket, int start, int page) {
		Bucket b = buckets.get (bucket);
		if (b == null) {
			return null;
		}
		return b.toJson (start, page);
	}

	@Override
	public Object get (String bucket, String key, boolean remove) {
		Bucket b = buckets.get (bucket);
		if (b == null) {
			return null;
		}
		Object v = b.get (key);
		if (remove) {
			b.delete (key);
		}
		return v;
	}

	@Override
	public void put (String bucket, String key, Object value, long ttl) {
		Bucket b = buckets.get (bucket);
		if (b == null) {
			throw new RuntimeException ("bucket '" + bucket + "' not found");
		}
		b.put (key, value, ttl);
	}
	
	@Override
	public JsonObject list () {
		JsonObject oCache = new JsonObject ();
		JsonArray aBuckets = new JsonArray ();
		oCache.set (Buckets, aBuckets);
		
		if (buckets == null || buckets.isEmpty ()) {
			return oCache;
		}
		
		for (String id : buckets.keySet ()) {
			Object oBucket = buckets.get (id).toJson ().set (BucketId, id);
			aBuckets.add (oBucket);
		}
		
		return oCache;
	}
	
}
