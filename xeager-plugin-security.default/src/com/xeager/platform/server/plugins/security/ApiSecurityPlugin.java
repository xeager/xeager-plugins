package com.xeager.platform.server.plugins.security;

import com.xeager.platform.api.security.impls.BasicConsumerResolver;
import com.xeager.platform.api.security.impls.CookieConsumerResolver;
import com.xeager.platform.api.security.impls.SignatureConsumerResolver;
import com.xeager.platform.api.security.impls.TokenConsumerResolver;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;

public class ApiSecurityPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;

	@Override
	public void init (final ApiServer server) throws Exception {
		server.addConsumerResolver (new TokenConsumerResolver ());
		server.addConsumerResolver (new CookieConsumerResolver ());
		server.addConsumerResolver (new SignatureConsumerResolver ());
		server.addConsumerResolver (new BasicConsumerResolver ());
	}	

	@Override
	public void kill () {
	
	}
	
}
