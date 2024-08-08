/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql.size.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.community.dialect.SingleStoreDialect;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-14585")
public class WhereAnnotatedOneToManySizeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Region.class, City.class };
	}

	@Before
	public void before() {
		Region lazio = new Region();
		lazio.setId( 1 );
		lazio.setName( "Lazio" );

		Region lombardy = new Region();
		lombardy.setId( 2 );
		lombardy.setName( "Lombardy" );

		City rome = new City();
		rome.setId( 1 );
		rome.setName( "Rome" );
		rome.setDeleted( false );
		rome.setRegion( lazio );

		City gradoli = new City();
		gradoli.setId( 2 );
		gradoli.setName( "Gradoli" );
		gradoli.setDeleted( true );
		gradoli.setRegion( lazio );

		City milan = new City();
		milan.setId( 3 );
		milan.setName( "Milan" );
		milan.setDeleted( false );
		milan.setRegion( lombardy );

		City pavia = new City();
		pavia.setId( 4 );
		pavia.setName( "Pavia" );
		pavia.setDeleted( false );
		pavia.setRegion( lombardy );

		lazio.getCities().add( rome );
		lazio.getCities().add( gradoli );

		lombardy.getCities().add( milan );
		lombardy.getCities().add( pavia );

		inTransaction( session -> {
			session.persist( lazio );
			session.persist( lombardy );

			session.persist( rome );
			session.persist( gradoli );
			session.persist( milan );
			session.persist( pavia );
		} );
	}

	@After
	public void after() {
		inTransaction( session -> {
			session.createNativeQuery( "DELETE FROM City" ).executeUpdate();
			session.createQuery( "DELETE FROM Region c" ).executeUpdate();
		} );
	}

	@Test
	@SkipForDialect(value = SingleStoreDialect.class, comment = "SingleStore does not support correlated subqueries in the ORDER BY clause")
	@SkipForDialect(value = DB2Dialect.class, comment = "DB2 does not support correlated subqueries in the ORDER BY clause")
	@SkipForDialect(value = AbstractHANADialect.class, comment = "HANA db does not support correlated subqueries in the ORDER BY clause")
	@SkipForDialect(value = TiDBDialect.class, comment = "TiDB db does not support correlated subqueries in the ORDER BY clause")
	@SkipForDialect(value = SybaseDialect.class, comment = "Sybase db does not support subqueries in the ORDER BY clause")
	public void orderBy_sizeOf() {
		inSession( session -> {
			QueryImplementor<Object[]> query = session.createQuery(
					"select r, size(r.cities) from Region r order by size(r.cities) desc" );
			List<Object[]> result = query.getResultList();
			assertThat( result ).extracting( f -> f[0] ).extracting( "name" ).containsExactly( "Lombardy", "Lazio" );
			assertThat( result ).extracting( f -> f[1] ).containsExactly( 2, 1 );
		} );
	}

	@Test
	public void project_sizeOf() {
		inSession( session -> {
			QueryImplementor<Integer> query = session.createQuery(
					"SELECT size(r.cities) FROM Region r", Integer.class );
			List<Integer> cityCounts = query.getResultList();
			assertThat( cityCounts ).containsExactlyInAnyOrder( 1, 2 );
		} );
	}

}
