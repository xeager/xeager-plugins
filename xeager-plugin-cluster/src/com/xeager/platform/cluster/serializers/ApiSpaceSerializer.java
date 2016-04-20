package com.xeager.platform.cluster.serializers;

import com.xeager.platform.cluster.ClusterSerializer;
import com.xeager.platform.json.JsonException;
import com.xeager.platform.json.JsonObject;

public class ApiSpaceSerializer implements ClusterSerializer {

	@Override
	public String name () {
		return ApiSpaceSerializer.class.getSimpleName ();
	}

	@Override
	public Object toObject (byte [] bytes, int pos, int len) {
		byte [] bSpec = new byte [len];
		System.arraycopy (bytes, pos, bSpec, 0, len); 
		try {
			return new JsonObject (new String (bSpec));
		} catch (JsonException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}

	@Override
	public byte [] toBytes (Object object) {
		return ((JsonObject)object).toString ().getBytes ();
	}

}
