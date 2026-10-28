package com.aria.service;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
/** Bounded account-based protection for one application instance. No passwords are retained. */
@Service
public class LoginAttemptService {
    private static final int MAX_KEYS=10000, MAX_FAILURES=6;
    private static final Duration WINDOW=Duration.ofMinutes(15);
    private record Attempt(int failures, Instant until) {}
    private final Map<String,Attempt> attempts=new HashMap<>();
    private final Clock clock;
    public LoginAttemptService(){this(Clock.systemUTC());}
    LoginAttemptService(Clock clock){this.clock=clock;}
    public synchronized void check(String email) {
        cleanup();
        Attempt attempt=attempts.get(email);
        if(attempt!=null && attempt.failures()>=MAX_FAILURES) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Слишком много попыток входа. Повторите через 15 минут");
        if(attempt==null && attempts.size()>=MAX_KEYS) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Вход временно ограничен. Повторите позже");
    }
    public synchronized void failed(String email) {
        cleanup();
        Attempt previous=attempts.get(email);
        if(previous==null && attempts.size()>=MAX_KEYS) return;
        attempts.put(email,new Attempt(previous==null?1:Math.min(MAX_FAILURES,previous.failures()+1),previous==null?clock.instant().plus(WINDOW):previous.until()));
    }
    public synchronized void succeeded(String email){attempts.remove(email);}
    private void cleanup(){Instant now=clock.instant();attempts.entrySet().removeIf(e->!e.getValue().until().isAfter(now));}
}
