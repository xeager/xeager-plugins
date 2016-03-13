package com.xeager.platform.plugins.impls.jetty;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

import com.xeager.platform.Json;
import com.xeager.platform.Lang;
import com.xeager.platform.api.ApiRequest;
import com.xeager.platform.api.http.HttpApiRequest;
import com.xeager.platform.api.http.HttpApiResponse;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;

public class JettyHttpPlugin extends AbstractPlugin {
	
	private static final long serialVersionUID = 4642997488038621776L;
	
	private static final Logger logger = Logger.getLogger (JettyHttpPlugin.class);

	private static final String PoolMin 				= "min";
	private static final String PoolMax 				= "max";
	private static final String PoolIdleTimeout 		= "idleTimeout";
	private static final String PoolQueueCapacity 		= "queueCapacity";
	
	private static final String SslKeyStore 			= "keyStore";
	private static final String SslPassword 			= "keyPassword";
	private static final String SslPort 				= "port";
	
	private String 		name;
	private int 		port = 80;
	private JsonObject 	pool = (JsonObject)new JsonObject ().set (PoolMin, 20).set (PoolMax, 200);
	private String 		context = Lang.SLASH;
	private boolean 	gzip = true;
	
	private JsonObject	ssl;

	private Server httpServer;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Integer poolMax 			= Json.getInteger (pool, PoolMax, 200);
		Integer poolMin 			= Json.getInteger (pool, PoolMin, 20);
		Integer poolIdleTimeout 	= Json.getInteger (pool, PoolIdleTimeout, 5);
		Integer poolQueueCapacity 	= Json.getInteger (pool, PoolQueueCapacity, 6000);
		
		QueuedThreadPool tp = new QueuedThreadPool (
			poolMax, 
			poolMin, 
			poolIdleTimeout * 60 * 1000,
			new ArrayBlockingQueue<Runnable> (poolQueueCapacity)
		);
		tp.setDetailedDump (false);
		
		httpServer = new Server (tp);
		
		ServerConnector connector = new ServerConnector (httpServer);
        connector.setPort (port);
        httpServer.addConnector (connector);
        
        if (ssl != null) {
            SslContextFactory contextFactory = new SslContextFactory();
            contextFactory.setKeyStorePath (new File (home, ssl.getString (SslKeyStore)).getAbsolutePath ());
            contextFactory.setKeyStorePassword (ssl.getString (SslPassword));
            SslConnectionFactory sslConnectionFactory = new SslConnectionFactory (contextFactory, HttpVersion.HTTP_1_1.toString ());

            HttpConfiguration config = new HttpConfiguration();
            config.setSecureScheme ("https");
            Integer sslPort = Json.getInteger (ssl, SslPort, 443);
            config.setSecurePort (sslPort);
            
            HttpConfiguration sslConfiguration = new HttpConfiguration (config);
            sslConfiguration.addCustomizer (new SecureRequestCustomizer ());
            
            HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory (sslConfiguration);

            ServerConnector sslConnector = new ServerConnector (httpServer, sslConnectionFactory, httpConnectionFactory);
            sslConnector.setPort (sslPort);
            
            httpServer.addConnector (sslConnector);
        }
        
        for (Connector cn : httpServer.getConnectors ()) {
            for (ConnectionFactory x  : cn.getConnectionFactories ()) {
    	        if (x instanceof HttpConnectionFactory) {
    	            ((HttpConnectionFactory)x).getHttpConfiguration ().setSendServerVersion (false);
    	        }
    	    }
        }
		
        ServletContextHandler sContext = new ServletContextHandler (ServletContextHandler.NO_SESSIONS);
        sContext.setContextPath (context);
        
        if (gzip) {
            sContext.setGzipHandler (new GzipHandler ());
        }
        
		httpServer.setHandler (sContext);

		ServletHolder apiHolder = 
			new ServletHolder (new HttpServlet () {
	        	
	        	private static final long serialVersionUID = -4391155835460802144L;
	
				protected void doGet (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		execute (req, resp);
	        	}
	
	        	protected void doPost (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		execute (req, resp);
	        	}
	
	        	protected void doPut (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		execute (req, resp);
	        	}
	
	        	protected void doDelete (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		execute (req, resp);
	        	}
	
	        	protected void execute (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		try {
	        			ApiRequest request = new HttpApiRequest (req);
	        			server.execute (request, new HttpApiResponse (request.getId (), resp));
	        		} catch (Exception e) {
	        			throw new ServletException (e.getMessage (), e);
	        		}
	        	}
	        	
	        });

		File uploadTmp = new File (home, "upload/tmp");
		if (!uploadTmp.exists ()) {
			uploadTmp.mkdirs ();
		}
		
        apiHolder.getRegistration ().setMultipartConfig (new MultipartConfigElement (uploadTmp.getAbsolutePath (), 1024*1024*50, 1024*1024*100, 1024*1024*10));
        
        sContext.addServlet (apiHolder, Lang.SLASH + Lang.STAR);
        
		httpServer.start ();

        httpServer.join ();

	}
	
	@Override
	public void kill () {
		if (httpServer == null) {
			return;
		}
		try {
			httpServer.stop ();
		} catch (Exception e) {
			logger.error (Lang.BLANK, e);
		}
	}

	public int getPort () {
		return port;
	}
	public void setPort (int port) {
		this.port = port;
	}

	public JsonObject getPool() {
		return pool;
	}
	public void setPool(JsonObject pool) {
		this.pool = pool;
	}

	public JsonObject getSsl() {
		return ssl;
	}

	public void setSsl(JsonObject ssl) {
		this.ssl = ssl;
	}

	public String getContext () {
		return context;
	}
	public void setContext (String context) {
		this.context = context;
	}

	@Override
	public void setName (String name) {
		this.name = name;
	}

	@Override
	public String getName () {
		return name;
	}

	public boolean isGzip () {
		return gzip;
	}
	public void setGzip (boolean gzip) {
		this.gzip = gzip;
	}

}
