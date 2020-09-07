//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.instance.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.collect.Streams;
import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.VirtObject;

public abstract class STCollectionStore<V extends VirtObject> extends StoreBase
		implements MetadataStore<StoreMetadata> {

	protected STCollection collection;

	private Map<Integer, V> cache = new HashMap<>();

	protected STCollectionStore(Logger log) {
		super(log);
	}

	protected abstract V constructor(STDocument document);

	protected void add(V object) {
		if (object.complete() != ErrorCode.OK) {
			// TODO
			throw new RuntimeException();
		}
		int tag = object.tag();
		collection.setDocument(tag, object.document);
		cache.put(tag, object);
	}

	/**
	 * Determine how many elements the store contains.
	 *
	 * @return The number of elements in the store
	 */
	public long count() {
		return collection.size();
	}

	public Optional<V> get(int tag) {
		var object = cache.get(tag);
		if (object != null)
			return Optional.of(object);

		var document = collection.getDocument(tag);
		if (document == null) {
			return Optional.empty();
		} else {
			return Optional.of(constructor(document));
		}
	}

	public Optional<V> remove(int tag) {
		var item = get(tag);
		if (item.isPresent())
			removeValue(item.get());
		return item;
	}

	public void removeValue(V value) {
		cache.values().remove(value);
		collection.remove(value.document);
	}

	public Stream<V> stream() {
		if (cache.size() == 0)
			return collection.documents().map(this::constructor);

		return Streams.concat(cache.values().stream(), collection.documents().map(this::constructor)).distinct();
	}

	@Override
	public StoreMetadata getMetadata() {
		return collection.getMetadata();
	}
}
