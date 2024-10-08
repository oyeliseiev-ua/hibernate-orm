/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Chris Cranford
 */
@JiraKey( value = "HHH-8058" )
public class EntityWithChangesQueryTest extends AbstractEntityWithChangesQueryTest {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, Boolean.TRUE );
	}
}
