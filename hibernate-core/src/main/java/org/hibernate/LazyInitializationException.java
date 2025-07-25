/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Indicates an attempt to access unfetched data outside the context
 * of an open stateful {@link Session}.
 * <p>
 * For example, this exception occurs when an uninitialized proxy or
 * collection is accessed after the session was closed.
 *
 * @see Hibernate#initialize(Object)
 * @see Hibernate#isInitialized(Object)
 * @see StatelessSession#fetch(Object)
 *
 * @author Gavin King
 */
public class LazyInitializationException extends HibernateException {

	/**
	 * Constructs a {@code LazyInitializationException} using the given message.
	 *
	 * @param message A message explaining the exception condition
	 */
	public LazyInitializationException(String message) {
		super( message );
	}

}
