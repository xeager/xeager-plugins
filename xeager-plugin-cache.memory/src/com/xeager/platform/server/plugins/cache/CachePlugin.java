package com.xeager.platform.server.plugins.cache;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.xeager.platform.Feature;
import com.xeager.platform.Json;
import com.xeager.platform.Lang;
import com.xeager.platform.api.ApiSpace;
import com.xeager.platform.cache.Cache;
import com.xeager.platform.cache.impls.GlobalCache;
import com.xeager.platform.db.Database;
import com.xeager.platform.json.JsonArray;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.plugins.PluginFeature;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;
import com.xeager.platform.server.ApiServer.Event;

public class CachePlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private static final Logger logger 		= Logger.getLogger (CachePlugin.class);
	
	private GlobalCache globalCache = new GlobalCache ();
	
	private Set<String> providers = new HashSet<String> ();
	
	private String		feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		Feature aFeature = Database.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();
		
		server.addFeature (new PluginFeature () {
			private static final long serialVersionUID = 3585173809402444745L;
			@Override
			public Class<?> type () {
				return Cache.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return globalCache.get (space.getNamespace () + Lang.DOT + name);
			}
			@Override
			public Set<String> providers () {
				return providers;
			}
		});
	}

	@Override
	public void onEvent (Event event, Object target) {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		// must revisit, maybe drop and ehcache
		
		logger.info ("onEvent " + event + ", target " + target.getClass ().getSimpleName ());
		
		ApiSpace space = (ApiSpace)target;
		
		switch (event) {
			case Create:
				// create factories
				JsonObject cacheFeature = Json.getObject (space.getFeatures (), feature);
				if (cacheFeature == null || cacheFeature.isEmpty ()) {
					return;
				}
				Iterator<String> keys = cacheFeature.keys ();
				while (keys.hasNext ()) {
					String key = keys.next ();
					JsonObject source = Json.getObject (cacheFeature, key);
					String provider = Json.getString (source, ApiSpace.Features.Provider);
					if (!providers.contains (provider)) {
						continue;
					}
					globalCache.add (space.getNamespace () + Lang.DOT + key);
				}
				break;
			default:
				break;
		}
	}
	
	@Override
	public void kill () {
	}
	
	public JsonArray getProviders () {
		return null;
	}

	public void setProviders (JsonArray providers) {
		if (providers == null) {
			return;
		}
		for (Object o : providers) {
			this.providers.add (o.toString ());
		}
	}

}
