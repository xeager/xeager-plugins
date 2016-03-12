package com.xeager.platform.server.plugins.watchdog;

import java.io.File;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.xeager.platform.plugins.impls.AbstractPlugin;
import com.xeager.platform.server.ApiServer;
import com.xeager.platform.server.utils.ConfigKeys;

public class WatchdogPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;

	private int watchPeriod = 5;
	
	@Override
	public void init (final ApiServer server, File home) throws Exception {

        final long pollingInterval = watchPeriod * 1000;

        File folder = new File (home.getParent (), ConfigKeys.Folders.Spaces);

        FileAlterationObserver observer = new FileAlterationObserver (folder);
        FileAlterationMonitor monitor = new FileAlterationMonitor (pollingInterval);
        
        FileAlterationListener listener = new FileAlterationListenerAdaptor () {
            @Override
            public void onFileCreate (File file) {
            	if (!file.isFile ()) {
            		return;
            	}
                try {
					//server.install (file, null, InstallType.Upgrade);
				} catch (Exception e) {
					throw new RuntimeException (e.getMessage (), e);
				}
            }
        };

        observer.addListener (listener);
        monitor.addObserver (observer);
        monitor.start ();
	}

	@Override
	public void kill () {
	}
	
}
