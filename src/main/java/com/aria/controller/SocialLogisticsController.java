package com.aria.controller;
import com.aria.service.SocialLogisticsService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
@RestController @RequestMapping("/api/admin/control/social-proposals")
public class SocialLogisticsController {
 private final SocialLogisticsService service;public SocialLogisticsController(SocialLogisticsService s){service=s;}
 public record Plan(@Min(0) long version,@NotNull Long assigneeId,@NotNull LocalDateTime plannedAt,@NotBlank @Pattern(regexp="SCHEDULED|READY|IN_TRANSIT") String status,@NotBlank @Size(max=700) String reason){}
 @GetMapping("/staff") public Object staff(){return service.staff();}
 @PutMapping("/{id}/logistics") public Object plan(@PathVariable Long id,@Valid @RequestBody Plan p){return service.plan(id,p.version(),p.assigneeId(),p.plannedAt(),p.status(),p.reason());}
}
