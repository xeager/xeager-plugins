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
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.server.security.impls.DefaultApiConsumer;

@ApiConsumerResolverAnnotation (name = CookieConsumerResolver.MethodName)
public class CookieConsumerResolver implements ApiConsumerResolver {

	private static final long serialVersionUID = 889277317993642120L;
	
	protected static final String MethodName = "cookie";

	interface Defaults {
		String 	Cookie 				= "suuid";
	}
	
	interface Spec {
		String 	Cookie 				= "cookie";
	}

	@Override
	public ApiConsumer resolve (Api api, ApiService service, ApiRequest request)
			throws ApiAuthenticationException {
		
		JsonObject oResolver = Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Methods), MethodName);
		
		String cookie = (String)request.get (ApiHeaders.Cookie, Scope.Header);
		if (Lang.isNullOrEmpty (cookie)) {
			return null;
		}
		
		String cookieName = Json.getString (oResolver, Spec.Cookie, Defaults.Cookie);

		String token = null;
		
		String [] cookieEntries = cookie.split (Lang.SEMICOLON);
		for (String cookieEntry : cookieEntries) {
			cookieEntry = cookieEntry.trim ();
			if (cookieEntry.startsWith (cookieName + Lang.EQUALS)) {
				token = cookieEntry.substring ((cookieName + Lang.EQUALS).length ());
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
		return consumer;
	}
	
}
