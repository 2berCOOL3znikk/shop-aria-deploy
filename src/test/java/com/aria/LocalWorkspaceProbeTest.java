package com.aria;

import com.aria.service.LocalWorkspaceProbe;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class LocalWorkspaceProbeTest {
 private MockEnvironment local(){var env=new MockEnvironment().withProperty("server.address","127.0.0.1").withProperty("aria.organization.key","aria-registration").withProperty("spring.datasource.url","jdbc:postgresql://127.0.0.1:55434/aria_registration_local");env.setActiveProfiles("local-onboarding");return env;}
 @Test void onlyExactOwnedLoopbackTargetIsAllowed(){var gate=new LocalWorkspaceProbe(local());assertThat(gate.validate("http://127.0.0.1:8089")).isEqualTo("http://127.0.0.1:8089");for(String uri:new String[]{"https://example.com","http://localhost:8089","http://127.0.0.1:8089/","http://127.0.0.1:8088","http://127.0.0.1:8089@foreign.example","http://169.254.169.254","file:///C:/secret"})assertThatThrownBy(()->gate.validate(uri)).isInstanceOf(IllegalArgumentException.class);}
 @Test void cannotEnableLocalExceptionInProductionOrAnotherDatabase(){var production=local();production.setActiveProfiles("local-onboarding","production");assertThatThrownBy(()->new LocalWorkspaceProbe(production).requireLocal()).isInstanceOf(IllegalArgumentException.class);var env=local();env.setProperty("spring.datasource.url","jdbc:postgresql://127.0.0.1:55434/aria_woo_e2e");var other=new LocalWorkspaceProbe(env);assertThatThrownBy(other::requireLocal).isInstanceOf(IllegalArgumentException.class);}
 @Test void defaultsDenyWithoutAnyNetworkCall(){var probe=new LocalWorkspaceProbe(new MockEnvironment());assertThatThrownBy(probe::requireLocal).isInstanceOf(IllegalArgumentException.class);}
}
