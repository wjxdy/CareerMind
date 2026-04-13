package com.careermind.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // 禁用强制懒加载，使用 null 替换未加载的懒加载对象
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        return module;
    }
}
