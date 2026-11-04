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
    public WorkspaceOverviewController(WorkspaceOverviewService workspace) { this.workspace = workspace; }
    @GetMapping("/overview") public Map<String, Object> overview() { return workspace.overview(); }
}
