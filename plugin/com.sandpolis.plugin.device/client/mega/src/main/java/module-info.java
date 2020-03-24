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
module com.sandpolis.plugin.device.client.mega {
	exports com.sandpolis.plugin.device.client.mega.snmp.library;

	requires com.sandpolis.core.instance;
	requires org.snmp4j;
	requires com.sandpolis.core.net;
	requires com.google.protobuf;
	requires com.sandpolis.plugin.device;
}
