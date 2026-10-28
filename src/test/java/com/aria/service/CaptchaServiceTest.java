package com.aria.service;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
class CaptchaServiceTest {
    private String answer(String question){var matcher=java.util.regex.Pattern.compile("\\d+").matcher(question);matcher.find();int a=Integer.parseInt(matcher.group());matcher.find();return Integer.toString(a+Integer.parseInt(matcher.group()));}
    @Test void wrongAnswerConsumesChallengeAndCannotBeRetried(){
        var service=new CaptchaService();var challenge=service.createChallenge();
        assertThat(service.verifyAnswer(challenge.get("id"),"-1")).isFalse();
        assertThat(service.verifyAnswer(challenge.get("id"),answer(challenge.get("question")))).isFalse();
    }
    @Test void concurrentReplayOnlySucceedsOnce()throws Exception {
        var service=new CaptchaService();var challenge=service.createChallenge();String answer=answer(challenge.get("question"));
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            var results=executor.invokeAll(java.util.List.<Callable<Boolean>>of(()->service.verifyAnswer(challenge.get("id"),answer),()->service.verifyAnswer(challenge.get("id"),answer)));
            assertThat(java.util.List.of(results.get(0).get(),results.get(1).get())).containsExactlyInAnyOrder(true,false);
        }
    }
}
