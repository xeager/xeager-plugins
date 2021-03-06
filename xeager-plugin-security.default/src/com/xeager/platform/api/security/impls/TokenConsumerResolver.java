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

@ApiConsumerResolverAnnotation (name = TokenConsumerResolver.MethodName)
public class TokenConsumerResolver implements ApiConsumerResolver {

	private static final long serialVersionUID = 889277317993642120L;
	
	protected static final String MethodName = "token";

	interface Defaults {
		String 	Scheme 		= "Token";
		String 	Bucket 		= "security.tokens";
	}
	
	interface Spec {
		String 	Scheme 		= "scheme";
		interface Auth {
			String 	Feature = "feature";
			String 	Bucket 	= "bucket";
		}
	}

	public TokenConsumerResolver () {
	}
	
	@Override
	public ApiConsumer resolve (Api api, ApiService service, ApiRequest request)
			throws ApiAuthenticationException {
		
		JsonObject oResolver = Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), MethodName);
		
		String 	application 	= Json.getString 	(oResolver, Spec.Scheme, Defaults.Scheme);

		String authHeader 	= (String)request.get (ApiHeaders.Authorization, Scope.Header);
		if (Lang.isNullOrEmpty (authHeader)) {
			return null;
		}
		
		String [] pair = Lang.split (authHeader, Lang.SPACE, true);
		if (pair.length < 2) {
			return null;
		}
		
		String app 		= pair [0];
		String token 	= pair [1];

		if (!app.equals (application)) {
			return null;
		}
		
		ApiConsumer consumer = new DefaultApiConsumer (ApiConsumer.Type.Token);
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
				throw new ApiAuthenticationException ("invalid token");
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
