package com.xeager.platform.cache.impls;

import com.xeager.platform.Lang;
import com.xeager.platform.api.ApiAccessDeniedException;
import com.xeager.platform.api.ApiSpace;
import com.xeager.platform.api.impls.ApiSpaceImpl;
import com.xeager.platform.cache.Cache;
import com.xeager.platform.json.JsonArray;
import com.xeager.platform.json.JsonObject;

public class CacheProxy implements Cache {

	private static final long serialVersionUID = 5793242779066926179L;
	
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
	public JsonArray list () throws ApiAccessDeniedException {
		if (!spaceNs.equals (ApiSpaceImpl.Spaces.Sys)) {
			throw new ApiAccessDeniedException ("Access denied");
		}
		return proxy.list ();
	}

	@Override
	public JsonObject get (String bucket, int start, int page) {
		return proxy.get (bucket, start, page);
	}
	
}
