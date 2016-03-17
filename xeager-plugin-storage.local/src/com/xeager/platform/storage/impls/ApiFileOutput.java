package com.xeager.platform.storage.impls;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import com.xeager.platform.IOUtils;
import com.xeager.platform.Lang;
import com.xeager.platform.api.ApiOutput;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.storage.StorageException;
import com.xeager.platform.storage.StorageObject;

public class ApiFileOutput implements ApiOutput {

	private static final long serialVersionUID = -4371715321710893775L;
	
	private StorageObject 	object;
	private String 			name;
	private String 			contentType;
	
	public ApiFileOutput (StorageObject object, String altName, String altContentType) {
		this.object 		= object;
		this.name 			= altName;
		this.contentType 	= altContentType;
	}
	
	@Override
	public JsonObject data () {
		try {
			return object.toJson ();
		} catch (StorageException e) {
			throw new RuntimeException (e.getMessage (), e);
		}	
	}

	@Override
	public long length () {
		try {
			return object.length ();
		} catch (StorageException e) {
			throw new RuntimeException (e.getMessage (), e);
		}	
	}

	@Override
	public String name () {
		if (!Lang.isNullOrEmpty (name)) {
			return name;
		}
		return object.name ();
	}

	@Override
	public String extension () {
		String name = object.name ();
		return name.substring (name.lastIndexOf (Lang.DOT) + 1);
	}

	@Override
	public String contentType () {
		if (!Lang.isNullOrEmpty (contentType)) {
			return contentType;
		}
		return object.contentType ();
	}

	@Override
	public void pipe (OutputStream out) throws IOException {
		InputStream is = null;
		try {
			is = toInput ();
			IOUtils.copy (is, out);
		} finally {
			IOUtils.closeQuietly (is);
		}
	}

	@Override
	public Date timestamp () {
		return object.timestamp ();
	}

	@Override
	public InputStream toInput () throws IOException {
		File file = ((LocalStorageObject)object).getSource ();
		if (file.isFile ()) {
			return new FileInputStream (file);
		} else {
			return new ByteArrayInputStream (data ().toString ().getBytes ());
		}
	}

	@Override
	public JsonObject meta () {
		return null;
	}

}
