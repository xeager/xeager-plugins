package com.xeager.platform.cache.impls;

import com.xeager.platform.Lang;
import com.xeager.platform.api.ApiSpace;
import com.xeager.platform.cache.Cache;
import com.xeager.platform.json.JsonObject;

public class CacheProxy implements Cache {

	private static final long serialVersionUID = 5793242779066926179L;
	
	private static final String BucketsBucket = "__Buckets_Bucket";
	
	private Cache 	proxy;
	private String 	spaceNs;
	private String 	prefix;
	
	public CacheProxy (ApiSpace space, String name, Cache proxy) {
		this.proxy 		= proxy;
		this.spaceNs 	= space.getNamespace ();
		this.prefix 	= this.spaceNs + Lang.DOT + name + Lang.DOT;
	}

	@Override
	public void create (String bucket, long ttl) {
		proxy.create (prefix + bucket, ttl);
		JsonObject oBucketsBucket = proxy.get (BucketsBucket, 0, -1);
		if (oBucketsBucket == null) {
			create (BucketsBucket, -1);
		}
		put (BucketsBucket, bucket, 1, -1);
	}

	@Override
	public boolean exists (String bucket) {
		return proxy.exists (bucket);
	}

	@Override
	public void delete (String bucket, String key) {
		proxy.delete (prefix + bucket, key);
	}

	@Override
	public void drop (String bucket) {
		proxy.drop (prefix + bucket);
		delete (BucketsBucket, bucket);
	}

	@Override
	public Object get (String bucket, String key, boolean remove) {
		return proxy.get (prefix + bucket, key, remove);
	}

	@Override
	public void put (String bucket, String key, Object value, long ttl) {
		proxy.put (prefix + bucket, key, value, ttl);
	}

	@Override
	public void clear (String bucket) {
		proxy.clear (prefix + bucket);
	}

	@Override
	public JsonObject list () {
		return proxy.get (BucketsBucket, 0, -1);
	}

	@Override
	public JsonObject get (String bucket, int start, int page) {
		return proxy.get (bucket, start, page);
	}
	
}
