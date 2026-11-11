package com.aria.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Public build facts contain no contacts, credentials, database addresses, or organization IDs. */
@Service
public class ApplicationIdentityService {
 private final Environment env;
 private final JdbcTemplate jdbc;
 private final ObjectProvider<BuildProperties> builds;
 public ApplicationIdentityService(Environment env,JdbcTemplate jdbc,ObjectProvider<BuildProperties> builds){this.env=env;this.jdbc=jdbc;this.builds=builds;}
 public Map<String,Object> publicInfo(){
  String key=env.getProperty("aria.organization.key","demo"),kind=key.equals("demo")?"DEMO":key.equals("aria-registration")?"REGISTRATION":"BUSINESS";
  boolean loopback="127.0.0.1".equals(env.getProperty("server.address")),desktop=loopback&&env.acceptsProfiles(Profiles.of("local-desktop"))&&!env.acceptsProfiles(Profiles.of("production"))&&"jdbc:postgresql://127.0.0.1:55434/aria_onboarding_shop".equals(env.getProperty("spring.datasource.url"));
  var build=builds.getIfAvailable();var version=new LinkedHashMap<String,Object>();version.put("version",build==null?"development":build.getVersion());version.put("revision",build==null?"development":Objects.toString(build.get("revision"),"development"));version.put("builtAt",build==null?null:build.getTime());
  var result=new LinkedHashMap<String,Object>();result.put("application","ARIA");result.put("workspaceType",kind);result.put("environment",loopback?"LOCAL":env.acceptsProfiles(Profiles.of("production"))?"PRODUCTION":"PREVIEW");result.put("build",version);result.put("desktop",desktop);result.put("startPath","/start.html");
  var links=new LinkedHashMap<String,String>();if(loopback)links.put("start","http://127.0.0.1:8089/start.html");if(desktop){links.put("registration","http://127.0.0.1:8088/company-start.html");links.put("storefront","http://127.0.0.1:8087/index.html");links.put("demo","http://127.0.0.1:8087/login.html?next=%2Fcontrol.html");}result.put("links",links);return result;
 }
 public Map<String,Object> company(){var row=jdbc.queryForMap("SELECT name,demonstration FROM saas_organization WHERE id=1");return Map.of("name",Objects.toString(row.get("name"),"ARIA"),"demonstration",Boolean.TRUE.equals(row.get("demonstration")));}
}
