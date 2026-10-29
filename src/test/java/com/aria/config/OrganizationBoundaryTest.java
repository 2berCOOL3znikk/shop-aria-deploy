package com.aria.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationBoundaryTest {
 private JdbcTemplate database(){
  var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
  jdbc.execute("create table saas_organization(id integer primary key check(id=1),organization_key varchar(80),name varchar(200),demonstration boolean,impact_enabled boolean)");
  return jdbc;
 }
 @Test void bindsExistingDemoWithoutMovingBusinessData(){
  var jdbc=database();new OrganizationBoundary(jdbc,"demo").afterPropertiesSet();new OrganizationBoundary(jdbc,"demo").afterPropertiesSet();
  assertEquals(1,jdbc.queryForObject("select count(*) from saas_organization",Integer.class));
  assertFalse(jdbc.queryForObject("select impact_enabled from saas_organization",Boolean.class));
 }
 @Test void copiedDemoCannotSilentlyBecomeAnotherCompany(){
  var jdbc=database();new OrganizationBoundary(jdbc,"demo").afterPropertiesSet();
  assertThrows(IllegalStateException.class,()->new OrganizationBoundary(jdbc,"another-company").afterPropertiesSet());
  assertEquals("demo",jdbc.queryForObject("select organization_key from saas_organization",String.class));
 }
 @Test void nonDemoRequiresExplicitProvisioningAndSecondOrganizationIsRejected(){
  var jdbc=database();assertThrows(IllegalStateException.class,()->new OrganizationBoundary(jdbc,"company-a").afterPropertiesSet());
  jdbc.update("insert into saas_organization values(1,'company-a','A',false,false)");
  new OrganizationBoundary(jdbc,"company-a").afterPropertiesSet();
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into saas_organization values(2,'company-b','B',false,false)"));
 }
}
