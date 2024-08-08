/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql.set;

import java.util.List;

import org.hibernate.community.dialect.SingleStoreDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.SemanticException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfLists;
import org.hibernate.testing.orm.domain.gambit.EntityWithManyToOneSelfReference;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Christian Beikov
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory
public class SetOperationTest {
    @BeforeEach
    public void createTestData(SessionFactoryScope scope) {
        scope.inTransaction(
                session -> {
                    session.save( new EntityOfLists( 1, "first" ) );
                    session.save( new EntityOfLists( 2, "second" ) );
                    session.save( new EntityOfLists( 3, "third" ) );
                    EntityWithManyToOneSelfReference first = new EntityWithManyToOneSelfReference( 1, "first", 123 );
                    EntityWithManyToOneSelfReference second = new EntityWithManyToOneSelfReference( 2, "second", 123 );
                    session.save( first );
                    first.setOther( first );
                    session.save( second );
                    second.setOther( second );
                }
        );
    }

    @AfterEach
    public void dropTestData(SessionFactoryScope scope) {
        scope.inTransaction(
                session -> {
                    // Because, why not MySQL/MariaDB... https://bugs.mysql.com/bug.php?id=7412
                    session.createQuery( "update EntityWithManyToOneSelfReference set other = null" ).executeUpdate();
                    session.createQuery( "delete from EntityWithManyToOneSelfReference" ).executeUpdate();
                    session.createQuery( "delete from EntityOfLists" ).executeUpdate();
                    session.createQuery( "delete from SimpleEntity" ).executeUpdate();
                }
        );
    }

    @Test
    @TestForIssue( jiraKey = "HHH-14704")
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    public void testUnionAllWithManyToOne(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    List<EntityWithManyToOneSelfReference> list = session.createQuery(
                            "from EntityWithManyToOneSelfReference e where e.id = 1 " +
                                    "union all " +
                                    "from EntityWithManyToOneSelfReference e where e.id = 2",
                            EntityWithManyToOneSelfReference.class
                    ).list();
                    assertThat( list.size(), is( 2 ) );
                }
        );
    }

    @Test
    @TestForIssue( jiraKey = "HHH-14704")
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    public void testUnionAllWithManyToOneFetch(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    List<EntityWithManyToOneSelfReference> list = session.createQuery(
                            "from EntityWithManyToOneSelfReference e join fetch e.other where e.id = 1 " +
                                    "union all " +
                                    "from EntityWithManyToOneSelfReference e join fetch e.other where e.id = 2",
                            EntityWithManyToOneSelfReference.class
                    ).list();
                    assertThat( list.size(), is( 2 ) );
                }
        );
    }

    @Test
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    public void testUnionAllWithManyToOneFetchJustOne(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    try {
                        session.createQuery(
                                "from EntityWithManyToOneSelfReference e join fetch e.other where e.id = 1 " +
                                        "union all " +
                                        "from EntityWithManyToOneSelfReference e where e.id = 2",
                                EntityWithManyToOneSelfReference.class
                        );
                        Assertions.fail( "Expected exception to be thrown!" );
                    }
                    catch (Exception e) {
                        Assertions.assertEquals( IllegalArgumentException.class, e.getClass() );
                        Assertions.assertEquals( SemanticException.class, e.getCause().getClass() );
                    }
                }
        );
    }

    @Test
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    public void testUnionAllWithManyToOneFetchDifferentAttributes(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    try {
                        session.createQuery(
                                "from EntityOfLists e join fetch e.listOfOneToMany where e.id = 1 " +
                                        "union all " +
                                        "from EntityOfLists e join fetch e.listOfManyToMany where e.id = 2",
                                EntityOfLists.class
                        );
                        Assertions.fail( "Expected exception to be thrown!" );
                    }
                    catch (Exception e) {
                        Assertions.assertEquals( IllegalArgumentException.class, e.getClass() );
                        Assertions.assertEquals( SemanticException.class, e.getCause().getClass() );
                    }
                }
        );
    }

    @Test
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    public void testUnionAllWithManyToOneFetchDifferentOrder(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    try {
                        session.createQuery(
                                "from EntityOfLists e join fetch e.listOfOneToMany join fetch e.listOfManyToMany where e.id = 1 " +
                                        "union all " +
                                        "from EntityOfLists e join fetch e.listOfManyToMany join fetch e.listOfOneToMany where e.id = 2",
                                EntityOfLists.class
                        );
                        Assertions.fail( "Expected exception to be thrown!" );
                    }
                    catch (Exception e) {
                        Assertions.assertEquals( IllegalArgumentException.class, e.getClass() );
                        Assertions.assertEquals( SemanticException.class, e.getCause().getClass() );
                    }
                }
        );
    }

    @Test
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    public void testUnionAll(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    List<EntityOfLists> list = session.createQuery(
                            "select e from EntityOfLists e where e.id = 1 " +
                                    "union all " +
                                    "select e from EntityOfLists e where e.id = 2",
                            EntityOfLists.class
                    ).list();
                    assertThat( list.size(), is( 2 ) );
                }
        );
    }

    @Test
    @SkipForDialect( dialectClass = SingleStoreDialect.class, reason = "SingleStore doesn't support UNION/UNION ALL with limit clause")
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    public void testUnionAllLimit(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    List<Tuple> list = session.createQuery(
                            "(select e.id, e from EntityOfLists e where e.id = 1 " +
                                    "union all " +
                                    "select e.id, e from EntityOfLists e where e.id = 2) " +
                                    "order by 1 fetch first 1 row only",
                            Tuple.class
                    ).list();
                    assertThat( list.size(), is( 1 ) );
                }
        );
    }

    @Test
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInSubquery.class)
    public void testUnionAllLimitSubquery(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    List<Tuple> list = session.createQuery(
                            "select e.id, e from EntityOfLists e where e.id = 1 " +
                                    "union all " +
                                    "select e.id, e from EntityOfLists e where e.id = 2 " +
                                    "order by 1 fetch first 1 row only",
                            Tuple.class
                    ).list();
                    assertThat( list.size(), is( 2 ) );
                }
        );
    }

    @Test
    @SkipForDialect( dialectClass = SingleStoreDialect.class, reason = "SingleStore doesn't support UNION/UNION ALL with limit clause")
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInSubquery.class)
    public void testUnionAllLimitNested(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    List<Tuple> list = session.createQuery(
                            "(select e.id, e from EntityOfLists e where e.id = 1 " +
                                    "union all " +
                                    "(select e.id, e from EntityOfLists e where e.id = 2 order by 1 fetch first 1 row only)) " +
                                    "order by 1 fetch first 1 row only",
                            Tuple.class
                    ).list();
                    assertThat( list.size(), is( 1 ) );
                }
        );
    }

    @Test
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    @SkipForDialect( dialectClass = SingleStoreDialect.class, reason = "SingleStore doesn't support UNION/UNION ALL with limit clause")
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInSubquery.class)
    @SkipForDialect(dialectClass = OracleDialect.class, reason = "Bug in BasicFormatterImpl causes exception during formatting of the SQL string")
    public void testAlternatingSetOperator(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    List<Integer> list = session.createQuery(
                            "(SELECT e.id FROM EntityOfLists e WHERE e.id = 1\n"
                                    + "UNION\n"
                                    + "SELECT e.id FROM EntityOfLists e WHERE e.id = 2\n"
                                    + "UNION ALL\n"
                                    + "SELECT e.id FROM EntityOfLists e WHERE e.id = 3)\n"
                                    + "ORDER BY 1 DESC"
                                    + " LIMIT 1",
                            Integer.class
                    ).list();
                    assertThat( list.size(), is( 1 ) );
                }
        );
    }

    @Test
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
    @SkipForDialect( dialectClass = SingleStoreDialect.class, reason = "SingleStore doesn't support UNION/UNION ALL with limit clause")
    @RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInSubquery.class)
    public void testUnionAllOrderByAttribute(SessionFactoryScope scope) {
        scope.inSession(
                session -> {
                    List<EntityOfLists> list = session.createQuery(
                            "(SELECT e FROM EntityOfLists e WHERE e.id = 1\n"
                                    + "UNION ALL\n"
                                    + "SELECT e FROM EntityOfLists e WHERE e.id = 2)\n"
                                    + "ORDER BY name DESC"
                                    + " LIMIT 1",
                            EntityOfLists.class
                    ).list();
                    assertThat( list.size(), is( 1 ) );
                }
        );
    }
}
