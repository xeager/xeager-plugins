package com.xeager.platform.storage.impls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.xeager.platform.IOUtils;
import com.xeager.platform.api.ApiStreamSource;
import com.xeager.platform.storage.Folder;
import com.xeager.platform.storage.StorageException;
import com.xeager.platform.storage.StorageObject;

public class LocalFolder extends LocalStorageObject implements Folder {

	private static final long serialVersionUID = 2756236507680103819L;
	
	public LocalFolder (File source) {
		super (source);
	}
	
	@Override
	public Folder add (String path) throws StorageException {
		validatePath (path); 
		File folder = new File (source, path);
		if (folder.exists ()) {
			throw new StorageException ("folder '" + path + "' already exists under " + name ());
		}
		folder.mkdirs ();
		if (!folder.exists ()) {
			throw new StorageException ("unbale to create folder '" + path + "' under " + name ());
		}
		return new LocalFolder (folder);
	}

	@Override
	public StorageObject add (ApiStreamSource ss, String altName)
			throws StorageException {
		
		String name = altName != null ? altName : ss.name ();
		
		validateName (name); 
		
		File file = new File (source, name);
		if (file.exists ()) {
			throw new StorageException ("object '" + name + "' already exists under " + name ());
		}
		
		OutputStream os = null;
		try {
			os = new FileOutputStream (file);
			IOUtils.copy (ss.stream (), os);
		} catch (IOException ioex) {
			throw new StorageException (ioex.getMessage (), ioex);
		} finally {
			IOUtils.closeQuietly (os);
		}
		
		return new LocalStorageObject (file);
	}

	@Override
	public StorageObject get (String path) throws StorageException {
		
		validatePath (path);
		
		File file = new File (source, path);
		if (!file.exists ()) {
			throw new StorageException ("object '" + path + "' not found under " + name ());
		}
		
		if (file.isDirectory ()) {
			return new LocalFolder (file);
		}
		
		return new LocalStorageObject (file);
	}

	@Override
	public void list (Visitor visitor) throws StorageException {
		if (visitor == null) {
			return;
		}
		
		LocalStorageObject so = new LocalStorageObject ();
		
		try {
			Files.newDirectoryStream (
				source.toPath (), 
				new DirectoryStream.Filter<Path>() {
					@Override
					public boolean accept (Path p) throws IOException {
						so.setSource (p.toFile ());
						visitor.visit (so);
						return false;
					}
				}
			);
		} catch (IOException e) {
			throw new StorageException (e.getMessage (), e);
		}

	}

}
