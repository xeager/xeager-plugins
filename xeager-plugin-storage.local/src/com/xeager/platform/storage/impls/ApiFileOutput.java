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
import com.xeager.platform.api.ApiContentTypes;
import com.xeager.platform.api.ApiOutput;
import com.xeager.platform.api.media.MediaTypeUtils;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.storage.StorageObject;

public class ApiFileOutput implements ApiOutput {

	private static final long serialVersionUID = -4371715321710893775L;
	
	protected File file;
	protected String name;
	protected String contentType;
	
	public ApiFileOutput (File file, String name, String contentType) {
		this.file = file;
		if (!Lang.isNullOrEmpty (name)) {
			this.name = name;
		} else {
			this.name = file.getName ();
		}
		if (!Lang.isNullOrEmpty (contentType)) {
			this.contentType = contentType;
		} else {
			if (!file.isFile ()) {
				this.contentType = ApiContentTypes.Json;
			}
			this.contentType = MediaTypeUtils.getMediaForFile (extension ());
		}
	}
	
	@Override
	public JsonObject data () {
		return (JsonObject)new JsonObject ()
				.set (StorageObject.Fields.Name, name ())
				.set (StorageObject.Fields.Timestamp, Lang.toUTC (timestamp ()))
				.set (StorageObject.Fields.Size, length ())
				.set (StorageObject.Fields.ContentType, contentType ());
	}

	@Override
	public long length () {
		return file.length ();
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public String extension () {
		if (!file.isFile () || file.getName ().lastIndexOf (Lang.DOT) <= 0) {
			return null;
		}		
		return file.getName ().substring (file.getName ().lastIndexOf (Lang.DOT) + 1);
	}

	@Override
	public String contentType () {
		return contentType;
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
		return new Date (file.lastModified ());
	}

	@Override
	public InputStream toInput () throws IOException {
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
