package com.xeager.platform.storage.impls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.stream.Stream;

import com.xeager.platform.FileUtils;
import com.xeager.platform.IOUtils;
import com.xeager.platform.Lang;
import com.xeager.platform.api.ApiContext;
import com.xeager.platform.api.ApiOutput;
import com.xeager.platform.api.ApiStreamSource;
import com.xeager.platform.api.impls.DefaultApiStreamSource;
import com.xeager.platform.api.media.MediaTypeUtils;
import com.xeager.platform.json.JsonArray;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.storage.Folder;
import com.xeager.platform.storage.StorageException;
import com.xeager.platform.storage.StorageObject;

public class LocalStorageObject implements StorageObject {

	private static final long serialVersionUID = 2187711542318846311L;

	protected File 		source;
	protected String 	extension;
	
	protected LocalStorageObject () {
	}
	
	public LocalStorageObject (File source) {
		setSource (source);
	}
	
	@Override
	public void copy (Folder folder, String name, boolean move)
			throws StorageException {
		validateName (name);
		if (move) {
			source.renameTo (new File (((LocalFolder)folder).getSource (), name));
		}
	}

	@Override
	public boolean delete () throws StorageException {
		try {
			return FileUtils.delete (source);
		} catch (IOException e) {
			throw new StorageException (e.getMessage (), e);
		}
	}

	@Override
	public boolean isFolder () {
		return source.isDirectory ();
	}

	@Override
	public String name () {
		return source.getName ();
	}

	@Override
	public InputStream reader (ApiContext context) throws StorageException {
		if (isFolder ()) {
			throw new StorageException (name () + " is a folder");
		}
		InputStream is = null;
		try {
			is = new FileInputStream (source);
		} catch (IOException ioex) {
			throw new StorageException (ioex.getMessage (), ioex);
		} 
		RecyclableInputStream ris = new RecyclableInputStream (is);
		context.addRecyclable (Lang.UUID (10), ris);
		return ris;
	}

	@Override
	public OutputStream writer (ApiContext context) throws StorageException {
		if (isFolder ()) {
			throw new StorageException (name () + " is a folder");
		}
		OutputStream os = null;
		try {
			os = new FileOutputStream (source);
		} catch (IOException ioex) {
			throw new StorageException (ioex.getMessage (), ioex);
		} 
		RecyclableOutputStream ros = new RecyclableOutputStream (os);
		context.addRecyclable (Lang.UUID (10), ros);
		return ros;
	}

	@Override
	public void rename (String name) throws StorageException {
		validateName (name);
		source.renameTo (new File (source.getParentFile (), name));
		if (!name.equals (name ())) {
			throw new StorageException ("unable rename object '" + name () + "' to '" + name + "'. Maybe the object is open by another device.");
		}
	}

	@Override
	public long length () throws StorageException {
		if (source.isFile ()) {
			return source.length ();
		}
		try {
			return Files.walk (source.toPath ()).mapToLong ( p -> p.toFile ().length () ).sum ();
		} catch (IOException e) {
			throw new StorageException  (e.getMessage (), e);
		}
	}

	@Override
	public Date timestamp () {
		return new Date (source.lastModified ());
	}

	@Override
	public ApiOutput toOutput (String altName, String altContentType) throws StorageException {
		return new ApiFileOutput (this, altName, altContentType);
	}

	@Override
	public ApiStreamSource toStreamSource (String altName, String altContentType)
			throws StorageException {
		if (isFolder ()) {
			throw new StorageException ("can't acquire stream source from a folder");
		}
		if (Lang.isNullOrEmpty (altName)) {
			altName = name ();
		}
		if (Lang.isNullOrEmpty (altContentType)) {
			altContentType = contentType ();
		}
		try {
			return new DefaultApiStreamSource (altName, altContentType, new FileInputStream (source));
		} catch (FileNotFoundException e) {
			throw new StorageException (e.getMessage (), e);
		}
	}

	@Override
	public long update (InputStream input, boolean append) throws StorageException {
		OutputStream os = null;
		try {
			os = new FileOutputStream (source, append);
			return IOUtils.copy (input, os);
		} catch (IOException ioex) {
			throw new StorageException (ioex.getMessage (), ioex);
		} finally {
			IOUtils.closeQuietly (os);
		}
	}

	@Override
	public String contentType () {
		if (isFolder ()) {
			return null;
		}
		return MediaTypeUtils.getMediaForFile (this.extension);
	}
	
	@Override
	public JsonObject toJson () {
		return toJson (true);
	}
	
	public JsonObject toJson (boolean fetchChildren) {
		JsonObject data = (JsonObject)new JsonObject ()
				.set (StorageObject.Fields.Name, name ())
				.set (StorageObject.Fields.Timestamp, Lang.toUTC (timestamp ()));
		try {
			if (source.isFile ()) {
				data.set (StorageObject.Fields.Length, length ())
					.set (StorageObject.Fields.ContentType, contentType ());
			}
			
			if (source.isDirectory ()) {
				long count = new LocalFolder (source).count ();
				data.set (ApiOutput.Defaults.Count, count);
				
				// get folder size in bytes
				Stream<Path> walkStream = null;
				try {
					walkStream = Files.walk (source.toPath ());
				    long length = walkStream.filter (p -> p.toFile ().isFile ())
             				.mapToLong (p -> p.toFile ().length ())
             				.sum ();
				    
				    data.set (StorageObject.Fields.Length, length);
				    
				} finally {
					if (walkStream != null) walkStream.close ();
				}
			    		
				// get files
				if (count > 0 && fetchChildren) {
					final JsonArray items = new JsonArray ();
					data.set (ApiOutput.Defaults.Items, items);
					
					LocalStorageObject so = new LocalStorageObject ();
					
					DirectoryStream<Path> stream = null;
					try {
						stream = Files.newDirectoryStream (source.toPath ());
						for (Path p: stream) {
							so.setSource (p.toFile ());
							items.add (so.toJson (false));
					    }
					} finally {
						try { if (stream != null) stream.close (); } catch (IOException ex) {}
					}
				}

			}
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	
		return data;	
	}

	protected File getSource () {
		return source;
	}
	
	protected void setSource (File source) {
		this.source = source;
		int indexOfDot = source.getName ().lastIndexOf (Lang.DOT);
		if (source.isFile () && indexOfDot >= 0) {
			extension = source.getName ().substring (indexOfDot + 1);
		}
	}
	
	protected void validatePath (String path) throws StorageException {
		if (Lang.isNullOrEmpty (path)) {
			throw new StorageException ("invalid object path 'null'");
		}
		if (path.startsWith (Lang.SLASH) || path.endsWith (Lang.SLASH)) {
			throw new StorageException ("invalid object path '" + path + "'. It shouldn't start with slashes or contains '.', '..' or '~' such as alpha/../beta or ./gamma or ~/omega");
		}
		String [] aPath = Lang.split (path, Lang.SLASH, true);
		for (String p : aPath) {
			if (p.equals (Lang.DOT) || p.equals (Lang.DOT + Lang.DOT) || p.equals (Lang.TILDE)) {
				throw new StorageException ("invalid object path '" + path + "'. It shouldn't start with slashes or contains '.', '..' or '~' such as alpha/../beta or ./gamma or ~/omega");
			}
		}
	} 

	protected void validateName (String name) throws StorageException {
		if (Lang.isNullOrEmpty (name)) {
			throw new StorageException ("invalid object name 'null'");
		}
		if (name.indexOf (Lang.SLASH) >= 0) {
			throw new StorageException ("invalid object name '" + name + "'. It shouldn't contain a '/' (slash) character");
		}
	}
}
