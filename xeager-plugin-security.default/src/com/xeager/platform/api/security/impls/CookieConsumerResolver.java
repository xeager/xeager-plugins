package com.xeager.platform.api.security.impls;

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
import com.xeager.platform.json.JsonArray;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.server.security.impls.DefaultApiConsumer;

@ApiConsumerResolverAnnotation (name = CookieConsumerResolver.MethodName)
public class CookieConsumerResolver implements ApiConsumerResolver {

	private static final long serialVersionUID = 889277317993642120L;
	
	protected static final String MethodName = "cookie";

	interface Defaults {
		JsonArray 	Cookies 		= (JsonArray)new JsonArray ().set (null, "suid");
		String 		Bucket			= "security.tokens";
	}
	
	interface Spec {
		String 	Names 		= "names";
		interface Auth {
			String 	Feature = "feature";
			String 	Bucket 	= "bucket";
		}
	}

	@Override
	public ApiConsumer resolve (Api api, ApiService service, ApiRequest request)
			throws ApiAuthenticationException {
		
		JsonObject oResolver = Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), MethodName);
		
		String cookie = (String)request.get (ApiHeaders.Cookie, Scope.Header);
		if (Lang.isNullOrEmpty (cookie)) {
			return null;
		}
		
		JsonArray cookiesNames = Json.getArray (oResolver, Spec.Names);
		if (cookiesNames == null) {
			cookiesNames = Defaults.Cookies;
		}
		if (cookiesNames.isEmpty ()) {
			return null;
		}
		
		String token = null;
		
		for (int i = 0; i < cookiesNames.count (); i++) {
			String cookieName = String.valueOf (cookiesNames.get (i));
			
			String [] cookieEntries = cookie.split (Lang.SEMICOLON);
			for (String cookieEntry : cookieEntries) {
				cookieEntry = cookieEntry.trim ();
				if (cookieEntry.startsWith (cookieName + Lang.EQUALS)) {
					token = cookieEntry.substring ((cookieName + Lang.EQUALS).length ());
				}
			}
			if (!Lang.isNullOrEmpty (token)) {
				break;
			}
			
		}

		if (Lang.isNullOrEmpty (token)) {
			return null;
		}

		ApiConsumer consumer = new DefaultApiConsumer (ApiConsumer.Type.Cookie);
		consumer.set (ApiConsumer.Fields.Token, token);
		
		return consumer;
		
	}
	
	@Override
	public ApiConsumer authorize (Api api, ApiService service, ApiRequest request, ApiConsumer consumer)
			throws ApiAuthenticationException {
		
		JsonObject auth = Json.getObject (Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), MethodName), Api.Spec.Security.Auth);
		if (auth == null || auth.isEmpty ()) {
			return consumer;
		}
		
		String 	feature = Json.getString (auth, Spec.Auth.Feature);
		String 	bucket 	= api.getNamespace () + Lang.SLASH + Json.getString (auth, Spec.Auth.Bucket, Defaults.Bucket);
		
		JsonObject oConsumer = (JsonObject)api.cache (request, feature).get (bucket, (String)consumer.get (ApiConsumer.Fields.Token), false);
		
		boolean isServiceSecure = Json.getBoolean (service.getSecurity (), ApiService.Spec.Security.Enabled, true);

		if (oConsumer == null) {
			if (isServiceSecure) {
				throw new ApiAuthenticationException ("invalid cookie");
			} else {
				return consumer;
			}
		}
		
		for (Object k : oConsumer.keySet ()) {
			consumer.set (String.valueOf (k), oConsumer.get (k));
		}
		
		consumer.set (ApiConsumer.Fields.Anonymous, false);
		
		return consumer;
	}
	
}
