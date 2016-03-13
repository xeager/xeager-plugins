package com.xeager.platform.server.plugins.search;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.util.Set;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.common.logging.log4j.LogConfigurator;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;

import com.xeager.platform.Json;
import com.xeager.platform.Lang;
import com.xeager.platform.api.ApiSpace;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.plugins.PluginFeature;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.search.SearchIndex;
import com.xeager.platform.search.impls.ElasticSearchIndex;
import com.xeager.platform.server.ApiServer;
import com.xeager.platform.server.ApiServer.Event;

public class SearchPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private static final String Root = "root";

	interface Folders {
		String Data = "data";
		String Logs = "logs";
		String Work = "work";
	}
	
	private static final String Node = "node.";
	private static final String Path = "path.";
	
	private static final String IsClient = Node + "client";
	
	private static final String PathConf 	= Path + "conf";

	private static final String PathData 	= Path + "data";
	private static final String PathLogs 	= Path + "logs";
	private static final String PathWork 	= Path + "work";
	
	private File 		root;
	
	private JsonObject 	config;
	
	private Node 		node = null;
	
	private boolean 	isClient;
	
	@SuppressWarnings("unchecked")
	@Override
	public void init (final ApiServer server) throws Exception {
		
		config.set (PathConf, home.getAbsolutePath ());

		isClient = Json.getBoolean (config, IsClient, false);
		
		String path = Json.getString (config, Root);
		
		if (Lang.isNullOrEmpty (path)) {
			root = new File (new File (System.getProperty ("user.home")), "xeager/search");
		} else {
			root = new File (path);
		}
		
		if (!root.exists ()) {
			root.mkdirs ();
		}
		if (!isClient) {
			File data = new File (root, Folders.Data);
			if (!data.exists ()) {
				data.mkdirs ();
			}
			// path.data
			config.set (PathData, data.getAbsolutePath ());
		}
		
		File logs = new File (root, Folders.Logs);
		if (!logs.exists ()) {
			logs.mkdirs ();
		}
		File work = new File (root, Folders.Work);
		if (!work.exists ()) {
			work.mkdirs ();
		}
		// path.logs, path.work
		config.set (PathLogs, logs.getAbsolutePath ());
		config.set (PathWork, work.getAbsolutePath ());
		
		// create node
		ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder ();
		if (config != null) {
			builder.put (config);
		}
		
		LogConfigurator.configure (builder.build ());
		
		node = nodeBuilder ().settings (builder).client (isClient).node ();
		
		if (!isClient) {
			node.start ();
		}
		
		server.addFeature (new PluginFeature () {
			private static final long serialVersionUID = 3585173809402444745L;
			@Override
			public Class<?> type () {
				return SearchIndex.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return new ElasticSearchIndex (node.client (), space.getNamespace ());
			}
			@Override
			public Set<String> providers () {
				return AbstractPlugin.PlatformProider;
			}
		});
	}

	@Override
	public void onEvent (Event event, Object target) {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		if (event.equals (Event.Create)) {
			String index = ((ApiSpace)target).getNamespace ();
			boolean indexExists = node.client ().admin ().indices ().prepareExists (index).execute ().actionGet ().isExists ();
			if (!indexExists) {
				node.client ().admin ().indices ().create (new CreateIndexRequest (index)).actionGet ();
			}
		} else if (event.equals (Event.Destroy)) {
			//drop index 
		} 
	}
	
	@Override
	public void kill () {
		if (node == null) {
			return;
		}
		node.close ();
	}
	
	public void setConfig (JsonObject config) {
		this.config = config;
	}
	public JsonObject getConfig () {
		return config;
	}

}
