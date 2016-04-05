package com.xeager.platform.plugins.database.orientdb.impls;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.xeager.platform.Lang;
import com.xeager.platform.cache.Cache;
import com.xeager.platform.db.Database;
import com.xeager.platform.db.DatabaseException;
import com.xeager.platform.db.EntityConfig;
import com.xeager.platform.db.SchemalessEntity;
import com.xeager.platform.db.query.Caching.Target;
import com.xeager.platform.db.query.CompiledQuery;
import com.xeager.platform.db.query.Query;
import com.xeager.platform.db.query.Query.Operator;
import com.xeager.platform.db.query.QueryCompiler;
import com.xeager.platform.db.query.impls.SqlQueryCompiler;
import com.xeager.platform.json.JsonObject;
import com.xeager.platform.server.reflect.BeanProxy;

public class OrientDatabase implements Database {

	private static final long serialVersionUID = 3547537996525908902L;
	
	private static final String 	DateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	private static final Logger 	logger 				= Logger.getLogger (OrientDatabase.class.getName ());
	
	public static final String		CacheQueriesBucket	= "__plugin/database/odb/QueriesBucket__";
	private static final String 	Lucene 				= "LUCENE";
	
	private interface Tokens {
		String Type 		= "{Type}";
		String Field 		= "{field}";
		String Parent 		= "{parent}";
		String Child 		= "{child}";
		String Collection 	= "{collection}";
		String Value 		= "{value}";
	}
	
	private interface Sql {
		String Skip			= "skip";
		String Limit		= "limit";
	}
	
	private static final Map<IndexType, INDEX_TYPE> IndexTypes = new HashMap<IndexType, INDEX_TYPE> ();
	static {
		IndexTypes.put (IndexType.Unique, INDEX_TYPE.UNIQUE);
		IndexTypes.put (IndexType.NotUnique, INDEX_TYPE.NOTUNIQUE);
	}
	
	private static final Map<Field.Type, OType> FieldTypes = new HashMap<Field.Type, OType> ();
	static {
		FieldTypes.put (Field.Type.Boolean, OType.BOOLEAN);
		FieldTypes.put (Field.Type.Byte, 	OType.BYTE);
		FieldTypes.put (Field.Type.Binary, 	OType.BINARY);
		FieldTypes.put (Field.Type.Short, 	OType.SHORT);
		FieldTypes.put (Field.Type.Long, 	OType.LONG);
		FieldTypes.put (Field.Type.Integer, OType.INTEGER);
		FieldTypes.put (Field.Type.Double, 	OType.DOUBLE);
		FieldTypes.put (Field.Type.Float, 	OType.FLOAT);
		FieldTypes.put (Field.Type.Decimal, OType.DECIMAL);
		FieldTypes.put (Field.Type.Date,	OType.DATE);
		FieldTypes.put (Field.Type.DateTime,OType.DATETIME);
		FieldTypes.put (Field.Type.String,	OType.STRING);
	}
	
	private static final String DeleteQuery 			= "delete from " + Tokens.Type + " where " + Fields.Id + " = :" + Fields.Id;
	private static final String GetQuery 				= "select from " + Tokens.Type + " where " + Fields.Id + " = :" + Fields.Id;

	private static final String CollectionAddQuery 		= "update " + Tokens.Parent + " add " + Tokens.Collection + " = " + Tokens.Child;
	private static final String CollectionRemoveQuery 	= "update " + Tokens.Parent + " remove " + Tokens.Collection + " = " + Tokens.Child;

	private static final String IncrementQuery 			= "update " + Tokens.Type + " increment " + Tokens.Field + " = " + Tokens.Value + " where " + Fields.Id + " = :" + Fields.Id + " LOCK RECORD";
	
	private Cache cache;
	private ODatabaseDocumentTx db;
	
	public OrientDatabase (Cache cache, ODatabaseDocumentTx db) {
		this.cache = cache;
		this.db = db;
		this.db.getStorage ().getConfiguration ().dateTimeFormat = DateFormat;
	}

	@Override
	public void createIndex (Class<?> entity, IndexType type, String name,
			Field... fields) throws DatabaseException {
		createIndex (getType (entity), type, name, fields);
	}
	@Override
	public void createIndex (String eType, IndexType type, String name,
			Field... fields) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		if (fields == null || fields.length == 0) {
			throw new DatabaseException ("entity " + eType + ". fields required to create an index");
		}
		
		if (!db.getMetadata ().getSchema ().existsClass (eType)) {
			throw new DatabaseException ("entity " + eType + " not found. The Store should be created prior indexing properties");
		}
		
		boolean save = false;
		
		OClass oClass = db.getMetadata ().getSchema ().getClass (eType);
		if (oClass == null) {
			oClass = db.getMetadata ().getSchema ().createClass (eType);
			save = true;
		}
		
		String [] props = new String [fields.length];
		
		for (int i = 0; i < fields.length; i++) {
			Field f = fields [i];
			props [i] = f.name ();
			if (f.type () != null) {
				oClass.createProperty (f.name (), FieldTypes.get (f.type ()));
			}
		}
		
		switch (type) {
			case Text:
				oClass.createIndex (eType + Lang.DOT + name, "FULLTEXT", null, null, Lucene, props);
				break;
			case Spacial:
				oClass.createIndex (eType + Lang.DOT + name, "SPATIAL", null, null, Lucene, props);
				break;
			default:
				oClass.createIndex (eType + Lang.DOT + name, IndexTypes.get (type), props);
				break;
		}
		if (save) {
			db.getMetadata ().getSchema ().save ();
		}
	}

	@Override
	public void dropIndex (Class<?> entity, String name) throws DatabaseException {
		dropIndex (getType (entity), name);
	}
	@Override
	public void dropIndex (String eType, String name) throws DatabaseException {

		eType = checkNotNull (eType);
		
		if (!db.getMetadata ().getSchema ().existsClass (eType)) {
			return;
		}
		
		db.getMetadata ().getIndexManager (). dropIndex (eType + Lang.DOT + name);
	}

	@Override
	public void put (Object object) throws DatabaseException {
		_put (object);
	}
	
	private ORID _put (Object object) throws DatabaseException {
		
		if (object == null) {
			throw new DatabaseException ("can't store NULL objects");
		}
		
		if (!Proxy.isProxyClass (object.getClass ())) {
			throw new DatabaseException ("object " + object.getClass ().getSimpleName () + " is not a database object");
		}
		
		BeanProxy record = (BeanProxy)Proxy.getInvocationHandler (object);
		
		ODocument doc = (ODocument)record.getInternal ();
		
		String type = getType (record);
		
		if (doc == null) {
			// it's a map proxy with an uuid
			if (record.contains (Fields.Id)) {
				doc = _get (type, (String)record.get (Fields.Id, null, null));
			} 
		} else {
			// get the persistent record
			if (!doc.getIdentity ().isPersistent () && record.contains (Fields.Id)) {
				doc = _get (type, (String)record.get (Fields.Id, null, null));
			}
		}
		
		// if it's an update and there is no change
		if (doc != null && !record.isChanged ()) {
			return doc.getIdentity ();
		}
		
		boolean update = doc != null;
		
		if (!update) {
			doc = new ODocument (type);
		}
		
		boolean hasEntityField = false;

		Set<String> changes = record.getChanges ();
		
		Iterator<String> keys = record.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			if (update && key.equalsIgnoreCase (Fields.Id)) {
				continue;
			}
			if (update && (changes == null || !changes.contains (key))) {
				continue;
			}
			if (key.equalsIgnoreCase (Database.Fields.Entity)) {
				hasEntityField = true;
				continue;
			}
			
			Object value = record.get (key, null, null);
			
			if (value == null || value.equals (ODocumentProxy.Null)) {
				doc.removeField (key);
				continue;
			}
			
			if (isProxy (value)) {
				value = new ODocument (_put (value));
			} else if (List.class.isAssignableFrom (value.getClass ())) {
				@SuppressWarnings("unchecked")
				List<Object> children = (List<Object>)value;
				if (!children.isEmpty ()) {
					List<Object> childDocs = doc.field (key);
					if (childDocs == null) {
						childDocs = new ArrayList<Object> ();
					}
					for (Object child : children) {
						if (isProxy (child)) {
							childDocs.add (new ODocument (_put (child)));
						} else {
							childDocs.add (child);
						}
					}
					value = childDocs;
				} else {
					value = null;
				}
			}
			
			doc.field (key, value);
			
		}
		if (!record.contains (Fields.Id)) {
			EntityConfig dea = record.getType ().getAnnotation (EntityConfig.class);
			String uuid = Lang.UUID (dea != null ? dea.idLength () : 20);
			doc.field (Fields.Id, uuid);
			record.set (Fields.Id, uuid);
		}
		if (!record.contains (Fields.Timestamp)) {
			Date timestamp = new Date ();
			doc.field (Fields.Timestamp, timestamp);
			record.set (Fields.Timestamp, timestamp);
		}
		
		doc = doc.save ();
		
		record.resetChanges ();
		record.setInternal (doc);
		
		if (SchemalessEntity.class.isAssignableFrom (object.getClass ()) && hasEntityField) {
			((SchemalessEntity)object).remove (Database.Fields.Entity);
		}
		
		return doc.getIdentity ();
		
	}

	@Override
	public <T> void increment (Object object, String field, int value) throws DatabaseException {
		
		ODocument doc = getParsistentObject (object);
		if (doc == null) {
			throw new DatabaseException ("object not found in database");
		}
		
		String query = format (Lang.replace (IncrementQuery, Tokens.Value, String.valueOf (value)), doc.getClassName (), field);
		
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put (Fields.Id, doc.field (Fields.Id));
		
		db.command (new OCommandSQL (query)).execute (params);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get (Class<T> cls, String id) throws DatabaseException {
		return (T)get (getType (cls), cls, id);
		
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get (String eType, String id) throws DatabaseException {
		return (T)get (checkNotNull (eType), null, id);
	}
	
	private Object get (String eType, Class<?> cls, String id) {
		
		if (Lang.isNullOrEmpty (id)) {
			return null;
		}
		
		ODocument doc = _get (eType, id);
		if (doc == null) {
			return null;
		}
		
		if (cls == null) {
			cls = SchemalessEntity.class;
		}
		
		return toObject (cls, doc);

	}
	
	@Override
	public int delete (Object object) throws DatabaseException {
		
		if (object == null) {
			return 0;
		}
		
		if (!Proxy.isProxyClass (object.getClass ())) {
			throw new DatabaseException ("Object " + object.getClass ().getSimpleName () + " is not a database object");
		}
		
		BeanProxy record = (BeanProxy)Proxy.getInvocationHandler (object);
		
		if (record.isEmpty ()) {
			throw new DatabaseException ("Can't delete empty object");
		}
		
		String type = getType (record);

		String id = (String)record.get (Database.Fields.Id, null, null);
		if (Lang.isNullOrEmpty (id)) {
			throw new DatabaseException ("deleting an object requires an id");
		}
		
		if (!db.getMetadata ().getSchema ().existsClass (type)) {
			return 0;
		}
		
		OCommandSQL command = new OCommandSQL (format (DeleteQuery, type));
		
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put (Fields.Id, id);
		
		return db.command (command).execute (params);
		
	}

	@Override
	public void drop (Class<?> cls) throws DatabaseException {
		drop (getType (cls));
		
	}
	@Override
	public void drop (String eType) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		if (!db.getMetadata ().getSchema ().existsClass (eType)) {
			return;
		}
		
		db.getMetadata ().getSchema ().dropClass (eType);
		
		db.getMetadata ().getSchema ().reload ();
	}
	@Override
	public long count (Class<?> cls) throws DatabaseException {
		return count (getType (cls));
	}
	@Override
	public long count (String eType) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		if (!db.getMetadata ().getSchema ().existsClass (eType)) {
			return 0;
		}
		
		return db.getMetadata ().getSchema ().getClass (eType).count ();
	}

	@Override
	public int delete (Query query) throws DatabaseException {
		if (query == null) {
			return 0;
		}
		Object result = _query (null, Query.Construct.delete, query);
		if (result == null) {
			return 0;
		}
		return (Integer)result;
	}

	@Override
	public void recycle () {
		if (db != null) {
			logger.info ("Recycling database connection " + db);
			db.activateOnCurrentThread ();
			db.close ();
		}
	}
	
	@Override
	public void add (Object parent, String collection, Object child)
			throws DatabaseException {
		addRemove (CollectionAddQuery, parent, collection, child);
	}

	@Override
	public void remove (Object parent, String collection, Object child)
			throws DatabaseException {
		addRemove (CollectionRemoveQuery, parent, collection, child);
	}

	@Override
	public JsonObject describe () {
		return new JsonObject ();
	}
	
	private ODocument getParsistentObject (Object object) throws DatabaseException {
		if (object == null) {
			throw new DatabaseException ("can't store NULL objects");
		}
		
		if (!Proxy.isProxyClass (object.getClass ())) {
			throw new DatabaseException ("object " + object.getClass ().getSimpleName () + " is not a database object");
		}
		
		BeanProxy record = (BeanProxy)Proxy.getInvocationHandler (object);
		
		ODocument doc = (ODocument)record.getInternal ();
		
		String type = getType (record);
		
		if (doc == null) {
			// it's a map proxy with an uuid
			if (record.contains (Fields.Id)) {
				doc = _get (type, (String)record.get (Fields.Id, null, null));
			} 
		} else {
			// get the persistent record
			if (!doc.getIdentity ().isPersistent () && record.contains (Fields.Id)) {
				doc = _get (type, (String)record.get (Fields.Id, null, null));
			}
		}
		return doc;
	}

	private void addRemove (String queryTpl, Object parent, String collection, Object child)
			throws DatabaseException {
		
		if (parent == null || child == null) {
			return;
		}
		
		if (!Proxy.isProxyClass (parent.getClass ())) {
			throw new DatabaseException ("Object " + parent.getClass ().getSimpleName () + " is not a database object");
		}
		
		if (!Proxy.isProxyClass (child.getClass ())) {
			throw new DatabaseException ("Object " + child.getClass ().getSimpleName () + " is not a database object");
		}
		
		ODocument parentDoc = null;
		ODocument childDoc = null;
		
		BeanProxy rParentRecord = (BeanProxy)Proxy.getInvocationHandler (parent);
		parentDoc = (ODocument)rParentRecord.getInternal ();
		if (parentDoc == null && rParentRecord.contains (Database.Fields.Id)) {
			parentDoc = _get (getType (rParentRecord), (String)rParentRecord.get (Fields.Id, null, null));
		}
		if (parentDoc == null) {
			throw new DatabaseException ("Parent Object " + parent.getClass ().getSimpleName () + " is not a persistent object");
		}
		
		BeanProxy rChildRecord = (BeanProxy)Proxy.getInvocationHandler (child);
		childDoc = (ODocument)rChildRecord.getInternal ();
		if (childDoc == null && rChildRecord.contains (Database.Fields.Id)) {
			childDoc = _get (getType (rChildRecord), (String)rChildRecord.get (Fields.Id, null, null));
		}
		if (childDoc == null) {
			throw new DatabaseException ("Child Object " + child.getClass ().getSimpleName () + " is not a persistent object");
		}
		
		String query = format (
			queryTpl, 
			parentDoc.getIdentity ().toString (), 
			collection, 
			childDoc.getIdentity ().toString ()
		);
		
		db.command (new OCommandSQL (query)).execute ();
	}

	private ODocument _get (String type, String id) {
		
		if (Lang.isNullOrEmpty (type)) {
			return null;
		}
		
		if (!db.getMetadata ().getSchema ().existsClass (type)) {
			return null;
		}
		
		String query = format (GetQuery, type);
		
		OSQLSynchQuery<ODocument> q = 
				new OSQLSynchQuery<ODocument> (query, 1);
		
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put (Fields.Id, id);
		
		List<ODocument> result = db.command (q).execute (params);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return result.get (0);

	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> find (Class<T> cls, Query query, Visitor<T> visitor) throws DatabaseException {
		List<ODocument> result;
		try {
			result = (List<ODocument>)_query (getType (cls), Query.Construct.select, query);
		} catch (Exception e) {
			throw new DatabaseException (e.getMessage (), e);
		}
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return toList (cls, result, visitor);
	}

	@Override
	public <T> T findOne (Class<T> type, Query query)
			throws DatabaseException {
		List<T> result = find (type, query, null);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return result.get (0);
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> toList (Class<?> type, List<ODocument> documents, Visitor<T> visitor) {
		if (type == null) {
			type = SchemalessEntity.class;
		}
		
		if (visitor == null) {
			return new ODocumentList<T> (type, documents);
		}
		
		T t = null;
		ODocumentProxy odp = null;
		if (visitor.optimize ()) {
			odp = new ODocumentProxy (type);
			t = (T)Proxy.newProxyInstance (type.getClassLoader (), new Class<?> [] { type }, odp);
		}
		
		for (ODocument document : documents) {
			if (visitor.optimize ()) {
				odp.setDocument (document);
			} else {
				t = (T)Proxy.newProxyInstance (type.getClassLoader (), new Class<?> [] { type }, new ODocumentProxy (type, document));
			}
			boolean cancel = visitor.onRecord (t);
			if (cancel) {
				return null;
			}
		}
		return null;
	}

	private Object _query (String type, Query.Construct construct, final Query query) throws DatabaseException {
		
		if (query == null) {
			return null;
		}
		
		boolean queryHasEntity = true;
		
		String entity = query.entity ();
		
		if (Lang.isNullOrEmpty (entity)) {
			queryHasEntity = false;
			entity = type;
		}
		
		entity = checkNotNull (entity);
		
		if (logger.isDebugEnabled ()) {
			logger.debug ("Entity: " + entity);
		}
		
		if (!db.getMetadata ().getSchema ().existsClass (entity)) {
			logger.debug ("entity " + entity + " not found");
			return null;
		}
		
		String cacheKey = construct.name () + query.name ();
		
		String 				sQuery 		= null;
		Map<String, Object> bindings 	= query.bindings ();
		
		if (queryHasEntity && cache != null && cache.exists (CacheQueriesBucket) && query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ())) {
			sQuery 		= (String)cache.get (CacheQueriesBucket, cacheKey, false);
			logger.debug ("Query meta loaded from cache: " + sQuery);
		} 
		
		if (sQuery == null) {
			final String fEntity = entity;
			QueryCompiler compiler = new SqlQueryCompiler (construct) {
				private static final long serialVersionUID = -1248971549807669897L;

				@Override
				protected void onQuery (Timing timing, Query query)
						throws DatabaseException {
					super.onQuery (timing, query);
					if (Timing.start.equals (timing)) {
						return;
					}
					if (query.start () > 0) {
						buff.append (Lang.SPACE).append (Sql.Skip).append (Lang.SPACE).append (query.start ());
					}
					if (query.page () > 0) {
						buff.append (Lang.SPACE).append (Sql.Limit).append (Lang.SPACE).append (query.page ());
					}
				}
				@Override
				protected String operatorFor (Operator operator) {
					if (Operator.ftq.equals (operator)) {
						return Lucene;
					}
					return super.operatorFor (operator);
				}
				
				@Override
				protected void entity () {
					buff.append (fEntity);
				}
			}; 
			
			CompiledQuery cQuery = compiler.compile (query);
			
			sQuery 		= cQuery.query 		();
			bindings	= cQuery.bindings 	();
			
			if (queryHasEntity && cache != null && cache.exists (CacheQueriesBucket) && query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ())) {
				cache.put (CacheQueriesBucket, cacheKey, sQuery, -1);
				logger.debug ("Query meta stored in cache: " + sQuery);
			} 
		}
		
		if (logger.isDebugEnabled ()) {
			logger.debug ("   Query: " + sQuery);
			logger.debug ("Bindings: " + bindings);
		}
		
		if (Query.Construct.select.equals (construct)) {
			OSQLSynchQuery<ODocument> q = new OSQLSynchQuery<ODocument> (sQuery);
			List<ODocument> result = db.command (q).execute (bindings);
			if (result == null || result.isEmpty ()) {
				return null;
			}
			return result;
		} else {
			return db.command (new OCommandSQL (sQuery)).execute (bindings);
		}
	}

	private String format (String query, String type) {
		return Lang.replace (query, Tokens.Type, type);
	}
	
	private String format (String query, String type, String field) {
		return Lang.replace (format (query, type), Tokens.Field, field);
	}
	
	private String format (String query, String parent, String collection, String child) {
		return Lang.replace (Lang.replace (Lang.replace (query, Tokens.Collection, collection), Tokens.Parent, parent), Tokens.Child, child);
	}
	
	private boolean isProxy (Object o) {
		try {
			Proxy.getInvocationHandler (o);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	private String getType (Class<?> cls) {
		if (cls == null) {
			return null;
		}
		EntityConfig dea = cls.getAnnotation (EntityConfig.class);
		if ((dea == null || Lang.isNullOrEmpty (dea.name ())) && cls.equals (SchemalessEntity.class)) {
			return null;
		}
		return (dea != null && !Lang.isNullOrEmpty (dea.name ()) ? dea.name ().trim () : cls.getSimpleName ());
	}

	private String getType (BeanProxy record) {
		String eType = (String)record.get (Database.Fields.Entity, null, null);
		if (eType == null) {
			return getType (record.getType ());
		}
		return eType;
	}

	private String checkNotNull (String eType) throws DatabaseException {
		if (Lang.isNullOrEmpty (eType)) {
			throw new DatabaseException ("entity name is null");
		}
		return eType;
	}

	@SuppressWarnings("unchecked")
	private <T> T toObject (Class<T> type, ODocument doc) {
		return (T) Proxy.newProxyInstance (type.getClassLoader (), new Class[] { type }, new ODocumentProxy (type, doc));
	}
	
}
