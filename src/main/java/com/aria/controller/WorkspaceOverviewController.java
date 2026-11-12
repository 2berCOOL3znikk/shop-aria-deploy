package com.aria.controller;

import com.aria.service.WorkspaceOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Same ADMIN and isolated-company boundary as all existing control endpoints. */
@RestController
@RequestMapping("/api/admin/control/workspace")
public class WorkspaceOverviewController {
    private final WorkspaceOverviewService workspace;
    private final com.aria.service.ProductFlowService flow;
    public WorkspaceOverviewController(WorkspaceOverviewService workspace,com.aria.service.ProductFlowService flow) { this.workspace = workspace;this.flow=flow; }
    @GetMapping("/overview") public Map<String, Object> overview() { return workspace.overview(); }
    @GetMapping("/flow") public Map<String,Object> flow(@org.springframework.web.bind.annotation.RequestParam(defaultValue="") String q,@org.springframework.web.bind.annotation.RequestParam(defaultValue="ALL") String view,@org.springframework.web.bind.annotation.RequestParam(defaultValue="0") int page,@org.springframework.web.bind.annotation.RequestParam(defaultValue="20") int size){return flow.list(q,view,page,size);}
    @GetMapping("/flow/{id}") public Map<String,Object> detail(@org.springframework.web.bind.annotation.PathVariable Long id){return flow.detail(id);}
}
