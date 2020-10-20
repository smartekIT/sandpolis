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
package com.sandpolis.core.instance.profile;

import com.sandpolis.core.instance.state.VirtProfile;
import com.sandpolis.core.instance.state.st.STDocument;

/**
 * A {@link Profile} is a generic container that stores data for an instance.
 * Most of the data are stored in a tree structure similar to a document store.
 *
 * @since 4.0.0
 */
public class Profile extends VirtProfile {

	Profile(STDocument parent) {
		super(parent);
	}

}
