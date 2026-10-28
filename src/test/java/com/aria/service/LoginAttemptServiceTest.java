package com.aria.service;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
class LoginAttemptServiceTest {
    static class MutableClock extends Clock {
        Instant time=Instant.parse("2026-10-28T12:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return time;}
    }
    @Test void blockedAccountCannotBypassWindowButOtherAccountWorksAndWindowExpires(){
        MutableClock clock=new MutableClock();var service=new LoginAttemptService(clock);
        for(int i=0;i<6;i++){service.check("owner@example.test");service.failed("owner@example.test");}
        assertThatThrownBy(()->service.check("owner@example.test")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatCode(()->service.check("other@example.test")).doesNotThrowAnyException();
        clock.time=clock.time.plusSeconds(900);
        assertThatCode(()->service.check("owner@example.test")).doesNotThrowAnyException();
    }
    @Test void successfulLoginClearsEarlierFailures(){
        var service=new LoginAttemptService();for(int i=0;i<5;i++)service.failed("owner");
        service.succeeded("owner");service.failed("owner");
        assertThatCode(()->service.check("owner")).doesNotThrowAnyException();
    }
}
