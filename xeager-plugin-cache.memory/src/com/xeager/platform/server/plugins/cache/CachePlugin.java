package com.xeager.platform.server.plugins.cache;

import java.util.Set;

import com.xeager.platform.api.ApiSpace;
import com.xeager.platform.cache.Cache;
import com.xeager.platform.cache.impls.CacheProxy;
import com.xeager.platform.cache.impls.MemoryCache;
import com.xeager.platform.plugins.PluginFeature;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;

public class CachePlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private Cache cache = new MemoryCache ();
	
	@Override
	public void init (final ApiServer server) throws Exception {
		server.addFeature (new PluginFeature () {
			private static final long serialVersionUID = 3585173809402444745L;
			@Override
			public Class<?> type () {
				return Cache.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return new CacheProxy (space, name, cache);
			}
			@Override
			public Set<String> providers () {
				return AbstractPlugin.PlatformProider;
			}
		});
	}

	@Override
	public void kill () {
	}
	
}
