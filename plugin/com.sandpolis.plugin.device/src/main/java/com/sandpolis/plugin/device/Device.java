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
package com.sandpolis.plugin.device;

import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.plugin.device.StateTree.VirtPlugin.VirtDevice;

public class Device extends VirtDevice {

	Device(STDocument document) {
		super(document);
	}

	public String getId() {
		return "";
	}
}
