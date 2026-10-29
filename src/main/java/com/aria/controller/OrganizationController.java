package com.aria.controller;

import com.aria.config.OrganizationBoundary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/admin/control/organization")
public class OrganizationController {
 private final OrganizationBoundary boundary;private final JdbcTemplate jdbc;
 public OrganizationController(OrganizationBoundary boundary,JdbcTemplate jdbc){this.boundary=boundary;this.jdbc=jdbc;}
 @GetMapping public Map<String,Object> current(){
  var organization=jdbc.queryForMap("select organization_key,name,demonstration,impact_enabled from saas_organization where id=1");
  return Map.of("organization",organization,"binding",boundary.key(),"isolation","DATABASE_AND_PROCESS","selfServiceRegistration",false,
   "stores",jdbc.queryForList("select id,name,platform,base_url,status,write_enabled,version from connected_stores where organization_id=1 order by id"));
 }
}
