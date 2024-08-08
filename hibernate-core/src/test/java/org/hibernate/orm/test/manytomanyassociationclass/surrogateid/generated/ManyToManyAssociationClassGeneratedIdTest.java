/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.manytomanyassociationclass.surrogateid.generated;

import java.util.HashSet;
import jakarta.persistence.PersistenceException;

import org.hibernate.community.dialect.SingleStoreDialect;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.hibernate.orm.test.manytomanyassociationclass.AbstractManyToManyAssociationClassTest;
import org.hibernate.orm.test.manytomanyassociationclass.Membership;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests on many-to-many association using an association class with a surrogate ID that is generated.
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/manytomanyassociationclass/surrogateid/generated/Mappings.hbm.xml"
)
@SkipForDialect( dialectClass = SingleStoreDialect.class)
public class ManyToManyAssociationClassGeneratedIdTest extends AbstractManyToManyAssociationClassTest {
	@Override
	public Membership createMembership(String name) {
		return new Membership( name );
	}

	@Test
	public void testRemoveAndAddEqualElement(SessionFactoryScope scope) {
		deleteMembership( getUser(), getGroup(), getMembership() );
		addMembership( getUser(), getGroup(), createMembership( "membership" ) );

		scope.inSession(
				session -> {
					try {
						session.getTransaction().begin();
						// The new membership is transient (it has a null surrogate ID), so
						// Hibernate assumes that it should be added to the collection.
						// Inserts are done before deletes, so a ConstraintViolationException
						// will be thrown on the insert because the unique constraint on the
						// user and group IDs in the join table is violated. See HHH-2801.
						session.merge( getUser() );
						session.getTransaction().commit();
						fail( "should have failed because inserts are before deletes" );
					}
					catch (Exception e) {
						session.getTransaction().rollback();
						// expected
						assertTyping( ConstraintViolationException.class, e );
					}
				}
		);
	}

	@Test
	public void testRemoveAndAddEqualCollection(SessionFactoryScope scope) {
		deleteMembership( getUser(), getGroup(), getMembership() );
		getUser().setMemberships( new HashSet() );
		getGroup().setMemberships( new HashSet() );
		addMembership( getUser(), getGroup(), createMembership( "membership" ) );

		scope.inSession(
				session -> {
					try {
						session.getTransaction().begin();
						// The new membership is transient (it has a null surrogate ID), so
						// Hibernate assumes that it should be added to the collection.
						// Inserts are done before deletes, so a ConstraintViolationException
						// will be thrown on the insert because the unique constraint on the
						// user and group IDs in the join table is violated. See HHH-2801.
						session.merge( getUser() );
						session.getTransaction().commit();
						fail( "should have failed because inserts are before deletes" );
					}
					catch (PersistenceException e) {
						session.getTransaction().rollback();
						// expected
						assertTyping( ConstraintViolationException.class, e );
					}
				}
		);
	}

	@Test
	public void testRemoveAndAddEqualElementNonKeyModified(SessionFactoryScope scope) {
		deleteMembership( getUser(), getGroup(), getMembership() );
		Membership membershipNew = createMembership( "membership" );
		addMembership( getUser(), getGroup(), membershipNew );
		membershipNew.setName( "membership1" );

		scope.inSession(
				session -> {
					try {
						session.getTransaction().begin();
						// The new membership is transient (it has a null surrogate ID), so
						// Hibernate assumes that it should be added to the collection.
						// Inserts are done before deletes, so a ConstraintViolationException
						// will be thrown on the insert because the unique constraint on the
						// user and group IDs in the join table is violated. See HHH-2801.
						session.merge( getUser() );
						session.getTransaction().commit();
						fail( "should have failed because inserts are before deletes" );
					}
					catch (PersistenceException e) {
						session.getTransaction().rollback();
						// expected
						assertTyping( ConstraintViolationException.class, e );
					}
				}
		);
	}
}
