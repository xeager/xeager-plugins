package com.xeager.platform.server.plugins.messenger.smtp;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import com.xeager.platform.Feature;
import com.xeager.platform.Json;
import com.xeager.platform.Lang;
import com.xeager.platform.Recyclable;
import com.xeager.platform.api.ApiSpace;
import com.xeager.platform.api.impls.ApiSpaceImpl;
import com.xeager.platform.json.JsonArray;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.messaging.Messenger;
import com.xeager.platform.messenger.impls.smtp.SmtpMessenger;
import com.xeager.platform.plugins.PluginFeature;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;
import com.xeager.platform.server.ApiServer.Event;

public class SmtpMessengerPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private static final Set<String> Providers = new HashSet<String> () {
		private static final long serialVersionUID = -6219529665471192558L;
		{
			add ("smtp");
			add (ApiSpace.FeatureProviders.Platform);
		}
	};
	
	interface Spec {
		String Server 	= "server";
		
		String Auth 		= "auth";
			String User 	= "user";
			String Password = "password";
	}
	
	private JsonArray mimeTypes;
	
	private String feature;
	
	@Override
	public void init (final ApiServer server, File home) throws Exception {
		
		Feature aFeature = Messenger.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();
		
		server.addFeature (new PluginFeature () {
			private static final long serialVersionUID = 3585173809402444745L;
			@Override
			public Class<?> type () {
				return Messenger.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return ((RecyclableMessenger)((ApiSpaceImpl)space).getRecyclable (createKey (name))).messenger ();
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
		
		if (event.equals (Event.Create)) {
			createSessions ((ApiSpace)target);
		} 
		// change event ...
	}
	
	@SuppressWarnings("unchecked")
	private void createSessions (ApiSpace space) {
		// create sessions
		JsonObject msgFeature = Json.getObject (space.getFeatures (), feature);
		if (msgFeature == null || msgFeature.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = msgFeature.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			String provider = Json.getString (msgFeature, ApiSpace.Features.Provider);
			if (!Providers.contains (provider)) {
				continue;
			}
			
			JsonObject spec = Json.getObject (msgFeature, ApiSpace.Features.Spec);
		
			if (mimeTypes != null && mimeTypes.count () > 0) {
				MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap (); 
				for (int i = 0; i < mimeTypes.count (); i++) {
					mc.addMailcap ((String)mimeTypes.get (i)); 
				}
			}
			
			JsonObject oServer = Json.getObject (spec, Spec.Server);
			if (oServer == null || oServer.isEmpty ()) {
				continue;
			}
			
			final JsonObject oAuth = Json.getObject (spec, Spec.Auth);
			if (oAuth == null || oAuth.isEmpty ()) {
				continue;
			}
			
			Properties props = new Properties ();
			props.putAll (oServer);
			
			final String user = Json.getString (oAuth, Spec.User);
			
			final Session session = Session.getInstance (
				props,
				new Authenticator () {
					protected PasswordAuthentication getPasswordAuthentication () {
						return new PasswordAuthentication (user, Json.getString (oAuth, Spec.Password));
					}
				}
			);
			
			((ApiSpaceImpl)space).addRecyclable (createKey (key), new RecyclableMessenger (new SmtpMessenger (user, session)));
		}
		
	}
	
	private String createKey (String name) {
		return feature + Lang.DOT + name;
	}

	@Override
	public void kill () {
	}

	public JsonArray getMimeTypes () {
		return mimeTypes;
	}
	public void setMimeTypes (JsonArray mimeTypes) {
		this.mimeTypes = mimeTypes;
	}
	
	class RecyclableMessenger implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private SmtpMessenger messenger;
		
		public RecyclableMessenger (SmtpMessenger messenger) {
			this.messenger = messenger;
		}
		
		@Override
		public void recycle () {
			// nothing
		}

		public SmtpMessenger messenger () {
			return messenger;
		}
		
	}
	
}
