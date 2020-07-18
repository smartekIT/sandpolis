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
package com.sandpolis.server.vanilla.gen;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Generator.GenConfig;
import com.sandpolis.core.instance.Generator.MicroConfig;
import com.sandpolis.core.foundation.soi.Build.SO_Build;

/**
 * Generates a <b>com.sandpolis.client.micro</b> stub.
 *
 * @author cilki
 * @since 6.1.0
 */
public class MicroGen extends Generator {

	private static final Logger log = LoggerFactory.getLogger(MicroGen.class);

	public MicroGen(GenConfig config, String archiveExtension) {
		super(config, archiveExtension);
	}

	@Override
	protected byte[] generate() throws Exception {
		// TODO get stub according to platform/architecture
		Path output = null;

		// TODO maybe the com.sandpolis.gradle.soi plugin should take care of this?
		inject(output, soi_build, Core.SO_BUILD.toByteArray());

		// Inject the client configuration
		inject(output, soi_client, config.getMicro().toByteArray());

		// TODO return actual output
		return new byte[] {};
	}

	/**
	 * Replace the first occurrence of the placeholder in the binary file with the
	 * given replacement. This method uses a standard needle/haystack linear search
	 * algorithm with backtracking.
	 *
	 * <p>
	 * The first four bytes of the placeholder must be zero and will become the size
	 * of the replacement buffer. Therefore, the replacement length must be no
	 * greater than four less than the length of the placeholder.
	 *
	 * @param binary      The binary file to process
	 * @param placeholder The unique placeholder
	 * @param replacement The payload buffer
	 * @throws IOException
	 */
	private void inject(Path binary, short[] placeholder, byte[] replacement) throws IOException {
		// Check the first four bytes of the placeholder
		if (placeholder[0] != 0 || placeholder[1] != 0 || placeholder[2] != 0 || placeholder[3] != 0)
			throw new IllegalArgumentException("The first four bytes of the placeholder must be 0");

		// Check the replacement buffer size
		if (replacement.length > placeholder.length - Integer.BYTES)
			throw new BufferOverflowException();

		try (var ch = FileChannel.open(binary, READ, WRITE)) {
			var buffer = ch.map(MapMode.READ_WRITE, ch.position(), ch.size()).order(ByteOrder.nativeOrder());

			buffer.mark();
			find: while (buffer.remaining() >= placeholder.length) {
				for (int i = 0; i < placeholder.length; i++) {
					if (buffer.get() != (byte) placeholder[i]) {
						buffer.reset();
						buffer.position(buffer.position() + 1).mark();
						continue find;
					}
				}

				// Return to the start of the placeholder
				buffer.position(buffer.position() - placeholder.length);

				// Overwrite!
				log.debug("Writing {} bytes at: 0x{}", replacement.length, Integer.toHexString(buffer.position()));
				buffer.putInt(replacement.length);
				buffer.put(replacement);

				return;
			}

			// Placeholder not found
			throw new IOException("Failed to find placeholder in binary");
		} finally {
			// Since it's not possible to explicitly unmap files yet, signalling the garbage
			// collector is the best we can do.
			System.gc();
		}
	}

	/**
	 * A placeholder for the unsigned bytes of a serialized {@link SO_Build}.
	 *
	 * <p>
	 * Note: synchronized with util/resources.hh in
	 * <b>com.sandpolis.client.micro</b>.
	 */
	private static final short[] soi_build = { 0x00, 0x00, 0x00, 0x00, 0x92, 0xc4, 0xda, 0x71, 0x4d, 0x6c, 0x0a, 0x64,
			0x0f, 0x55, 0x27, 0x5c, 0xb8, 0x27, 0x1b, 0x6f, 0x91, 0x41, 0x1d, 0x90, 0x64, 0xc4, 0x30, 0xff, 0x20, 0xf7,
			0x8d, 0xb2, 0xf5, 0xeb, 0x92, 0x3b, 0xca, 0xa0, 0x23, 0x95, 0x21, 0x0e, 0x1c, 0x72, 0x1b, 0x0c, 0xec, 0x64,
			0x54, 0xf6, 0xe4, 0x14, 0xe9, 0x01, 0xd0, 0x37, 0xaf, 0xaf, 0x29, 0xb5, 0x00, 0x17, 0x36, 0x32, 0xa3, 0xfa,
			0x22, 0xda, 0x41, 0x95, 0x4c, 0x41, 0x1c, 0x5b, 0xbb, 0x31, 0xd8, 0x5c, 0x47, 0x9e, 0x2c, 0x80, 0x4e, 0x61,
			0x4c, 0x10, 0x0f, 0x14, 0x92, 0x37, 0x9d, 0xa1, 0x93, 0x8a, 0x19, 0x8d, 0xa3, 0x2a, 0x5f, 0x26, 0xe6, 0x82,
			0x16, 0x6f, 0xe1, 0xec, 0x2a, 0x10, 0x54, 0x10, 0xa8, 0x37, 0xe3, 0x0a, 0xf7, 0x6f, 0x23, 0x90, 0x56, 0x1a,
			0x18, 0x57, 0xba, 0x73, 0xed, 0x3a, 0x67, 0x9b };

	/**
	 * A placeholder for the unsigned bytes of a serialized {@link MicroConfig}.
	 *
	 * <p>
	 * Note: synchronized with util/resources.hh in
	 * <b>com.sandpolis.client.micro</b>.
	 */
	private static final short[] soi_client = { 0x00, 0x00, 0x00, 0x00, 0x11, 0x2d, 0x93, 0x29, 0x0a, 0x38, 0x80, 0x0e,
			0xbc, 0x6b, 0x7c, 0xbd, 0xdc, 0x0f, 0x3e, 0x2b, 0x2c, 0x17, 0x60, 0xde, 0x33, 0x65, 0x48, 0x74, 0xb9, 0x89,
			0x76, 0x00, 0x37, 0x83, 0x05, 0x8a, 0xa2, 0xa5, 0xea, 0x0d, 0x6f, 0xbc, 0x47, 0x5d, 0x96, 0xa8, 0xa5, 0xa7,
			0x0f, 0xe8, 0x6a, 0xe8, 0x96, 0xaa, 0xec, 0xc8, 0x29, 0xd9, 0x2a, 0x46, 0xaf, 0x85, 0x71, 0xcf, 0x86, 0xf0,
			0x72, 0x1d, 0x5b, 0xbe, 0x01, 0x34, 0xf3, 0x7b, 0x42, 0x08, 0x64, 0x56, 0x9e, 0x2a, 0x3e, 0xd5, 0xc7, 0x61,
			0x40, 0x00, 0xea, 0xc6, 0x74, 0xa9, 0xf8, 0x9c, 0xc9, 0x92, 0x24, 0x9f, 0xfa, 0xad, 0x73, 0x51, 0x50, 0xd4,
			0xd5, 0x7c, 0xa5, 0x15, 0x45, 0x2d, 0x0d, 0x56, 0x01, 0x65, 0x9a, 0x7d, 0x8b, 0xca, 0xdd, 0x09, 0xfe, 0xe8,
			0xc9, 0x87, 0x58, 0x53, 0xab, 0x89, 0x50, 0x57, 0x59, 0xd2, 0x09, 0xf2, 0x9d, 0x40, 0x38, 0xae, 0x2f, 0xf5,
			0x0e, 0x72, 0xc1, 0xf4, 0xbe, 0xbb, 0xa0, 0xdd, 0xc2, 0xd6, 0x9d, 0xa6, 0xa6, 0x47, 0xae, 0x8a, 0xfe, 0x93,
			0x60, 0x65, 0xe5, 0xbd, 0x15, 0x37, 0xfe, 0xa0, 0x7f, 0xfb, 0x04, 0xec, 0x2d, 0x16, 0xb7, 0x52, 0x88, 0x01,
			0xdc, 0xd4, 0x3f, 0xfe, 0x97, 0xa6, 0x81, 0x77, 0x7e, 0x57, 0xd0, 0xc3, 0xa4, 0x96, 0xc9, 0x41, 0x9a, 0x50,
			0xe9, 0x2d, 0x44, 0x31, 0x2a, 0xfb, 0x5e, 0xcd, 0xe1, 0x81, 0xd4, 0x0b, 0x1f, 0xef, 0xee, 0xb0, 0x2a, 0x56,
			0x8c, 0x44, 0x36, 0xe1, 0x9a, 0x03, 0x7d, 0x9e, 0x11, 0xbe, 0x43, 0x74, 0x61, 0xec, 0x49, 0x6b, 0x22, 0x8f,
			0xf6, 0x73, 0x67, 0xc3, 0xe5, 0x32, 0xa8, 0x25, 0xee, 0x8e, 0xc2, 0x43, 0xaf, 0x3f, 0xf0, 0x6e, 0xc9, 0xd5,
			0x80, 0x58, 0x57, 0x56, 0xaf, 0x1b, 0x64, 0x46, 0x8e, 0x60, 0x1e, 0xf9, 0xd7, 0xa1, 0xad, 0x68, 0x76, 0x99,
			0x42, 0xe2, 0x1f, 0xd8, 0x6c, 0x7a, 0x7e, 0x82, 0xf6, 0x73, 0x80, 0xb7, 0xc4, 0xfe, 0x30, 0x56, 0x21, 0xb3,
			0x48, 0xf4, 0xce, 0xd8, 0x63, 0x87, 0xeb, 0x4a, 0xbf, 0xb8, 0x3e, 0xc1, 0xd9, 0xce, 0x80, 0xa6, 0x9e, 0x10,
			0xd6, 0x69, 0xaa, 0xce, 0xd0, 0xa9, 0x2a, 0xec, 0x90, 0xa8, 0x1b, 0x31, 0x48, 0x38, 0xa7, 0xc0, 0xd3, 0xa7,
			0x09, 0x46, 0xa7, 0x14, 0xe3, 0xb5, 0x52, 0xd1, 0x2d, 0x66, 0x36, 0x46, 0x1b, 0x84, 0x7f, 0x0e, 0x7e, 0x14,
			0xb3, 0xd7, 0x58, 0x5a, 0x3e, 0x62, 0x34, 0xc7, 0xe4, 0xab, 0xb4, 0x1e, 0x2e, 0x2c, 0xc9, 0x98, 0xfa, 0x37,
			0xee, 0xd4, 0x20, 0x11, 0xba, 0x99, 0x24, 0x50, 0xed, 0x77, 0x42, 0xbd, 0x53, 0xc0, 0xa7, 0x44, 0x52, 0x26,
			0xde, 0xe2, 0x84, 0x32, 0xfb, 0xf2, 0x07, 0x27, 0xf5, 0xeb, 0xd7, 0x44, 0xc1, 0xdd, 0x6e, 0x69, 0x11, 0x5f,
			0x74, 0x79, 0xc5, 0x1c, 0x9d, 0xd9, 0xf2, 0x31, 0x2b, 0xf8, 0xbc, 0xd4, 0xbf, 0x66, 0x4e, 0x68, 0x8e, 0x08,
			0x7d, 0xd4, 0x78, 0x04, 0xb8, 0x09, 0xd2, 0x43, 0x31, 0x28, 0x7b, 0x4a, 0xf0, 0xc8, 0x52, 0xf8, 0xf2, 0xbc,
			0x83, 0x64, 0xee, 0x29, 0x6d, 0x42, 0x16, 0xe6, 0xa7, 0xb0, 0x0c, 0x34, 0x12, 0xcb, 0xfc, 0xa7, 0x3d, 0x8f,
			0x8e, 0x3b, 0x69, 0xcd, 0xd7, 0xad, 0x02, 0x9b, 0xb0, 0x36, 0xe7, 0xef, 0x6a, 0x61, 0xac, 0xd2, 0xe9, 0x9d,
			0x8c, 0xd8, 0xab, 0x46, 0x94, 0x8e, 0x94, 0x75, 0x3c, 0xce, 0x7e, 0x05, 0x7e, 0xa5, 0xa9, 0x17, 0x9e, 0x5c,
			0xb1, 0x05, 0xc3, 0xc1, 0xe3, 0xce, 0xb0, 0x0b, 0x1f, 0xec, 0x74, 0x08, 0x50, 0xd8, 0xe0, 0x98, 0x0b, 0xfe,
			0x24, 0x69, 0xaa, 0xd2, 0x25, 0xb5, 0xf3, 0xb9, 0x9b, 0xef, 0xc6, 0x85, 0xe0, 0x04, 0x64, 0xae, 0xb1, 0x43,
			0x79, 0x07, 0x6e, 0x73, 0x6c, 0xa1, 0x80, 0x60, 0xa7, 0x99, 0x05, 0x58, 0x19, 0xc7, 0xdc, 0x85, 0x53, 0xec,
			0xa9, 0x4f, 0x00, 0x3e, 0xcc, 0xf9, 0x0a, 0xfd, 0xef, 0x97, 0xf8, 0xcc, 0x2e, 0x2e, 0xe7, 0x1e, 0x33, 0x69,
			0x74, 0xd4, 0xb5, 0xb2, 0x28, 0x39, 0xb1, 0xa2, 0xaa, 0x5c, 0xf9, 0xa7, 0x85, 0xfc, 0x76, 0x42, 0x00, 0x7f,
			0x4e, 0xdf, 0xf9, 0x5c, 0x6c, 0xeb, 0xb3, 0x9d, 0xd5, 0x66, 0xe0, 0xbb, 0xd0, 0x47, 0x2a, 0x56, 0x4b, 0xdb,
			0x34, 0xa9, 0x1d, 0xa3, 0x10, 0x8f, 0x38, 0x8e, 0x73, 0xbf, 0x69, 0x22, 0x95, 0x84, 0xc6, 0x35, 0x8c, 0x67,
			0x33, 0x9b, 0x58, 0x9b, 0x5d, 0xe5, 0x73, 0x07, 0xe6, 0x1c, 0x81, 0xdd, 0xcb, 0x68, 0xba, 0xa6, 0x89, 0x46,
			0xa6, 0xa5, 0xf4, 0x75, 0x4b, 0x9f, 0xb7, 0x3e, 0x44, 0x73, 0xe3, 0x38, 0x19, 0xe7, 0x0c, 0x0c, 0xe5, 0x49,
			0xdb, 0xcb, 0xed, 0x57, 0x9d, 0x24, 0x4d, 0xfe, 0x25, 0xf5, 0xbd, 0x70, 0x80, 0x9b, 0x02, 0x7a, 0xc9, 0x13,
			0x0f, 0x1f, 0x6a, 0x7d, 0x9b, 0x9d, 0x27, 0xe8, 0x8e, 0x50, 0x8a, 0x51, 0x3d, 0xb3, 0x19, 0xdd, 0x50, 0xdf,
			0x98, 0xfa, 0x41, 0x29, 0xec, 0xa5, 0xe6, 0x38, 0x96, 0xb6, 0xdc, 0xdd, 0xc4, 0x67, 0x2c, 0x30, 0x78, 0xb1,
			0x3c, 0x24, 0x27, 0x4e, 0x9d, 0x88, 0x01, 0xc6, 0x96, 0x62, 0x13, 0x2a, 0x78, 0xc2, 0x3d, 0x29, 0xb1, 0xbd,
			0x16, 0xd4, 0x53, 0x7b, 0xec, 0xc2, 0x41, 0x8c, 0x5a, 0xad, 0x64, 0x60, 0x3b, 0x8b, 0x04, 0x12, 0x10, 0xae,
			0xa3, 0x52, 0x49, 0x3f, 0x3d, 0x89, 0xb4, 0x99, 0xcf, 0x12, 0xaf, 0x1f, 0xbd, 0x7e, 0xf3, 0x1c, 0x88, 0xaf,
			0x7e, 0x25, 0x20, 0xad, 0x58, 0x10, 0xcf, 0x32, 0x41, 0xef, 0x71, 0x27, 0x27, 0xe5, 0xe7, 0x5d, 0x32, 0x5b,
			0x33, 0xae, 0x2c, 0x8d, 0x04, 0x1f, 0xe5, 0xb2, 0x9e, 0x25, 0x3f, 0xa4, 0xa3, 0x40, 0x52, 0xab, 0x79, 0xdd,
			0x5a, 0x49, 0x10, 0xca, 0x77, 0x30, 0xe8, 0x79, 0x0d, 0x6d, 0x3f, 0x48, 0x38, 0x2d, 0x0b, 0xc0, 0x7a, 0x93,
			0xe8, 0x1a, 0x21, 0xfb, 0x42, 0x25, 0xa1, 0x7a, 0xf4, 0x36, 0x9a, 0x9c, 0x4f, 0x11, 0x10, 0x3c, 0xa2, 0xcc,
			0x56, 0x59, 0x8f, 0x18, 0xe9, 0x4f, 0xae, 0x0e, 0xc2, 0x35, 0x3e, 0x35, 0x61, 0xd7, 0xe3, 0x1e, 0xd6, 0xbb,
			0x33, 0x82, 0x26, 0x9c, 0x84, 0xe1, 0x37, 0xb8, 0x12, 0xc1, 0xcb, 0x7a, 0x36, 0x1e, 0xfc, 0x30, 0xc1, 0xe5,
			0x20, 0x5a, 0x61, 0xf0, 0xa6, 0xa5, 0xcb, 0x79, 0xa7, 0xcb, 0x4e, 0x9b, 0x1f, 0x3c, 0x70, 0xd6, 0xe4, 0xc8,
			0xd4, 0x57, 0xa9, 0x3c, 0xb0, 0x7e, 0x7e, 0xb5, 0x91, 0xdf, 0xba, 0x13, 0x70, 0x29, 0x84, 0x43, 0xed, 0x05,
			0x5b, 0xb5, 0x01, 0x21, 0x5c, 0x9a, 0x5f, 0x07, 0x7d, 0x04, 0x61, 0xb3, 0x63, 0x52, 0x81, 0x25, 0x59, 0x67,
			0xd5, 0x3c, 0x9a, 0xee, 0xb4, 0x5c, 0x84, 0x70, 0x68, 0xf5, 0xb6, 0x08, 0xbc, 0xce, 0xcd, 0x9d, 0x52, 0xa1,
			0x85, 0xad, 0xbf, 0xab, 0xb3, 0x0e, 0xdf, 0xbd, 0x0f, 0xfc, 0xa7, 0x6b, 0x28, 0xb9, 0x93, 0xff, 0x6c, 0x1f,
			0xa0, 0xaf, 0x70, 0x2e, 0x51, 0x7c, 0x39, 0xe1, 0x76, 0x6e, 0xb4, 0xed, 0x63, 0xf2, 0x58, 0xe8, 0x3d, 0xa1,
			0x68, 0xab, 0xe2, 0x2c, 0x02, 0x45, 0x58, 0x3d, 0x4d, 0xa4, 0x61, 0xe0, 0x3d, 0xb6, 0x18, 0x80, 0x02, 0xfd,
			0x12, 0x1b, 0x38, 0x5a, 0xb1, 0x73, 0x0b, 0x0b, 0x03, 0x4e, 0x8d, 0x3e, 0x21, 0xb7, 0xbb, 0x07, 0x3d, 0x20,
			0x7c, 0xbe, 0x04, 0x26, 0x7e, 0xab, 0x2e, 0xb2, 0x03, 0x0a, 0x69, 0xfc, 0x26, 0x80, 0xe1, 0x8b, 0xce, 0x52,
			0xa8, 0x11, 0xf8, 0xc1, 0xfd, 0xec, 0x55, 0x1e, 0xb5, 0xc0, 0x0c, 0x0d, 0x4f, 0x89, 0xd3, 0x86, 0x2f, 0xec,
			0x3e, 0x9c, 0x98, 0xeb, 0x3e, 0x1a, 0x56, 0x78, 0x61, 0xa7, 0x87, 0x3c, 0xe0, 0xdf, 0x0b, 0x00, 0x49, 0x83,
			0xce, 0x73, 0xe1, 0x0a, 0x3b, 0xf8, 0xdf, 0x59, 0x36, 0xf9, 0x45, 0x96, 0xc3, 0x2e, 0x93, 0xc3, 0xd0, 0x69,
			0x4a, 0x6a, 0xe8, 0x14, 0xb3, 0xad, 0x40, 0xdc, 0x1e, 0x46, 0x25, 0x65, 0x9a, 0x92, 0x3f, 0xb7, 0x59, 0x83,
			0x34, 0x83, 0xce, 0x32, 0xbb, 0x38, 0x6d, 0x32, 0xc8, 0xed, 0x3d, 0x21, 0x91, 0x90, 0xb3, 0xaf, 0x84, 0x85,
			0x28, 0x6c, 0xed, 0x44, 0x46, 0xba, 0x8e, 0x7a, 0x63, 0x69, 0x15, 0xc4, 0xd0, 0x48, 0xa7, 0xab, 0xa8, 0xab,
			0x59, 0x5e, 0x6a, 0xcd, 0x11, 0x07, 0x73, 0x21, 0xde, 0x18, 0x97, 0x5a, 0xba, 0x69, 0xab, 0x34, 0x32, 0xd2,
			0x9f, 0x89, 0xc4, 0xbe, 0x6c, 0x97, 0x1d, 0x0c, 0x11, 0x85, 0xf7, 0x59, 0x91, 0xce, 0xfa, 0xb3, 0x63, 0x41,
			0x6c, 0xdd, 0x45, 0xd6, 0x94, 0xf0, 0x7b, 0xa3, 0x2e, 0x68, 0x2d, 0x0e, 0xca, 0x25, 0x05, 0x85, 0x94, 0xbc,
			0x8c, 0x8e, 0x14, 0xd2, 0x07, 0xa2, 0x1e, 0xce, 0xe6, 0xa4, 0xcd, 0xd4, 0x0d, 0x9d, 0xc4, 0x66, 0x86, 0x25,
			0x01, 0x3a, 0x78, 0xda, 0x54, 0x6d, 0x12, 0x2b, 0x1d, 0x26, 0x38, 0x9a, 0x62, 0x22, 0xf3, 0x50, 0x8b, 0xb6,
			0xc9, 0x6b, 0xcc, 0xfe, 0x50, 0x55, 0x5b, 0x33, 0xac, 0x49, 0xd2, 0x70, 0xb6, 0x77, 0xa4, 0xff, 0x38, 0xae,
			0x4e, 0x05, 0x12, 0xc4, 0x78, 0xc2, 0x05, 0x19, 0x75, 0xe5, 0xba, 0xfa, 0xb2, 0x14, 0xa3, 0x1d, 0x5e, 0x23,
			0x54, 0xe0, 0x56, 0x3d, 0x55, 0xaf, 0xe5, 0x62, 0x77, 0x3d, 0x8b, 0x0a, 0xa8, 0x90, 0x36, 0x11, 0x96, 0x5c,
			0x5b, 0x7f, 0x84, 0x2b, 0x49, 0x0e, 0xff, 0xae, 0xb3, 0x83, 0x8d, 0x30, 0xc0, 0xaa, 0x84, 0xa2, 0x1b, 0x29,
			0x03, 0x40, 0x1e, 0x28, 0x30, 0x74, 0xc4, 0x21, 0x8e, 0xf7, 0xc4, 0x9e, 0x65, 0xd6, 0x62, 0x7b, 0x33, 0x7a,
			0x81, 0x8c, 0x5b, 0x2d, 0x3e, 0x8f, 0x31, 0xe8, 0xa7, 0x27, 0xf0, 0x77, 0x82, 0x09, 0x2c, 0xcf, 0x25, 0xf8,
			0x63, 0x29, 0xab, 0x9e, 0x10, 0x18, 0xc4, 0x97, 0xe4, 0x6e, 0x95, 0xb5, 0xd1, 0x97, 0x85, 0xe3, 0xf8, 0xe4,
			0xbb, 0x5f, 0x42, 0x6d, 0x75, 0x69, 0x96, 0x02, 0xe6, 0xab, 0x8b, 0x7b, 0x5d, 0xde, 0x65, 0x9e, 0xec, 0xdf,
			0xc3, 0x64, 0x6c, 0xe5, 0xa1, 0xd3, 0x0f, 0xcc, 0xae, 0x73, 0xb6, 0x92, 0x9b, 0xa7, 0xae, 0x84, 0xf0, 0xdf,
			0x99, 0xab, 0x17, 0x68, 0x57, 0x14, 0xda, 0x8c, 0x52, 0x75, 0x15, 0x8c, 0x65, 0x8d, 0x0c, 0x3c, 0x39, 0xc6,
			0x24, 0x57, 0xec, 0x92, 0xc9, 0xdb, 0xf7, 0x46, 0xa4, 0xe7, 0x39, 0x43, 0x27, 0x54, 0xf5, 0xba, 0x17, 0x2c,
			0x33, 0x12, 0xd7, 0x73, 0xf7, 0xf0, 0x25, 0x63, 0x1f, 0x87, 0x5b, 0x14, 0x92, 0xd6, 0xe5, 0xdc, 0xd2, 0x89,
			0xed, 0x50, 0x07, 0x3b, 0x83, 0xe3, 0x48, 0x5a, 0xf5, 0x32, 0x67, 0xeb, 0xca, 0x4a, 0x6e, 0xfd, 0xb0, 0xa1,
			0x75, 0x8f, 0x31, 0x80, 0x49, 0x27, 0x4d, 0xe7, 0xe6, 0x1e, 0x56, 0x45, 0xc8, 0x24, 0x83, 0x27, 0x65, 0xbe,
			0x23, 0x14, 0xc4, 0x02, 0x55, 0x07, 0xfd, 0x30, 0x11, 0x6b, 0x01, 0x15, 0xf6, 0x51, 0x00, 0x5b, 0xa5, 0xa4,
			0xb2, 0xe6, 0xe7, 0xad, 0xde, 0x47, 0xc9, 0xd8, 0x0c, 0x0b, 0x38, 0x33, 0xf4, 0x5c, 0x32, 0x44, 0x04, 0x77,
			0xff, 0x13, 0xe5, 0x17, 0xe0, 0x64, 0x98, 0x38, 0xe8, 0x2d, 0x38, 0xfb, 0x9f, 0x04, 0x3b, 0xf7, 0x34, 0x28,
			0x06, 0x6f, 0x7f, 0x35, 0xb2, 0xe5, 0xb4, 0xdb, 0x9f, 0xc4, 0x73, 0x43, 0x16, 0xcc, 0x63, 0x75, 0xea, 0x3d,
			0xa6, 0x1a, 0xf3, 0x34, 0x05, 0xd5, 0x73, 0x51, 0x0c, 0x79, 0xfd, 0xf9, 0xe2, 0xa0, 0x3d, 0x27, 0xff, 0x02,
			0xf8, 0xf5, 0x73, 0x64, 0xf4, 0xe5, 0xa8, 0xb7, 0x09, 0x11, 0x64, 0x88, 0x99, 0xea, 0x0e, 0xb2, 0x4c, 0x97,
			0x9e, 0xee, 0x99, 0x03, 0xf9, 0x70, 0xe9, 0xf3, 0x16, 0x6d, 0xca, 0x6f, 0x6c, 0x0a, 0x3f, 0xf8, 0x7c, 0xb2,
			0x21, 0xa3, 0x0a, 0x85, 0x3c, 0x5f, 0xea, 0xd1, 0x33, 0x3c, 0x43, 0x46, 0x1b, 0x82, 0x36, 0x51, 0xe6, 0x77,
			0x5e, 0x8a, 0x7c, 0x33, 0x24, 0x31, 0x9e, 0x6a, 0x41, 0xee, 0x1c, 0xde, 0x76, 0x6c, 0xa6, 0x1f, 0x00, 0x5a,
			0xdd, 0x25, 0xbc, 0xf6, 0x79, 0xde, 0x6c, 0xd6, 0x5b, 0xf0, 0x98, 0x42, 0x78, 0x13, 0x71, 0xee, 0x28, 0xb5,
			0x5b, 0xf2, 0x4a, 0x2f, 0x16, 0xb7, 0xc0, 0xc1, 0x8a, 0xbe, 0xf3, 0xa8, 0xdc, 0xa8, 0x17, 0x46, 0x5b, 0xa5,
			0xb0, 0x4e, 0xad, 0x92, 0x5e, 0xe3, 0x48, 0xd1, 0xa6, 0xf0, 0xe5, 0x16, 0xab, 0x05, 0x67, 0x83, 0x67, 0xe5,
			0xe7, 0x8c, 0xf2, 0x2d, 0x25, 0xf2, 0x62, 0xc5, 0xc8, 0xca, 0xb9, 0xdc, 0x66, 0x11, 0xbb, 0x7e, 0x08, 0x1a,
			0x0d, 0x2f, 0x80, 0x9c, 0xd2, 0xe3, 0x82, 0x5b, 0x69, 0x08, 0x81, 0x38, 0xdf, 0x1f, 0xf0, 0x20, 0xe0, 0x71,
			0x77, 0xc4, 0xc1, 0xdb, 0x7f, 0x3b, 0xa5, 0x69, 0x5e, 0x59, 0x84, 0x35, 0xac, 0x79, 0xd8, 0x83, 0xbd, 0xf7,
			0xc2, 0xd4, 0xa7, 0x26, 0x5c, 0x1e, 0x39, 0x60, 0x55, 0xd4, 0xf9, 0x01, 0xf8, 0xf4, 0x74, 0x62, 0x10, 0xfc,
			0xe4, 0xde, 0x88, 0x10, 0xc8, 0x3e, 0x98, 0x73, 0x89, 0x31, 0xc5, 0xd4, 0xec, 0x58, 0x64, 0x09, 0x13, 0x28,
			0xf3, 0xdc, 0xd6, 0xca, 0xe9, 0x60, 0xd4, 0x2e, 0x49, 0x1f, 0xcd, 0xc8, 0xc2, 0xe5, 0xc9, 0x0d, 0xf3, 0xa6,
			0xf4, 0x66, 0x0e, 0x6e, 0xb0, 0xcc, 0x6b, 0x61, 0x60, 0x10, 0x7a, 0xac, 0xe3, 0x68, 0xd8, 0xe2, 0x40, 0x5a,
			0xc5, 0xcf, 0x7f, 0x75, 0xd8, 0x19, 0x9c, 0x69, 0x0a, 0x6a, 0x3e, 0x6a, 0x21, 0xa6, 0x1b, 0xb1, 0xba, 0x36,
			0x23, 0xf3, 0x25, 0x8b, 0x4d, 0x52, 0xda, 0xf3, 0x38, 0x8f, 0x28, 0x35, 0xc7, 0xea, 0xdd, 0x85, 0x6a, 0x20,
			0xad, 0x34, 0x3e, 0xdc, 0x1e, 0x7b, 0x12, 0x33, 0x60, 0x04, 0x24, 0x9a, 0x53, 0x3d, 0xde, 0xd9, 0x0e, 0x06,
			0xc5, 0x40, 0xeb, 0xa1, 0x4c, 0xa6, 0xaf, 0x77, 0xa1, 0xb3, 0x50, 0xc4, 0x80, 0x37, 0x5f, 0x54, 0x39, 0x03,
			0x34, 0xea, 0x9e, 0x17, 0x20, 0x3e, 0x26, 0x18, 0x6e, 0xcc, 0xc3, 0x93, 0x6e, 0xca, 0xce, 0xe6, 0x1d, 0x10,
			0x8a, 0xef, 0x7e, 0x6d, 0xa0, 0x94, 0x1a, 0x65, 0x0b, 0x1a, 0x2b, 0xc0, 0x8b, 0x76, 0xf7, 0xe8, 0xe1, 0x0d,
			0x38, 0xd5, 0x8f, 0xaa, 0x22, 0x3b, 0x84, 0x78, 0x1f, 0x95, 0x25, 0x2b, 0x41, 0x98, 0x69, 0xf2, 0x3d, 0x84,
			0xd1, 0xd4, 0x36, 0x92, 0x52, 0x0b, 0x0e, 0x2d, 0x91, 0xe2, 0x89, 0xf2, 0x4b, 0x5a, 0x6d, 0x27, 0x93, 0xa7,
			0xd3, 0x4d, 0xbc, 0x65, 0xc2, 0x26, 0x8e, 0x3a, 0xb4, 0x4d, 0x4a, 0x13, 0x3a, 0x34, 0xa4, 0xeb, 0x02, 0xa5,
			0x15, 0xf2, 0x4c, 0xc0, 0x2c, 0x53, 0x2c, 0xe0, 0x81, 0x70, 0xd3, 0x74, 0x10, 0x81, 0x76, 0xd3, 0x4e, 0x16,
			0x94, 0x30, 0x15, 0x90, 0x9d, 0x58, 0xbc, 0xd0, 0xfa, 0x45, 0x1f, 0x7e, 0x4e, 0xcc, 0x01, 0x74, 0xdc, 0xbc,
			0x23, 0x4b, 0xca, 0x05, 0x5b, 0xc6, 0xdd, 0xfd, 0x1c, 0xf4, 0x98, 0xce, 0xce, 0x44, 0x92, 0xb9, 0xea, 0xc4,
			0xf9, 0x16, 0x68, 0x90, 0x62, 0x5c, 0xe9, 0xd5, 0x9b, 0xd4, 0x6e, 0xad, 0xb9, 0x8f, 0x10, 0x5d, 0xaf, 0x5f,
			0x23, 0xd0 };
}
