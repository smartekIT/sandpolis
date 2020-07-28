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
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.foundation.Result.ErrorCode.ACCESS_DENIED;
import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.failure;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.core.instance.profile.ProfileStore.ProfileStore;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.foundation.util.CryptoUtil;
import com.sandpolis.core.foundation.util.ValidationUtil;
import com.sandpolis.core.instance.profile.Profile;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.sv.msg.MsgLogin.RQ_Login;
import com.sandpolis.core.sv.msg.MsgLogin.RQ_Logout;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * This {@link Exelet} handles login and logout requests from viewer instances.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class LoginExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(LoginExe.class);

	@Handler(auth = true)
	public static void rq_logout(ExeletContext context, RQ_Logout rq) {
		log.debug("Processing logout request from: {}", context.connector.getRemoteAddress());
		context.connector.close();
	}

	@Handler(auth = false)
	public static MessageOrBuilder rq_login(ExeletContext context, RQ_Login rq) {
		log.debug("Processing login request from: {}", context.connector.getRemoteAddress());
		var outcome = begin();

		// Validate username
		String username = rq.getUsername();
		if (!ValidationUtil.username(username)) {
			log.debug("The username ({}) is invalid", username);
			return failure(outcome, INVALID_USERNAME);
		}

		User user = UserStore.getByUsername(username).orElse(null);
		if (user == null) {
			log.debug("The user ({}) does not exist", username);
			return failure(outcome, ACCESS_DENIED);
		}

		// Check expiration
		if (user.isExpired()) {
			log.debug("The user ({}) is expired", username);
			return failure(outcome, ACCESS_DENIED);
		}

		// Check password
		if (!CryptoUtil.PBKDF2.check(rq.getPassword(), user.getHash())) {
			log.debug("Authentication failed", username);
			return failure(outcome, ACCESS_DENIED);
		}

		log.debug("Accepting login request for user: {}", username);

		// Mark connection as authenticated
		context.connector.authenticate();

		// Update login metadata
		Profile profile = ProfileStore.getViewer(username).orElse(null);
		if (profile == null) {
			// Build new profile
			profile = new Profile(context.connector.getRemoteUuid(), context.connector.getRemoteInstance(),
					context.connector.getRemoteInstanceFlavor());
			profile.instance().viewer().username().set(username);
			ProfileStore.add(profile);
		}

		profile.instance().viewer().ip().set(context.connector.getRemoteAddress());

		return success(outcome);
	}

	private LoginExe() {
	}
}
