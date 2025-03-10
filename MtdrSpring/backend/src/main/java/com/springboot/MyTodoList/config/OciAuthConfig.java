package com.springboot.MyTodoList.config;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration
public class OciAuthConfig {

    @Value("${oci.config.file:~/.oci/config}")
    private String configFile;

    @Value("${oci.config.profile:DEFAULT}")
    private String configProfile;

    @Bean
    @Profile("!oci")
    public BasicAuthenticationDetailsProvider configFileProvider() throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(configFile, configProfile);
    }
}
