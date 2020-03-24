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
package com.sandpolis.server.vanilla.gen.mega;

import com.sandpolis.core.instance.Generator.GenConfig;
import com.sandpolis.server.vanilla.gen.MegaGen;

/**
 * This generator produces a Windows batch file.
 *
 * @author cilki
 * @since 5.0.0
 */
public class BatPackager extends MegaGen {
	public BatPackager(GenConfig config) {
		super(config, ".bat", "/lib/sandpolis-client-installer.bat");
	}

	@Override
	protected byte[] generate() throws Exception {
		return null;
	}
}
