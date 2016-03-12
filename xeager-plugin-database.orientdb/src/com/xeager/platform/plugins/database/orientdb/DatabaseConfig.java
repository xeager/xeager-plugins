package com.xeager.platform.plugins.database.orientdb;

import java.io.Serializable;

import com.xeager.platform.json.JsonObject;

public class DatabaseConfig implements Serializable {

	private static final long serialVersionUID = 2978956077302402220L;
	
	private String 		host;
	
	private String 		user;
	private String 		password;
	
	private JsonObject 	pool;
	
	public DatabaseConfig () {
	}

	public String getHost () {
		return host;
	}
	public void setHost (String host) {
		this.host = host;
	}

	public String getUser () {
		return user;
	}
	public void setUser (String user) {
		this.user = user;
	}

	public String getPassword () {
		return password;
	}
	public void setPassword (String password) {
		this.password = password;
	}

	public JsonObject getPool () {
		return pool;
	}

	public void setPool (JsonObject pool) {
		this.pool = pool;
	}

}
