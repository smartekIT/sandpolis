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
package com.sandpolis.core.instance.state.oid;

import com.sandpolis.core.instance.state.VirtProfile;
import com.sandpolis.core.instance.state.VirtProfile.Oid;

public class InstanceOid {

	private static final InstanceOid instance = new InstanceOid();

	public final VirtProfile.Oid profile = new VirtProfile.Oid("/profile");

	public VirtProfile.Oid profile(String path) {
		return (Oid) profile.resolve(path);
	}

	public static InstanceOid InstanceOid() {
		return instance;
	}
}
