package com.aria.service;
import com.aria.entity.AuditEvent;
import com.aria.repository.AuditEventRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
@Service
public class AuditService {
 private final AuditEventRepository events;
 public AuditService(AuditEventRepository events){this.events=events;}
 public void record(String action,String resource,String details){
  var auth=SecurityContextHolder.getContext().getAuthentication();
  AuditEvent e=new AuditEvent();e.setActor(auth==null?"system":auth.getName());
  e.setAction(action);e.setResource(resource);e.setDetails(details);events.save(e);
 }
}
