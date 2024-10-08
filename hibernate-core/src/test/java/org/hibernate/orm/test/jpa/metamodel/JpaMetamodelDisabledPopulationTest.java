/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.metamodel;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12871")
public class JpaMetamodelDisabledPopulationTest extends AbstractJpaMetamodelPopulationTest {
	@Override
	protected String getJpaMetamodelPopulationValue() {
		return "disabled";
	}
}
