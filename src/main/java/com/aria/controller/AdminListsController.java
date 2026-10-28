package com.aria.controller;
import com.aria.dto.AdminViews.*;
import com.aria.entity.AuditEvent;
import com.aria.service.AdminReadService;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/control")
public class AdminListsController {
    private final AdminReadService read;
    public AdminListsController(AdminReadService read){this.read=read;}
    @GetMapping("/orders/page") public PageResult<OrderRow> orders(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String state,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size,@RequestParam(defaultValue="newest")String sort){return read.orderPage(q,state,page,size,sort);}
    @GetMapping("/audit/page") public PageResult<AuditEvent> audit(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){return read.auditPage(q,page,size);}
}
