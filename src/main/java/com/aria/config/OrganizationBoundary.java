package com.aria.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** A process must never serve a database belonging to another organization. */
@Component @DependsOnDatabaseInitialization
public class OrganizationBoundary implements InitializingBean {
 private final JdbcTemplate jdbc;
 private final String organizationKey;
 public OrganizationBoundary(JdbcTemplate jdbc,@Value("${aria.organization.key:demo}") String key){
  if(key==null||!key.matches("[a-z0-9][a-z0-9-]{0,79}"))throw new IllegalStateException("Invalid organization binding");
  this.jdbc=jdbc;this.organizationKey=key;
 }
 @Override public void afterPropertiesSet(){
  var rows=jdbc.queryForList("select id, organization_key from saas_organization");
  // H2 compatibility, before a deployment has PostgreSQL migrations. Non-demo provisioning is explicit.
  if(rows.isEmpty()&&organizationKey.equals("demo")){
   jdbc.update("insert into saas_organization(id,organization_key,name,demonstration,impact_enabled) values(1,'demo','ARIA Demo',true,false)");
   rows=jdbc.queryForList("select id, organization_key from saas_organization");
  }
  if(rows.size()!=1||((Number)rows.getFirst().get("id")).intValue()!=1||!organizationKey.equals(rows.getFirst().get("organization_key")))
   throw new IllegalStateException("Organization/database binding mismatch; refusing startup");
 }
 public String key(){return organizationKey;}
}
