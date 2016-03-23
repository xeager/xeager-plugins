package com.xeager.platform.api.security.impls;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.xeager.platform.Json;
import com.xeager.platform.Lang;
import com.xeager.platform.api.Api;
import com.xeager.platform.api.ApiHeaders;
import com.xeager.platform.api.ApiRequest;
import com.xeager.platform.api.ApiRequest.Scope;
import com.xeager.platform.api.ApiService;
import com.xeager.platform.api.ApiSpace;
import com.xeager.platform.api.security.ApiAuthenticationException;
import com.xeager.platform.api.security.ApiConsumer;
import com.xeager.platform.api.security.ApiConsumerResolver;
import com.xeager.platform.api.security.ApiConsumerResolverAnnotation;
import com.xeager.platform.db.Database;
import com.xeager.platform.db.SchemalessEntity;
import com.xeager.platform.db.query.impls.JsonQuery;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.server.security.impls.DefaultApiConsumer;

@ApiConsumerResolverAnnotation (name = SignatureConsumerResolver.MethodName)
public class SignatureConsumerResolver implements ApiConsumerResolver {

	private static final long serialVersionUID = 889277317993642120L;

	protected static final String MethodName = "signature";

	private static final Logger logger = Logger.getLogger (SignatureConsumerResolver.class);

	interface Defaults {
		String 	Scheme 				= "Bearer";
		long 	Validity 			= 5;
		String 	TimestampHeader 	= ApiHeaders.Timestamp;
	}
	
	interface Spec {
		String 	Scheme 				= "scheme";
		String 	Validity 			= "validity";
		String 	TimestampHeader 	= "timestampHeader";
		interface Auth {
			String 	Feature 		= "feature";
			String 	Query 			= "query";
			String 	AccessKeyField	= "accessKeyField";
			String 	SecretKeyField	= "secretKeyField";
		}
	}

	@Override
	public ApiConsumer resolve (Api api, ApiService service, ApiRequest request)
			throws ApiAuthenticationException {
		
		JsonObject oResolver = Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Methods), MethodName);
		
		String 	application 	= Json.getString 	(oResolver, Spec.Scheme, Defaults.Scheme);

		String auth = (String)request.get (ApiHeaders.Authorization, Scope.Header);
		
		if (Lang.isNullOrEmpty (auth)) {
			return null;
		}
		
		String [] pair = Lang.split (auth, Lang.SPACE, true);
		
		if (pair.length < 2) {
			return null;
		}
		
		String app = pair [0];

		if (!app.equals (application)) {
			return null;
		}
		
		String accessKeyAndSignature = pair [1];
		if (Lang.isNullOrEmpty (accessKeyAndSignature)) {
			return null;
		}
		
		int indexOfEquals = accessKeyAndSignature.indexOf (Lang.COLON);
		if (indexOfEquals <= 0) {
			return null;
		}

		String accessKey 	= accessKeyAndSignature.substring (0, indexOfEquals);
		String signature 	= accessKeyAndSignature.substring (indexOfEquals + 1);
		
		ApiConsumer consumer = new DefaultApiConsumer (ApiConsumer.Type.Signature);
		consumer.set (ApiConsumer.Fields.AccessKey, accessKey);
		consumer.set (ApiConsumer.Fields.Signature, signature);
		
		return consumer;

	}
	
	@Override
	public ApiConsumer authorize (Api api, ApiService service, ApiRequest request, ApiConsumer consumer)
			throws ApiAuthenticationException {
		
		Object oExpiryDate = consumer.get (ApiConsumer.Fields.ExpiryDate);
		if (oExpiryDate != null) {
			Date expiryDate = null;
			if (oExpiryDate instanceof Date) {
				expiryDate = (Date)oExpiryDate;
			} else if (oExpiryDate instanceof String) {
				try {
					expiryDate = Lang.toDate ((String)oExpiryDate, Lang.DEFAULT_DATE_FORMAT);
				} catch (Exception ex) { 
					throw new ApiAuthenticationException (ex.getMessage (), ex);
				}
			} else {
				throw new ApiAuthenticationException ("unsupported expiry date format found on cunsumer " + oExpiryDate.getClass ());
			}
			if (expiryDate.before (new Date ())) {
				throw new ApiAuthenticationException ("No timestamp specified");
			}
		}
		
		JsonObject oResolver = Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Methods), MethodName);
		
		long 	validity 		= Json.getLong 		(oResolver, Spec.Validity, Defaults.Validity) * 1000;
		String 	timestampHeader = Json.getString 	(oResolver, Spec.TimestampHeader, Defaults.TimestampHeader);
		
		String accessKey = (String)consumer.get (ApiConsumer.Fields.AccessKey);
		if (Lang.isNullOrEmpty (accessKey)) {
			throw new ApiAuthenticationException ("Invalid request. Invalid consumer " + accessKey);
		}

		String timestamp = (String)request.get (timestampHeader, Scope.Header);
		if (Lang.isNullOrEmpty (timestamp)) {
			throw new ApiAuthenticationException ("No timestamp specified");
		}
		
		String signature = (String)consumer.get (ApiConsumer.Fields.Signature);
		if (Lang.isNullOrEmpty (signature)) {
			throw new ApiAuthenticationException ("Unsigned request");
		}

		String secretKey = (String)consumer.get (ApiConsumer.Fields.SecretKey);
		if (Lang.isNullOrEmpty (secretKey)) {
			secretKey = getSecretKey (api, request, accessKey);
		}
		if (Lang.isNullOrEmpty (secretKey)) {
			throw new ApiAuthenticationException ("Invalid consumer " + accessKey);
		}
		
		Date time;
		try {
			time = Lang.toUTC (timestamp);
		} catch (ParseException e) {
			throw new ApiAuthenticationException ("Bad timestamp format. Use UTC [" + Lang.UTC_DATE_FORMAT + "]");
		}
		
		if (time == null) {
			throw new ApiAuthenticationException ("Bad timestamp format. Use UTC [" + Lang.UTC_DATE_FORMAT + "]");
		}
		
		long elapsed = System.currentTimeMillis () - time.getTime ();
		if (elapsed > validity) {
			throw new ApiAuthenticationException ("Invalid request. Elapsed time must not exceed " + (validity/1000) + " seconds");
		}

		String calculated = null;
		
		try {
			calculated = api.space ().sign (request, timestamp, accessKey, (String)consumer.get (ApiConsumer.Fields.SecretKey), false);
		} catch (Exception ex) {
			throw new ApiAuthenticationException (ex.getMessage (), ex);
		}
		
		logger.info (request.getId () + " -> caldulated signature: " + calculated);
		
		if (!signature.equals (calculated)) {
			throw new ApiAuthenticationException ("Invalid signature");
		}
		
		consumer.set (ApiConsumer.Fields.Anonymous, false);
		return consumer;
	}
	
	private String getSecretKey (Api api, ApiRequest request, String accessKey) throws ApiAuthenticationException {
		JsonObject auth = Json.getObject (Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Methods), MethodName), Api.Spec.Security.Auth);
		if (auth == null || auth.isEmpty ()) {
			return null;
		}
		
		String 		feature = Json.getString (auth, Spec.Auth.Feature, ApiSpace.Features.Default);
		JsonObject 	query 	= Json.getObject (auth, Spec.Auth.Query);
		
		if (query == null || query.isEmpty ()) {
			return null;
		}
		
		Map<String, Object> bindings = new HashMap<String, Object> ();
		bindings.put (Spec.Auth.AccessKeyField, accessKey);
		
		JsonQuery q = new JsonQuery (query, bindings);
		
		SchemalessEntity odb = null;
		try {
			odb = (SchemalessEntity)api.space ().feature (Database.class, feature, request).findOne (null, q);
		} catch (Exception ex) {
			throw new ApiAuthenticationException (ex.getMessage (), ex);
		}
		
		if (odb == null) {
			throw new ApiAuthenticationException ("invalid accessKey " + accessKey);
		}
		
		Object oSecretKey = odb.get (Spec.Auth.SecretKeyField);
		
		if (oSecretKey == null) {
			throw new ApiAuthenticationException ("secret key not found for accessKey " + accessKey);
		}

		if (!(oSecretKey instanceof String)) {
			throw new ApiAuthenticationException ("secret key not of a valid type");
		}
		return (String)oSecretKey;
	}
	
}
