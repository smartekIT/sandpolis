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
open module com.sandpolis.core.profile {
	exports com.sandpolis.core.profile.attribute.key;
	exports com.sandpolis.core.profile.attribute;
	exports com.sandpolis.core.profile.cmd;
	exports com.sandpolis.core.profile.store;
	exports com.sandpolis.core.profile;

	requires com.google.common;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires java.persistence;
	requires org.slf4j;
}
