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
package com.sandpolis.core.instance.state;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.instance.state.STStore.STStoreConfig;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreBase;

public final class STStore extends StoreBase implements ConfigurableStore<STStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(STStore.class);

	/**
	 * The root of the instance's state tree.
	 */
	private STDocument root;

	private ExecutorService service;

	public STStore() {
		super(log);
	}

	public STDocument root() {
		return root;
	}

	public ExecutorService pool() {
		return service;
	}

	@Override
	public void init(Consumer<STStoreConfig> configurator) {
		var config = new STStoreConfig();
		configurator.accept(config);

		service = Executors.newFixedThreadPool(config.concurrency);
		root = config.root;
		root.setOid(VirtST.DOCUMENT);
	}

	@Override
	public void close() throws Exception {
		service.shutdown();
	}

	@ConfigStruct
	public final class STStoreConfig {
		public int concurrency;
		public STDocument root;
	}

	public static final STStore STStore = new STStore();
}
