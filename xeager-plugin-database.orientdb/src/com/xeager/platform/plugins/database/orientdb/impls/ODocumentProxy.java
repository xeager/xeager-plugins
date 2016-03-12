package com.xeager.platform.plugins.database.orientdb.impls;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xeager.platform.Lang;
import com.xeager.platform.db.Database;
import com.xeager.platform.db.DatabaseMarshaller;
import com.xeager.platform.db.SchemalessEntity;
import com.xeager.platform.json.JsonArray;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.server.reflect.BeanProxy;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class ODocumentProxy extends BeanProxy {
	
	public static final Object Null = new Object ();

	public ODocumentProxy (Class<?> type) {
		super (type == null ? SchemalessEntity.class : type);
	}

	public ODocumentProxy (Class<?> type, ODocument document) {
		this (type);
		this.internal = document;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object get (String key, Class<?> type, Class<?> subType) {
		
		ODocument document = (ODocument)internal;

		Object v = document.field (key);
		if (v == null) {
			return null;
		}
		if (Map.class.isAssignableFrom (v.getClass ()) && !(v instanceof JsonObject)) {
			return new JsonObject ((Map)v, true);
		} else if (ODocument.class.isAssignableFrom (v.getClass ())) {
			return Proxy.newProxyInstance (type.getClassLoader (), new Class[] { type }, new ODocumentProxy (type, (ODocument)v));
		} else if (List.class.isAssignableFrom (v.getClass ())) {
			List<Object> objects = (List<Object>)v;
			if (objects.isEmpty ()) {
				return null;
			}
			if (ODocument.class.isAssignableFrom (objects.get (0).getClass ())) {
				return new ODocumentList (subType, objects);
			}
			return new JsonArray (objects);
		}
		return v;
	}

	@Override
	public Iterator<String> keys () {
		
		ODocument document = (ODocument)internal;
		
		final String [] fields = document.fieldNames ();
		if (fields == null) {
			return null;
		}
		
		return new Iterator<String> () {
			int index = -1;
			@Override
			public boolean hasNext () {
				index++;
				return index < fields.length;
			}
			@Override
			public String next () {
				return fields [index];
			}
			@Override
			public void remove () {
				throw new UnsupportedOperationException ("Iterator.remove not supported");
			}
		};
	}

	@Override
	public void remove (String key) {
		set (key, Null);
	}

	@Override
	public void set (String key, Object value) {
		ODocument document = (ODocument)internal;
		String 	uuid = (String)document.field (Database.Fields.Uuid);
		Date 	time = (Date)document.field (Database.Fields.Timestamp);
		if (changes == null) {
			
			// creating a new document for update
			document = new ODocument (document.getClassName (), document.getIdentity ());
			document.field (Database.Fields.Uuid, uuid);
			document.field (Database.Fields.Timestamp, time);
			internal = document;
			
			changes = new HashSet<String> ();
		}
		changes.add (key);
		document.field (key, value);
	}

	@Override
	public boolean contains (String key) {
		ODocument document = (ODocument)internal;
		return document.containsField (key);
	}

	@Override
	public boolean isEmpty () {
		if (changes == null) {
			return true;
		}
		return changes.isEmpty ();
	}

	@Override
	public JsonObject dump () {
		return toJson ((ODocument)internal);
	}
	
	@SuppressWarnings("unchecked")
	private JsonObject toJson (ODocument doc) {
		
		Set<String> discard = null;
		
		DatabaseMarshaller man = type.getAnnotation (DatabaseMarshaller.class);
		if (man != null) {
			discard = new HashSet<String>(Arrays.asList(man.discard ()));
		}
		
		JsonObject obj = new JsonObject ();
		
		String [] fields = doc.fieldNames ();
		for (String f : fields) {
			if (discard != null && discard.contains (f)) {
				continue;
			}
			Object v = doc.field (f);
			if (v instanceof Date) {
				v = Lang.toUTC ((Date)v);
			} else if (v instanceof Map) {
				v = new JsonObject ((Map<String, Object>)v, true);
			} else if (v instanceof List) {
				v = new JsonArray ((List<Object>)v);
			} else if (v instanceof ODocument) {
				v = toJson ((ODocument)v);
			}
			
			obj.set (f, v);
		}
		
		return obj;
		
	}

	public ODocument getDocument  () {
		return (ODocument)internal;
	}

	public void setDocument (ODocument document) {
		this.internal = document;
	}

}
