/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/*
 * @author gajendra.jatav(raaz2.gajendra@gmail.com)
 */
public class BatchSortingTest extends BaseInsertOrderingTest {

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				GeoCountry.class, GeoDistrict.class,
				GeoDistrictDetail.class, GeoNation.class
		};
	}

	@Test
	@JiraKey(value = "HHH-13410")
	public void batchInsertTest() {

		sessionFactoryScope().inTransaction( session -> {
			GeoCountry country = new GeoCountry();
			GeoDistrict geoDistrict = new GeoDistrict();
			geoDistrict.setDistrictName( "SomeDistrict" );
			GeoDistrictDetail districtDetail = new GeoDistrictDetail();
			geoDistrict.setGeoDistrictDetail( districtDetail );
			GeoNation nation = new GeoNation();
			nation.setNationName( "NationName" );
			country.setCountryName( "CountryName" );
			session.persist( country );
			List<GeoDistrict> geoDistricts = new ArrayList<>();
			geoDistrict.setCountryId( country.getId() );
			geoDistricts.add( geoDistrict );
			country.setDistricts( geoDistricts );
			session.persist( geoDistrict );
			session.persist( nation );
			clearBatches();
		} );

		verifyPreparedStatementCount( 4 );
	}

	@Entity(name = "GeoCountry")
	public static class GeoCountry {

		@GeneratedValue
		@Id
		private Long id;

		@OneToMany(fetch = FetchType.LAZY)
		@JoinColumn(name = "COUNTRY_ID", referencedColumnName = "ID", updatable = false, insertable = false)
		private List<GeoDistrict> districts;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "NATION_ID")
		private GeoNation nation;

		private String countryName;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<GeoDistrict> getDistricts() {
			return districts;
		}

		public void setDistricts(List<GeoDistrict> districts) {
			this.districts = districts;
		}

		public GeoNation getNation() {
			return nation;
		}

		public void setNation(GeoNation nation) {
			this.nation = nation;
		}

		public String getCountryName() {
			return countryName;
		}

		public void setCountryName(String countryName) {
			this.countryName = countryName;
		}
	}

	@Entity(name = "GeoDistrict")
	public static class GeoDistrict {

		@GeneratedValue
		@Id
		private Long id;

		private String districtName;

		@Column(name = "COUNTRY_ID")
		private Long countryId;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinColumn(name = "DISTRICT_DTL_ID")
		private GeoDistrictDetail geoDistrictDetail;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDistrictName() {
			return districtName;
		}

		public void setDistrictName(String districtName) {
			this.districtName = districtName;
		}

		public Long getCountryId() {
			return countryId;
		}

		public void setCountryId(Long countryId) {
			this.countryId = countryId;
		}

		public GeoDistrictDetail getGeoDistrictDetail() {
			return geoDistrictDetail;
		}

		public void setGeoDistrictDetail(GeoDistrictDetail geoDistrictDetail) {
			this.geoDistrictDetail = geoDistrictDetail;
		}
	}

	@Entity(name = "GeoDistrictDetail")
	public static class GeoDistrictDetail {

		@GeneratedValue
		@Id
		private Long id;

		private String detail;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

	@Entity(name = "GeoNation")
	public static class GeoNation {

		@GeneratedValue
		@Id
		private Long id;

		@Column(name = "GOV_ID")
		private Long govId;

		@Column(name = "TEAM_ID")
		private Long teamId;

		private String nationName;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getGovId() {
			return govId;
		}

		public void setGovId(Long govId) {
			this.govId = govId;
		}

		public Long getTeamId() {
			return teamId;
		}

		public void setTeamId(Long teamId) {
			this.teamId = teamId;
		}

		public String getNationName() {
			return nationName;
		}

		public void setNationName(String nationName) {
			this.nationName = nationName;
		}
	}
}
