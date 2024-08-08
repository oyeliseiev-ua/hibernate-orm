package org.hibernate.orm.test.timezones;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.SingleStoreDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.type.descriptor.DateTimeUtils;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SkipForDialect(value = SingleStoreDialect.class, comment = "SingleStore doesn't support zoned timestamp")
@DomainModel(annotatedClasses = AutoZonedTest.Zoned.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "AUTO"))
public class AutoZonedTest {

	@Test void test(SessionFactoryScope scope) {
		final ZonedDateTime nowZoned;
		final OffsetDateTime nowOffset;
		if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof SybaseDialect ) {
			// Sybase has 1/300th sec precision
			nowZoned = ZonedDateTime.now().withZoneSameInstant( ZoneId.of("CET") )
					.with( ChronoField.NANO_OF_SECOND, 0L );
			nowOffset = OffsetDateTime.now().withOffsetSameInstant( ZoneOffset.ofHours(3) )
					.with( ChronoField.NANO_OF_SECOND, 0L );
		}
		else {
			nowZoned = ZonedDateTime.now().withZoneSameInstant( ZoneId.of("CET") );
			nowOffset = OffsetDateTime.now().withOffsetSameInstant( ZoneOffset.ofHours(3) );
		}
		long id = scope.fromTransaction( s-> {
			Zoned z = new Zoned();
			z.zonedDateTime = nowZoned;
			z.offsetDateTime = nowOffset;
			s.persist(z);
			return z.id;
		});
		scope.inSession( s -> {
			Zoned z = s.find(Zoned.class, id);
			final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
			assertEquals(
					DateTimeUtils.roundToDefaultPrecision( nowZoned.toInstant(), dialect ),
					DateTimeUtils.roundToDefaultPrecision( z.zonedDateTime.toInstant(), dialect )
			);
			assertEquals(
					DateTimeUtils.roundToDefaultPrecision( nowOffset.toInstant(), dialect ),
					DateTimeUtils.roundToDefaultPrecision( z.offsetDateTime.toInstant(), dialect )
			);
			assertEquals( nowZoned.toOffsetDateTime().getOffset(), z.zonedDateTime.toOffsetDateTime().getOffset() );
			assertEquals( nowOffset.getOffset(), z.offsetDateTime.getOffset() );
		});
	}

	@Entity
	public static class Zoned {
		@Id
		@GeneratedValue Long id;
		ZonedDateTime zonedDateTime;
		OffsetDateTime offsetDateTime;
	}
}
