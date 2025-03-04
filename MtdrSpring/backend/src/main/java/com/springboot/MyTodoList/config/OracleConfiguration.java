package com.springboot.MyTodoList.config;

import oracle.jdbc.pool.OracleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class OracleConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(OracleConfiguration.class);
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;
    
    @Value("${spring.datasource.url}")
    private String url;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Bean
    @Primary
    public DataSource dataSource() throws SQLException {
        OracleDataSource ds = new OracleDataSource();
        ds.setDriverType(driverClassName);
        logger.info("Using Driver: {}", driverClassName);
        ds.setURL(url);
        logger.info("Using URL: {}", url);
        ds.setUser(username);
        logger.info("Using Username: {}", username);
        ds.setPassword(password);
        return ds;
    }
}