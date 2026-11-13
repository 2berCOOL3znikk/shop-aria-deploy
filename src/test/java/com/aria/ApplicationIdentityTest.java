package com.aria;

import com.aria.service.ApplicationIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationIdentityTest {
 @SuppressWarnings("unchecked") private ApplicationIdentityService identity(MockEnvironment env,JdbcTemplate jdbc){return new ApplicationIdentityService(env,jdbc,mock(ObjectProvider.class));}
 private MockEnvironment local(){var env=new MockEnvironment().withProperty("server.address","127.0.0.1").withProperty("aria.organization.key","business-example").withProperty("spring.datasource.url","jdbc:postgresql://127.0.0.1:55434/aria_onboarding_shop");env.setActiveProfiles("local-desktop");return env;}
 @Test void publicFactsDoNotReadPrivateCompanyData(){var jdbc=mock(JdbcTemplate.class);var info=identity(local(),jdbc).publicInfo();assertThat(info).containsEntry("desktop",true).containsEntry("workspaceType","BUSINESS").doesNotContainKeys("company","email","binding","password","database");assertThat((java.util.Map<?,?>)info.get("links")).hasSize(4);verifyNoInteractions(jdbc);}
 @Test void productionAndAnotherDatabaseCannotAdvertiseDesktopConnections(){var env=local();env.setActiveProfiles("local-desktop","production");assertThat(identity(env,mock(JdbcTemplate.class)).publicInfo()).containsEntry("desktop",false);env.setActiveProfiles("local-desktop");env.setProperty("spring.datasource.url","jdbc:postgresql://127.0.0.1:55434/another_company");var result=identity(env,mock(JdbcTemplate.class)).publicInfo();assertThat(result).containsEntry("desktop",false);assertThat((java.util.Map<?,?>)result.get("links")).hasSize(1);}
 @Test void stagingIsClearlyLabelledWithoutAdvertisingDesktopOrPrivateFacts(){var env=new MockEnvironment().withProperty("aria.organization.key","aria-v4-staging");env.setActiveProfiles("postgres","production","free","staging");var jdbc=mock(JdbcTemplate.class);var info=identity(env,jdbc).publicInfo();assertThat(info).containsEntry("environment","STAGING").containsEntry("desktop",false).doesNotContainKeys("password","database","email","binding");assertThat((java.util.Map<?,?>)info.get("links")).isEmpty();verifyNoInteractions(jdbc);}

 @Test void reportsKnownRenderCommitWhenTheContainerHasNoGitBuildMetadata(){var env=new MockEnvironment().withProperty("RENDER_GIT_COMMIT","a".repeat(40));var info=identity(env,mock(JdbcTemplate.class)).publicInfo();assertThat((java.util.Map<String,Object>)info.get("build")).containsEntry("revision","a".repeat(40));env.setProperty("RENDER_GIT_COMMIT","not-a-commit");assertThat((java.util.Map<String,Object>)identity(env,mock(JdbcTemplate.class)).publicInfo().get("build")).containsEntry("revision","development");}

}
