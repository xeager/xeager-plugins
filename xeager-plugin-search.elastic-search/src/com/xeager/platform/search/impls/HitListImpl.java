package com.xeager.platform.search.impls;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.elasticsearch.search.SearchHits;

import com.xeager.platform.json.JsonObject;
import com.xeager.platform.search.Hit;
import com.xeager.platform.search.HitList;

public class HitListImpl implements HitList {
	
	private SearchHits hits;
	
	public HitListImpl (SearchHits hits) {
		this.hits = hits;
	}

	@Override
	public JsonObject dump () {
		// TODO
		return null;
	}

	@Override
	public float maxScore () {
		return hits.getMaxScore ();
	}

	@Override
	public int size () {
		if (hits == null) {
			return 0;
		}
		return (int)hits.totalHits ();
	}

	@Override
	public boolean isEmpty () {
		return size () > 0;
	}

	@Override
	public Hit get (int index) {
		if (hits == null) {
			return null;
		}
		return null;
	}

	@Override
	public Iterator<Hit> iterator () {
		// TODO
		return null;
	}

	@Override
	public ListIterator<Hit> listIterator () {
		// TODO
		return null;
	}

	@Override
	public ListIterator<Hit> listIterator (int index) {
		// TODO
		return null;
	}

	@Override
	public Object [] toArray () {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public <T> T[] toArray (T[] a) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public boolean contains (Object o) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public boolean add (Hit e) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public boolean remove (Object o) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public boolean containsAll (Collection<?> c) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public boolean addAll (Collection<? extends Hit> c) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public boolean addAll (int index, Collection<? extends Hit> c) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public boolean removeAll (Collection<?> c) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public boolean retainAll (Collection<?> c) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public void clear () {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public Hit set (int index, Hit element) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public void add (int index, Hit element) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public Hit remove (int index) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public int indexOf (Object o) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public int lastIndexOf (Object o) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

	@Override
	public List<Hit> subList (int fromIndex, int toIndex) {
		throw new UnsupportedOperationException ("method not supported by SearchEngine");
	}

}
