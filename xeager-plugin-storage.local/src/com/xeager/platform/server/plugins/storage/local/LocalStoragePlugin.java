package com.xeager.platform.server.plugins.storage.local;

import java.io.File;
import java.util.Set;

import com.xeager.platform.Feature;
import com.xeager.platform.Json;
import com.xeager.platform.Lang;
import com.xeager.platform.api.ApiSpace;
import com.xeager.platform.json.JsonArray;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.plugins.PluginFeature;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;
import com.xeager.platform.server.ApiServer.Event;
import com.xeager.platform.storage.Storage;
import com.xeager.platform.storage.impls.LocalStorage;

public class LocalStoragePlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;

	private String 	root;
	private File 	fRoot;
	
	private Set<String> providers;
	
	private String 	feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Storage.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();

		if (Lang.isNullOrEmpty (root)) {
			fRoot = new File (new File (System.getProperty ("user.home")), "xeager/storage");
		} else {
			fRoot = new File (root);
		}
		if (!fRoot.exists ()) {
			fRoot.mkdirs ();
		}
		
		server.addFeature (new PluginFeature () {
			private static final long serialVersionUID = -9012279234275100528L;
			
			@Override
			public Class<?> type () {
				return Storage.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return new LocalStorage (new File (fRoot, space.getNamespace ()));
			}
			@Override
			public Set<String> providers () {
				return providers;
			}
		});
	}

	public void setRoot (String root) {
		this.root = root;
	}
	public String getRoot () {
		return root;
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

	@Override
	public void kill () {
	
	}
	
	@Override
	public void onEvent (Event event, Object target) {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		ApiSpace space = (ApiSpace)target;
		
		File spaceStorage = null;
		switch (event) {
			case Create:
				spaceStorage = new File (fRoot, space.getNamespace ());
				if (!spaceStorage.exists ()) {
					spaceStorage.mkdir ();
				}
				break;
			case AddFeature:
				// if it's storage and provider is 'platform' create folder if not exists
				JsonObject sFeature = Json.getObject (space.getFeatures (), feature);
				if (sFeature == null || sFeature.isEmpty ()) {
					return;
				}
				spaceStorage = new File (fRoot, space.getNamespace ());
				if (!spaceStorage.exists ()) {
					spaceStorage.mkdir ();
				}
				break;
			case DeleteFeature:
				// NOTHING TO BE DONE. Should keep the space folder
				
				break;
			default:
				break;
		}
	}
	
}
