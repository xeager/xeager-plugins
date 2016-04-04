package com.xeager.platform.api.security.impls;

import java.util.HashMap;
import java.util.Map;

import com.xeager.platform.Json;
import com.xeager.platform.Lang;
import com.xeager.platform.api.Api;
import com.xeager.platform.api.ApiHeaders;
import com.xeager.platform.api.ApiRequest;
import com.xeager.platform.api.ApiRequest.Scope;
import com.xeager.platform.api.ApiService;
import com.xeager.platform.api.security.ApiAuthenticationException;
import com.xeager.platform.api.security.ApiConsumer;
import com.xeager.platform.api.security.ApiConsumerResolver;
import com.xeager.platform.api.security.ApiConsumerResolverAnnotation;
import com.xeager.platform.db.SchemalessEntity;
import com.xeager.platform.db.query.impls.JsonQuery;
import com.xeager.platform.encoding.Base64;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.server.security.impls.DefaultApiConsumer;

@ApiConsumerResolverAnnotation (name = TokenConsumerResolver.MethodName)
public class BasicConsumerResolver implements ApiConsumerResolver {

	private static final long serialVersionUID = 889277317993642120L;
	
	protected static final String MethodName 	= "basic";
	
	protected static final String BasicAuth 	= "Basic";
	
	interface Defaults {
		String 	LoginField = "email";
	}
	
	interface Spec {
		interface Auth {
			String Feature 		= "feature";
			String Query 		= "query";
		}
	}
	
	@Override
	public ApiConsumer resolve (Api api, ApiService service, ApiRequest request)
			throws ApiAuthenticationException {
		
		String authHeader 	= (String)request.get (ApiHeaders.Authorization, Scope.Header);
		if (Lang.isNullOrEmpty (authHeader)) {
			return null;
		}
		
		String [] pair = Lang.split (authHeader, Lang.SPACE, true);
		if (pair.length < 2) {
			return null;
		}
		
		String app 			= pair [0];
		if (!app.equals (BasicAuth)) {
			return null;
		}

		String credentials		= new String (Base64.decodeBase64 (pair [1]));
		String [] aCredentials 	= Lang.split (credentials, Lang.COLON, true);
		if (aCredentials == null || aCredentials.length < 2) {
			return null;
		}
		
		ApiConsumer consumer = new DefaultApiConsumer (ApiConsumer.Type.Basic);
		consumer.set (ApiConsumer.Fields.Id, aCredentials [0]);
		consumer.set (ApiConsumer.Fields.Password, aCredentials [1]);
		
		return consumer;
	}

	@Override
	public ApiConsumer authorize (Api api, ApiService service, ApiRequest request, ApiConsumer consumer)
			throws ApiAuthenticationException {
		
		JsonObject auth = Json.getObject (Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), MethodName), Api.Spec.Security.Auth);
		if (auth == null || auth.isEmpty ()) {
			return consumer;
		}
		
		String 		feature 	= Json.getString (auth, Spec.Auth.Feature);
		JsonObject 	query 		= Json.getObject (auth, Spec.Auth.Query);
		
		if (query == null || query.isEmpty ()) {
			return consumer;
		}
		
		Map<String, Object> bindings = new HashMap<String, Object> ();
		bindings.put (ApiConsumer.Fields.Id, consumer.get (ApiConsumer.Fields.Id));
		bindings.put (ApiConsumer.Fields.Password, consumer.get (ApiConsumer.Fields.Password));
		
		JsonQuery q = new JsonQuery (query, bindings);
		
		SchemalessEntity odb = null;
		try {
			odb = (SchemalessEntity)api.database (request, feature).findOne (null, q);
		} catch (Exception ex) {
			throw new ApiAuthenticationException (ex.getMessage (), ex);
		}
		
		boolean isServiceSecure = Json.getBoolean (service.getSecurity (), ApiService.Spec.Security.Enabled, true);
		
		if (odb == null) {
			if (isServiceSecure) {
				throw new ApiAuthenticationException ("invalid user/password");
			} else {
				return consumer;
			}
		}
		
		JsonObject oConsumer = odb.toJson ();
		
		for (Object k : oConsumer.keySet ()) {
			consumer.set (String.valueOf (k), oConsumer.get (k));
		}
		
		consumer.set (ApiConsumer.Fields.Anonymous, false);

		return consumer;
	}
	
}
