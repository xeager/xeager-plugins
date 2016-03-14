package com.xeager.platform.storage.impls;

import java.io.IOException;
import java.io.InputStream;

import com.xeager.platform.IOUtils;
import com.xeager.platform.Recyclable;

public class RecyclableInputStream extends InputStream implements Recyclable {

	private static final long serialVersionUID = 7867377376863560794L;
	
	private InputStream proxy;
	
	public RecyclableInputStream (InputStream proxy) {
		this.proxy = proxy;
	}

	@Override
	public int read () throws IOException {
		return proxy.read ();
	}

	@Override
	public int read (byte [] b) throws IOException {
		return proxy.read (b);
	}

	@Override
	public int read (byte [] b, int off, int len) throws IOException {
		return proxy.read (b, off, len);
	}

	@Override
	public long skip (long n) throws IOException {
		return proxy.skip (n);
	}

	@Override
	public int available () throws IOException {
		return proxy.available ();
	}

	@Override
	public void close () throws IOException {
		IOUtils.closeQuietly (proxy);
	}

	@Override
	public synchronized void mark (int readlimit) {
		proxy.mark (readlimit);
	}

	@Override
	public synchronized void reset () throws IOException {
		proxy.reset ();
	}

	@Override
	public boolean markSupported () {
		return proxy.markSupported ();
	}

	@Override
	public void recycle () {
		IOUtils.closeQuietly (proxy);
	}
	
}
