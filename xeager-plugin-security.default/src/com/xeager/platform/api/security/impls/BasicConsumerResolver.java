package com.xeager.platform.api.security.impls;

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
import com.xeager.platform.encoding.Base64;
import com.xeager.platform.server.security.impls.DefaultApiConsumer;

@ApiConsumerResolverAnnotation (name = TokenConsumerResolver.MethodName)
public class BasicConsumerResolver implements ApiConsumerResolver {

	private static final long serialVersionUID = 889277317993642120L;
	
	protected static final String MethodName 	= "basic";
	
	protected static final String BasicAuth 	= "Basic";
	
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
			//throw new ApiAuthenticationException (ApiHeaders.Authorization + Lang.SPACE + Scope.Header + "-> User/Password not set");
		}
		
		ApiConsumer consumer = new DefaultApiConsumer (ApiConsumer.Type.Basic);
		consumer.set (ApiConsumer.Fields.Uuid, aCredentials [0]);
		consumer.set (ApiConsumer.Fields.Password, aCredentials [1]);
		
		return consumer;
	}

	@Override
	public ApiConsumer authorize (Api api, ApiService service, ApiRequest request, ApiConsumer consumer)
			throws ApiAuthenticationException {
		return consumer;
	}
	
}
