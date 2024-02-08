/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.core.userdetails;

import java.io.IOException;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class UserDetailsMarshaller implements ProtoStreamMarshaller<User> {

	private static final int USERNAME_INDEX = 1;
	private static final int PASSWORD_INDEX = 2;
	private static final int AUTHORITY_INDEX = 3;
	private static final int FLAGS_INDEX = 4;

	private static final int DISABLED = 0;
	private static final int ACCOUNT_EXPIRED = 1;
	private static final int CREDENTIALS_EXPIRED = 2;
	private static final int ACCOUNT_LOCKED = 3;
	private static final int FLAGS = 4;

	@Override
	public User readFrom(ProtoStreamReader reader) throws IOException {
		User.UserBuilder builder = User.builder().password("");
		List<GrantedAuthority> authorities = new LinkedList<>();
		while (!reader.isAtEnd()) {
			int tag = reader.readTag();
			switch (WireType.getTagFieldNumber(tag)) {
				case USERNAME_INDEX:
					builder.username(reader.readString());
					break;
				case PASSWORD_INDEX:
					builder.password(reader.readString());
					break;
				case AUTHORITY_INDEX:
					authorities.add(reader.readAny(GrantedAuthority.class));
					break;
				case FLAGS:
					BitSet flags = reader.readObject(BitSet.class);
					builder.disabled(flags.get(DISABLED));
					builder.accountExpired(flags.get(ACCOUNT_EXPIRED));
					builder.credentialsExpired(flags.get(CREDENTIALS_EXPIRED));
					builder.accountLocked(flags.get(ACCOUNT_LOCKED));
					break;
				default:
					reader.skipField(tag);
			}
		}
		if (!authorities.isEmpty()) {
			builder.authorities(authorities);
		}
		return (User) builder.build();
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, User user) throws IOException {
		writer.writeString(USERNAME_INDEX, user.getUsername());
		String password = user.getPassword();
		if ((password != null) && !password.isEmpty()) {
			writer.writeString(PASSWORD_INDEX, password);
		}
		for (GrantedAuthority authority : user.getAuthorities()) {
			writer.writeAny(AUTHORITY_INDEX, authority);
		}
		BitSet flags = new BitSet(FLAGS);
		if (!user.isEnabled()) {
			flags.set(DISABLED);
		}
		if (!user.isAccountNonExpired()) {
			flags.set(ACCOUNT_EXPIRED);
		}
		if (!user.isCredentialsNonExpired()) {
			flags.set(CREDENTIALS_EXPIRED);
		}
		if (!user.isAccountNonLocked()) {
			flags.set(ACCOUNT_LOCKED);
		}
		if (!flags.isEmpty()) {
			writer.writeObject(FLAGS_INDEX, flags);
		}
	}

	@Override
	public Class<? extends User> getJavaClass() {
		return User.class;
	}
}
