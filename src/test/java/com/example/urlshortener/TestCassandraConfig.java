package com.example.urlshortener;

import com.datastax.oss.driver.api.core.CqlSession;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.core.CassandraTemplate;

@Configuration
@Profile("test")
public class TestCassandraConfig {

    @Bean
    @Primary
    public CqlSession cqlSession() {
        return Mockito.mock(CqlSession.class);
    }

    @Bean
    @Primary
    public CassandraTemplate cassandraTemplate() {
        return Mockito.mock(CassandraTemplate.class);
    }
}
