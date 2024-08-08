/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tm;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.SingleStoreDialect;
import org.hibernate.orm.test.resource.transaction.jta.JtaPlatformStandardTestingImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class JtaBeforeCompletionFailureTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		TestingJtaBootstrap.prepare( builder.getSettings() );
		builder.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session ->
						session.save( newEntity( 1 ) )
		);

	}

	@AfterEach
	public void tearDown() {
		inTransaction(
				session ->
						session.createQuery( "delete SimpleEntity" ).executeUpdate()
		);
	}

	@SkipForDialect( dialectClass = SingleStoreDialect.class, reason = "SingleStore unique keys are restricted")
	@Test
	@TestForIssue(jiraKey = "HHH-9888")
	public void testUniqueConstraintViolationDuringManagedFlush() throws Exception {
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		// begin the transaction ("CMT" style)
		tm.begin();

		try (Session session = sessionFactory().openSession()) {

			session.save( newEntity( 2 ) );

			// complete the transaction ("CMT" style) - this leads to the managed flush
			// which should lead to the UK violation
			try {
				tm.commit();
				fail( "Expecting a failure from JTA commit" );
			}
			catch (RollbackException expected) {
				boolean violationExceptionFound = false;
				Throwable cause = expected;
				while ( cause != null ) {
					if ( cause instanceof JDBCException ) {
						violationExceptionFound = true;
						break;
					}
					cause = cause.getCause();
				}

				if ( !violationExceptionFound ) {
					fail( "Did not find JDBCException in JTA RollbackException chain" );
				}
			}
		}
	}

	private SimpleEntity newEntity(int id) {
		// since "key" is reused, should violate the UK
		return new SimpleEntity( id, "key", "name" );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		public Integer id;
		@Column(unique = true, nullable = false, name = "entity_key")
		public String key;
		public String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String key, String name) {
			this.id = id;
			this.key = key;
			this.name = name;
		}
	}
}
