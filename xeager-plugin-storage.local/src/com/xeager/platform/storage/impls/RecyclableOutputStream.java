package com.xeager.platform.storage.impls;

import java.io.IOException;
import java.io.OutputStream;

import com.xeager.platform.IOUtils;
import com.xeager.platform.Recyclable;

public class RecyclableOutputStream extends OutputStream implements Recyclable {

	private static final long serialVersionUID = 7867377376863560794L;
	
	private OutputStream proxy;
	
	public RecyclableOutputStream (OutputStream proxy) {
		this.proxy = proxy;
	}

	@Override
	public void write (int b) throws IOException {
		proxy.write (b);
	}

	@Override
	public void write (byte [] b) throws IOException {
		proxy.write (b);
	}

	@Override
	public void write (byte [] b, int off, int len) throws IOException {
		proxy.write (b, off, len);
	}

	@Override
	public void flush () throws IOException {
		proxy.flush ();
	}

	@Override
	public void close () throws IOException {
		IOUtils.closeQuietly (proxy);
	}

	@Override
	public void recycle () {
		IOUtils.closeQuietly (proxy);
	}
	
}
