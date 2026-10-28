package com.aria.controller;
import com.aria.service.CustomerAssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@RestController @RequestMapping("/api/assistant")
public class CustomerAssistantController {
    private final CustomerAssistantService assistant;private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
    private record Window(long until,int count){}
    public CustomerAssistantController(CustomerAssistantService assistant){this.assistant=assistant;}
    public record Question(@NotBlank @Size(max=500) String query,@Size(max=5) List<@NotNull @Positive Long> productIds,Boolean externalAiConsent){}
    @GetMapping("/status") public Map<String,Object> status(){return assistant.status();}
    @PostMapping("/query") public Map<String,Object> ask(@Valid @RequestBody Question q,HttpServletRequest request){
        long now=System.currentTimeMillis();String address=request.getRemoteAddr();
        if(windows.size()>5000){windows.entrySet().removeIf(e->e.getValue().until()<now);if(windows.size()>5000)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Повторите позже");}
        var window=windows.compute(address,(k,w)->w==null||w.until()<now?new Window(now+300000,1):new Window(w.until(),w.count()+1));
        if(window.count()>20)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Слишком много запросов. Повторите через несколько минут");
        return assistant.answer(q.query(),q.productIds()==null?List.of():q.productIds(),Boolean.TRUE.equals(q.externalAiConsent()));
    }
}
