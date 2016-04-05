package com.xeager.platform.server.plugins.messenger.smtp;

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
	
	interface Spec {
		String Server 	= "server";
		
		String Auth 		= "auth";
			String User 	= "user";
			String Password = "password";
	}
	
	private JsonArray mimeTypes;
	
	private String feature;
	
	private Set<String> providers = new HashSet<String> ();
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
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
				return ((RecyclableMessenger)(space.getRecyclable (createKey (name)))).messenger ();
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
		
		switch (event) {
			case Create:
				createSessions ((ApiSpace)target);
				break;
			case AddFeature:
				// if it's Messenger and provider is 'smtp' create createSession
				createSessions ((ApiSpace)target);
				break;
			case DeleteFeature:
				// if it's Messenger and provider is 'smtp' shutdown session
				dropSessions ((ApiSpace)target);
				break;
			default:
				break;
		}
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
			if (!providers.contains (provider)) {
				continue;
			}
			
			String sessionKey = createKey (key);
			if (space.containsRecyclable (sessionKey)) {
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
			
			space.addRecyclable (sessionKey, new RecyclableMessenger (new SmtpMessenger (user, session)));
		}
		
	}
	
	private void dropSessions (ApiSpace space) {
		
		JsonObject msgFeature = Json.getObject (space.getFeatures (), feature);
		
		Set<String> recyclables = space.getRecyclables ();
		for (String r : recyclables) {
			if (!r.startsWith (feature + Lang.DOT)) {
				continue;
			}
			String name = r.substring ((feature + Lang.DOT).length ());
			if (msgFeature == null || msgFeature.containsKey (name)) {
				// it's deleted
				RecyclableMessenger rm = (RecyclableMessenger)space.getRecyclable (r);
				// remove from recyclables
				space.removeRecyclable (r);
				// recycle
				rm.recycle ();
			}
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
