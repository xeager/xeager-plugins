package com.xeager.platform.plugins.database.orientdb.impls;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.xeager.platform.db.SchemalessEntity;

public class ODocumentList<T> implements List<T> {

	private Class<?> type;
	private List<ODocument> documents;
	
	public ODocumentList (Class<?> type, List<ODocument> documents) {
		this.type = (type == null ? SchemalessEntity.class : type);
		this.documents = documents;
	}
	
	@Override
	public int size () {
		if (documents == null) {
			return 0;
		}
		return documents.size ();
	}

	@Override
	public boolean isEmpty () {
		return size () <= 0;
	}

	@Override
	public boolean contains (Object o) {
		throw new UnsupportedOperationException ("contains not supported");
	}

	@Override
	public Iterator<T> iterator () {
		throw new UnsupportedOperationException ("iterator not supported");
	}

	@Override
	public Object[] toArray () {
		throw new UnsupportedOperationException ("toArray not supported");
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray (T[] a) {
		throw new UnsupportedOperationException ("toArray not supported");
	}

	@Override
	public boolean add (T e) {
		throw new UnsupportedOperationException ("add not supported");
	}

	@Override
	public boolean remove (Object o) {
		throw new UnsupportedOperationException ("remove not supported");
	}

	@Override
	public boolean containsAll (Collection<?> c) {
		throw new UnsupportedOperationException ("containsAll not supported");
	}

	@Override
	public boolean addAll (Collection<? extends T> c) {
		throw new UnsupportedOperationException ("addAll not supported");
	}

	@Override
	public boolean addAll (int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException ("addAll not supported");
	}

	@Override
	public boolean removeAll (Collection<?> c) {
		throw new UnsupportedOperationException ("removeAll not supported");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException ("retainAll not supported");
	}

	@Override
	public void clear () {
		if (documents == null) {
			return;
		}
		documents.clear ();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get (int index) {
		if (documents == null) {
			return null;
		}
		return (T)Proxy.newProxyInstance (type.getClassLoader (), new Class<?> [] { type }, new ODocumentProxy (type, documents.get (index)));
	}

	@Override
	public T set (int index, T element) {
		throw new UnsupportedOperationException ("set not supported");
	}

	@Override
	public void add (int index, T element) {
		throw new UnsupportedOperationException ("add not supported");
	}

	@Override
	public T remove (int index) {
		if (documents == null) {
			return null;
		}
		documents.remove (index);
		return null;
	}

	@Override
	public int indexOf (Object o) {
		throw new UnsupportedOperationException ("indexOf not supported");
	}

	@Override
	public int lastIndexOf (Object o) {
		throw new UnsupportedOperationException ("lastIndexOf not supported");
	}

	@Override
	public ListIterator<T> listIterator () {
		throw new UnsupportedOperationException ("listIterator not supported");
	}

	@Override
	public ListIterator<T> listIterator (int index) {
		throw new UnsupportedOperationException ("listIterator not supported");
	}

	@Override
	public List<T> subList (int fromIndex, int toIndex) {
		throw new UnsupportedOperationException ("subList not supported");
	}

}
