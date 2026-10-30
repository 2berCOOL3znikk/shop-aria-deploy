package com.aria.controller;
import com.aria.service.ConnectedStoresService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/control/stores")
public class ConnectedStoresController {
 private final ConnectedStoresService service;public ConnectedStoresController(ConnectedStoresService s){service=s;}
 public record Create(@NotBlank @Size(max=200) String name,@Pattern(regexp="WOOCOMMERCE|ARIA_DEMO") @NotNull String platform,@Size(max=500) String url){}
 public record Version(@Min(0) long version){}
 public record Credentials(@Min(0) long version,@Pattern(regexp="ck_[a-zA-Z0-9]{20,100}") @NotNull String key,@Pattern(regexp="cs_[a-zA-Z0-9]{20,100}") @NotNull String secret){}
 public record Business(@NotBlank @Size(max=200) String name){}
 @GetMapping public Object list(){return service.list();}
 @PostMapping public Object create(@Valid @RequestBody Create c){return service.create(c.name(),c.platform(),c.url());}
 @PutMapping("/business") public Object business(@Valid @RequestBody Business b){return service.rename(b.name());}
 @PutMapping("/{id}/credentials") public Object credentials(@PathVariable Long id,@Valid @RequestBody Credentials c){return service.credentials(id,c.version(),c.key(),c.secret());}
 @PostMapping("/{id}/check") public Object check(@PathVariable Long id,@Valid @RequestBody Version v){return service.sync(id,v.version(),true);}
 @PostMapping("/{id}/sync") public Object sync(@PathVariable Long id,@Valid @RequestBody Version v){return service.sync(id,v.version(),false);}
 @PostMapping("/{id}/disconnect") public Object disconnect(@PathVariable Long id,@Valid @RequestBody Version v){return service.disconnect(id,v.version());}
 @GetMapping("/{id}/history") public Object history(@PathVariable Long id){return service.history(id);}
 @GetMapping("/{id}/catalog") public Object catalog(@PathVariable Long id){return service.catalog(id);}
}
