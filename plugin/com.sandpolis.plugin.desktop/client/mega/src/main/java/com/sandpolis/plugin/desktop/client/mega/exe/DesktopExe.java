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
package com.sandpolis.plugin.desktop.client.mega.exe;

import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.failure;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.net.stream.OutboundStreamAdapter;
import com.sandpolis.plugin.desktop.MessageDesktop.DesktopMSG;
import com.sandpolis.plugin.desktop.MsgDesktop.RQ_Screenshot;
import com.sandpolis.plugin.desktop.MsgDesktop.RS_Screenshot;
import com.sandpolis.plugin.desktop.MsgRd.EV_DesktopStream;
import com.sandpolis.plugin.desktop.MsgRd.RQ_DesktopStream;
import com.sandpolis.plugin.desktop.client.mega.JavaDesktopSource;

public final class DesktopExe extends Exelet {

	@Auth
	@Handler(tag = DesktopMSG.RQ_SCREENSHOT_FIELD_NUMBER)
	public static MessageOrBuilder rq_screenshot(RQ_Screenshot rq) {
		var outcome = begin();

		try (var out = new ByteArrayOutputStream()) {
			BufferedImage screenshot = new Robot()
					.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			ImageIO.write(screenshot, "jpg", out);

			return RS_Screenshot.newBuilder().setData(ByteString.copyFrom(out.toByteArray()));
		} catch (Exception e) {
			return failure(outcome);
		}
	}

	@Auth
	@Handler(tag = DesktopMSG.RQ_DESKTOP_STREAM_FIELD_NUMBER)
	public static MessageOrBuilder rq_desktop_stream(ExeletContext context, RQ_DesktopStream rq) {
		var outcome = begin();
		// TODO use correct stream ID
		// Stream stream = new Stream();

		context.defer(() -> {
			var source = new JavaDesktopSource();
			var outbound = new OutboundStreamAdapter<EV_DesktopStream>(rq.getId(), context.connector,
					context.request.getFrom(), ev -> {
						return Any.pack(DesktopMSG.newBuilder().setEvDesktopStream(ev).build(),
								"com.sandpolis.plugin.desktop");
					});
			StreamStore.add(source, outbound);
			source.start();
		});

		return success(outcome);
	}

	@Auth
	@Handler(tag = DesktopMSG.EV_DESKTOP_STREAM_FIELD_NUMBER)
	public static void ev_desktop_stream(ExeletContext context, EV_DesktopStream ev) {
		StreamStore.streamData(context.request.getId(), ev);
	}

	private DesktopExe() {
	}
}
