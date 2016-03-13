package com.xeager.platform.plugins.database.orientdb;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.xeager.platform.Feature;
import com.xeager.platform.Json;
import com.xeager.platform.Lang;
import com.xeager.platform.Recyclable;
import com.xeager.platform.api.ApiContext;
import com.xeager.platform.api.ApiSpace;
import com.xeager.platform.api.impls.ApiSpaceImpl;
import com.xeager.platform.api.impls.DefaultApiContext;
import com.xeager.platform.cache.Cache;
import com.xeager.platform.db.Database;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.plugins.PluginFeature;
import com.xeager.platform.plugins.database.orientdb.impls.OrientDatabase;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;
import com.xeager.platform.server.ApiServer.Event;

public class OrientDatabasePlugin extends AbstractPlugin {

	private static final long serialVersionUID = -6219529665471192558L;
	
	private static final Logger logger 		= Logger.getLogger (OrientDatabasePlugin.class);
	
	private static final ApiContext ZeroApiContext = new DefaultApiContext ();

	private static final String DbPrefix 	= "db_";
	
	private static final Set<String> Providers = new HashSet<String> () {
		private static final long serialVersionUID = -6219529665471192558L;
		{
			add ("orientdb");
			add (ApiSpace.FeatureProviders.Platform);
		}
	};
	
	interface Spec {
		String Host 	= "host";
		String Port 	= "port";
		String User 	= "user";
		String Password = "password";
		
		String Database = "database";

		String Pool 	= "pool";
		String Min 		= "min";
		String Max 		= "max";
	}
	
	interface Protocol {
		String Remote	= "remote:";
		String Local 	= "plocal:";
	}
	
	private OServer 			oServer;
	private String				path;
	
	private String 				_server;
	private JsonObject 			_default;
	
	private String				feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Database.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();
		
		if (!Lang.isNullOrEmpty (_server)) {
			startServer (home);
		} 

		// add features
		server.addFeature (new PluginFeature () {
			private static final long serialVersionUID = 2626039344401539390L;
			@Override
			public Class<?> type () {
				return Database.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return new OrientDatabase (space.feature (Cache.class, ApiSpace.Features.Default, ZeroApiContext), OrientDatabasePlugin.this.acquire (space, name));
			}
			@Override
			public Set<String> providers () {
				return Providers;
			}
		});
		
	}
	
	@Override
	public void onEvent (Event event, Object target) {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		logger.info ("onEvent " + event + ", target " + target.getClass ().getSimpleName ());
		
		ApiSpace space = (ApiSpace)target;
		
		if (event.equals (Event.Create)) {
			createFactories ((ApiSpace)target);
			Cache cache = space.feature (Cache.class, ApiSpace.Features.Default, ZeroApiContext);
			if (!cache.exists (OrientDatabase.CacheQueriesBucket)) {
				cache.create (OrientDatabase.CacheQueriesBucket, 0);
			}
		} 
		// change event ...
	}
	
	private void createFactories (ApiSpace space) {
		
		// create factories
		JsonObject dbFeature = Json.getObject (space.getFeatures (), feature);
		if (dbFeature == null || dbFeature.isEmpty ()) {
			return;
		}
		
		boolean platformFactoryCreated = false;
		
		Iterator<String> keys = dbFeature.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			JsonObject source = Json.getObject (dbFeature, key);
			String provider = Json.getString (source, ApiSpace.Features.Provider);
			if (!Providers.contains (provider)) {
				continue;
			}
			
			JsonObject database = Json.getObject (source, ApiSpace.Features.Spec);
			if (ApiSpace.FeatureProviders.Platform.equals (provider)) {
				if (platformFactoryCreated) {
					continue;
				}
				database = _default;
			}
			OrientGraphFactory factory = createFactory (key, space, provider, database);
			if (ApiSpace.FeatureProviders.Platform.equals (provider)) {
				ODatabaseDocumentTx db = null;
				try {
					db = factory.getDatabase ();
					OUser user = db.getMetadata ().getSecurity ().getUser (Json.getString (database, Spec.User));
					user.setPassword (Json.getString (database, Spec.Password)).save (); 
				} finally {
					if (db != null) {
						db.close ();
					}
				}
				platformFactoryCreated = true;
			}
		}
	}
	
	private OrientGraphFactory createFactory (String name, ApiSpace space, String provider, JsonObject database) {
		
		String factoryKey = createFactoryKey  (name, space);
		
		JsonObject pool = Json.getObject (database, Spec.Pool);
		if (pool == null) {
			pool = new JsonObject ();
		}
		
		int poolMin = Json.getInteger (pool, Spec.Min, 5);
		if (poolMin < 1) {
			poolMin = 5;
		}
		
		int poolMax = Json.getInteger (pool, Spec.Max, 20);
		if (poolMax < 1) {
			poolMax = 20;
		}
		
		OrientGraphFactory factory = new OrientGraphFactory (createUrl (name, space, provider, database), Json.getString (database, Spec.User), Json.getString (database, Spec.Password));
		factory.setupPool (poolMin, poolMax);
		
		((ApiSpaceImpl)space).addRecyclable (factoryKey, new RecyclableFactory (factory));
		
		return factory;
		
	}
	
	private String createFactoryKey (String name, ApiSpace space) {
		return feature + Lang.DOT + name;
	}

	private String createUrl (String name, ApiSpace space, String provider, JsonObject database) {
		if (ApiSpace.FeatureProviders.Platform.equals (provider)) {
			String dbName = DbPrefix + space.getNamespace ();
			if (oServer != null) {
				return Protocol.Local + path + "databases" + Lang.SLASH + dbName;
			} 
			return Protocol.Remote + Json.getObject (database, Spec.Host) + Lang.COLON + Json.getObject (database, Spec.Port) + Lang.SLASH + dbName;
		} 
		return Protocol.Remote + Json.getObject (database, Spec.Host) + Lang.COLON + Json.getObject (database, Spec.Port) + Lang.SLASH + Json.getObject (database, Spec.Database);
	}
	
	@Override
	public void kill () {
		try {
			if (oServer != null) {
				oServer.shutdown ();
			}
		} catch (Throwable th) {
			// IGNORE
		}
	}

	private void startServer (final File home) throws Exception {
		File orientDbHome = new File (home, "orientdb");
		
		System.setProperty ("ORIENTDB_HOME", orientDbHome.getAbsolutePath ());
		System.setProperty ("ORIENTDB_ROOT_PASSWORD", Json.getString (_default, Spec.Password));
		System.setProperty ("orientdb.www.path", new File (orientDbHome, "www").getAbsolutePath ());
		System.setProperty ("java.util.logging.config.file", new File (orientDbHome, "config/orientdb-server-log.properties").getAbsolutePath ());
		
		path = orientDbHome.getAbsolutePath ();
		path = Lang.replace (path, "\\", Lang.SLASH);
		
		int indexOfColon = path.indexOf (Lang.COLON + Lang.SLASH);
		if (indexOfColon > 0) {
			path.substring (indexOfColon + 1);
		}
		if (!path.endsWith (Lang.SLASH)) {
			path += Lang.SLASH;
		}
		
		oServer = OServerMain.create ();
		oServer.startup (new File (orientDbHome, _server));
		oServer.activate ();
	}	

	public String getServer () {
		return _server;
	}
	public void setServer (String server) {
		this._server = server;
	}

	public JsonObject getDefault () {
		return _default;
	}
	public void setDefault (JsonObject _default) {
		this._default = _default;
	}

	public ODatabaseDocumentTx acquire (ApiSpace space, String name) {
		return ((RecyclableFactory)((ApiSpaceImpl)space).getRecyclable (createFactoryKey (name, space))).factory ().getDatabase ();
	}
	
	class RecyclableFactory implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private OrientGraphFactory factory;
		
		public RecyclableFactory (OrientGraphFactory factory) {
			this.factory = factory;
		}
		
		@Override
		public void recycle () {
			try {
				factory.close ();
			} catch (Exception ex) {
				// Ignore
			}
		}

		public OrientGraphFactory factory () {
			return factory;
		}
		
	}

}
