package com.aria.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Render connection strings include userinfo; the PostgreSQL JDBC driver requires separate credentials. */
@Configuration(proxyBeanMethods=false)
@Profile("postgres")
public class DatabaseUrlConfiguration {
    @Bean
    static BeanFactoryPostProcessor normalizeDatabaseUrl(ConfigurableEnvironment environment) {
        Map<String,Object> properties=parse(environment.getProperty("spring.datasource.url",""));
        if(!properties.isEmpty())environment.getPropertySources().addFirst(new MapPropertySource("ariaJdbcConnection",properties));
        return factory -> {};
    }
    static Map<String,Object> parse(String input) {
        String raw=input.startsWith("jdbc:")?input.substring(5):input;
        if(raw.startsWith("postgres://"))raw="postgresql://"+raw.substring("postgres://".length());
        if(!raw.startsWith("postgresql://"))throw new IllegalStateException("Ожидается строка подключения PostgreSQL");
        try {
            URI uri=URI.create(raw);
            if(uri.getHost()==null || uri.getRawPath()==null || uri.getRawPath().length()<2 || uri.getFragment()!=null)
                throw new IllegalArgumentException();
            Map<String,Object> values=new HashMap<>();
            String authority=uri.getRawAuthority();
            if(uri.getRawUserInfo()!=null) {
                String[] credentials=uri.getRawUserInfo().split(":",2);
                values.put("spring.datasource.username",decode(credentials[0]));
                if(credentials.length>1)values.put("spring.datasource.password",decode(credentials[1]));
                authority=authority.substring(authority.lastIndexOf('@')+1);
            }
            values.put("spring.datasource.url","jdbc:postgresql://"+authority+uri.getRawPath()+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery()));
            return values;
        } catch(IllegalArgumentException exception) {
            // Never include a credential-bearing URL or parsing exception in an error message.
            throw new IllegalStateException("Некорректная строка подключения PostgreSQL");
        }
    }
    private static String decode(String value){return URLDecoder.decode(value.replace("+","%2B"),StandardCharsets.UTF_8);}
}
