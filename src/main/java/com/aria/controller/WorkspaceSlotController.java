package com.aria.controller;
import com.aria.service.WorkspaceSlotService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class WorkspaceSlotController {
 private final WorkspaceSlotService service;
 public WorkspaceSlotController(WorkspaceSlotService service){this.service=service;}
 @PostMapping("/api/internal/workspace/bootstrap")
 public Object bootstrap(HttpServletRequest request,@RequestHeader(value="X-ARIA-Workspace-Proof",required=false) String proof)throws java.io.IOException{
  byte[] bytes=request.getInputStream().readNBytes(16385);if(bytes.length>16384)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.handle(new String(bytes,java.nio.charset.StandardCharsets.UTF_8),proof,request.getRemoteAddr()));
 }
}
