/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.schemagen;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Vlad MIhalcea
 */
@RequiresDialect( H2Dialect.class )
@JiraKey( value = "HHH-10972" )
public class JpaFileSchemaGeneratorTest extends JpaSchemaGeneratorTest {

	protected String getLoadSqlScript() {
		return toFilePath(super.getLoadSqlScript());
	}

	protected String getCreateSqlScript() {
		return toFilePath(super.getCreateSqlScript());
	}

	protected String getDropSqlScript() {
		return toFilePath(super.getDropSqlScript());
	}

	@Override
	protected String getResourceUrlString(String resource) {
		return resource;
	}
}