package com.xeager.platform.search.impls;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;

import org.elasticsearch.action.admin.indices.mapping.delete.DeleteMappingRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;

import com.xeager.platform.json.JsonObject;
import com.xeager.platform.search.Hit;
import com.xeager.platform.search.HitList;
import com.xeager.platform.search.Indexable;
import com.xeager.platform.search.SearchEngineException;
import com.xeager.platform.search.SearchIndex;

public class ElasticSearchIndex implements SearchIndex {

	private static final long serialVersionUID = -5714112356979616023L;

	private Client client;
	
	private String index;
	
	public ElasticSearchIndex (Client client, String space) {
		this.index 	= space;
		this.client = client;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void create (String collection, JsonObject spec)
			throws SearchEngineException {

		try {
			client.admin ().indices ()
				    .preparePutMapping (index)
				    .setType (collection)
				    .setSource (jsonBuilder ().map (spec))
				    .execute ().actionGet ();
		} catch (IOException ioex) {
			throw new SearchEngineException (ioex.getMessage (), ioex);
		}

	}

	@Override
	public void drop (String collection) throws SearchEngineException {
		client.admin ().indices ().deleteMapping (new DeleteMappingRequest (index).types (collection)).actionGet();
	}

	@Override
	public boolean ready () throws SearchEngineException {
		return client.admin ().indices ().prepareExists (index).execute().actionGet ().isExists ();
	}

	@Override
	public long count (String collection) throws SearchEngineException {
		return client.prepareCount (index).setTypes (collection).execute ().actionGet ().getCount ();
	}

	@Override
	public void delete (String collection, String id) throws SearchEngineException {
		client.prepareDelete (index, collection, id).execute ().actionGet (); 
	}

	@Override
	public Hit get (String collection, String id) throws SearchEngineException {
		return null;
	}

	@Override
	public void put (String collection, Indexable indexable) throws SearchEngineException {
		IndexRequest put = new IndexRequest (index, collection, indexable.id ());
		put.source (indexable.dump ());
		client.index (put).actionGet ();
	}

	@Override
	public void update (String collection, Indexable indexable)
			throws SearchEngineException {
		UpdateRequestBuilder updater = client.prepareUpdate (index, collection, indexable.id ());
		updater.setDoc (indexable.dump ());
		updater.execute ().actionGet ();
	}

	@Override
	public HitList search (String collection, JsonObject spec)
			throws SearchEngineException {
		
		SearchResponse response = client.prepareSearch (index)
		        .setTypes (collection)
		        .execute ()
		        .actionGet ();
		
		return new HitListImpl (response.getHits ());
	}

	@Override
	public void recycle () {
		client.close ();
	}

}
