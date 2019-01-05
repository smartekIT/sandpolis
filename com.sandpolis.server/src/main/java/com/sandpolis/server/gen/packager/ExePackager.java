/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.gen.packager;

import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.server.gen.Packager;

/**
 * This {@link Packager} produces a Windows portable executable.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class ExePackager extends Packager {
	private ExePackager() {
	}

	public static final ExePackager INSTANCE = new ExePackager();

	@Override
	public void process(GenConfig config, Object payload) throws Exception {
		// TODO Auto-generated method stub

	}

}
