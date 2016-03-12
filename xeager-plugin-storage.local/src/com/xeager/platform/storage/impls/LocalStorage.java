package com.xeager.platform.storage.impls;

import java.io.File;

import com.xeager.platform.storage.Folder;
import com.xeager.platform.storage.Storage;
import com.xeager.platform.storage.StorageException;

public class LocalStorage implements Storage {

	private static final long serialVersionUID = 9208848890318179761L;

	protected Folder root;
	
	public LocalStorage (File root) {
		this.root = new LocalFolder (root);
	}

	@Override
	public long quota () throws StorageException {
		return -1;
	}

	@Override
	public Folder root () throws StorageException {
		return root;
	}
	
	

}
