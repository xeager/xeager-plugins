package com.xeager.platform.cache.impls;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xeager.platform.cache.Cache;
import com.xeager.platform.json.JsonObject;

public class MemoryCache implements Cache {
	
	private static final long serialVersionUID = -6790675915953439241L;
	
	private Map<String, Bucket> buckets = new ConcurrentHashMap<String, Bucket> ();

	@Override
	public void create (String bucket, long ttl) {
		Bucket b = buckets.get (bucket);
		if (b != null) {
			return;
		}
		buckets.put (bucket, new Bucket (ttl));
	}

	@Override
	public boolean exists (String bucket) {
		return buckets.get (bucket) != null;
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
		Bucket b = buckets.get (bucket);
		if (b == null) {
			return;
		}
		b.clear ();
		buckets.remove (bucket);
	}

	@Override
	public Object get (String bucket, String key, boolean remove) {
		Bucket b = buckets.get (bucket);
		if (b == null) {
			return null;
		}
		Object v = b.get (key);
		if (remove && v != null) {
			b.delete (key);
		}
		return v;
	}

	@Override
	public void put (String bucket, String key, Object value, long ttl) {
		Bucket b = buckets.get (bucket);
		if (b == null) {
			return;
		}
		b.put (key, value, ttl);
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
	public JsonObject list () {
		throw new UnsupportedOperationException ("list is not supported by the memory cache spi");
	}

	@Override
	public JsonObject get (String bucket, int start, int page) {
		Bucket b = buckets.get (bucket);
		if (b == null) {
			return null;
		}
		return b.toJson (true);
	}

}
