/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.community.dialect.SingleStoreDialect;
import org.hibernate.orm.test.jpa.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.orm.test.jpa.metamodel.Customer;
import org.hibernate.orm.test.jpa.metamodel.Customer_;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Order_;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class UncorrelatedSubqueryTest extends AbstractMetamodelSpecificTest {
	@Test
	public void testGetCorrelatedParentIllegalStateException() {
		// test that attempting to call getCorrelatedParent on a uncorrelated query/subquery
		// throws ISE

		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaQuery<Customer> criteria = builder.createQuery( Customer.class );
		Root<Customer> customerRoot = criteria.from( Customer.class );
		Join<Customer, Order> orderJoin = customerRoot.join( Customer_.orders );
		criteria.select( customerRoot );
		Subquery<Double> subCriteria = criteria.subquery( Double.class );
		Root<Order> subqueryOrderRoot = subCriteria.from( Order.class );
		subCriteria.select( builder.min( subqueryOrderRoot.get( Order_.totalPrice ) ) );
		criteria.where( builder.equal( orderJoin.get( "totalPrice" ), builder.all( subCriteria ) ) );

		assertFalse( customerRoot.isCorrelated() );
		assertFalse( subqueryOrderRoot.isCorrelated() );

		try {
			customerRoot.getCorrelationParent();
			fail( "Should have resulted in IllegalStateException" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			subqueryOrderRoot.getCorrelationParent();
			fail( "Should have resulted in IllegalStateException" );
		}
		catch (IllegalStateException expected) {
		}

		em.getTransaction().commit();
		em.close();
	}

	@SkipForDialect( dialectClass = SingleStoreDialect.class, reason = "SingleStore doesn't support ALL clause")
	@Test
	public void testEqualAll() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaQuery<Customer> criteria = builder.createQuery( Customer.class );
		Root<Customer> customerRoot = criteria.from( Customer.class );
		Join<Customer, Order> orderJoin = customerRoot.join( Customer_.orders );
		criteria.select( customerRoot );
		Subquery<Double> subCriteria = criteria.subquery( Double.class );
		Root<Order> subqueryOrderRoot = subCriteria.from( Order.class );
		subCriteria.select( builder.min( subqueryOrderRoot.get( Order_.totalPrice ) ) );
		criteria.where( builder.equal( orderJoin.get( "totalPrice" ), builder.all( subCriteria ) ) );
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testLessThan() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaQuery<Customer> criteria = builder.createQuery( Customer.class );
		Root<Customer> customerRoot = criteria.from( Customer.class );

		Subquery<Double> subCriteria = criteria.subquery( Double.class );
		Root<Customer> subQueryCustomerRoot = subCriteria.from( Customer.class );
		subCriteria.select( builder.avg( subQueryCustomerRoot.get( Customer_.age ) ) );

		criteria.where(
				builder.lessThan(
						customerRoot.get( Customer_.age ),
						subCriteria.getSelection().as( Integer.class )
				)
		);
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}
}
