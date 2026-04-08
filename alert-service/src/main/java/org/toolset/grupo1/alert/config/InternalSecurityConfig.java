package org.toolset.grupo1.alert.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InternalSecurityConfig implements WebMvcConfigurer {

    private final InternalTokenInterceptor internalTokenInterceptor;

    public InternalSecurityConfig(InternalTokenInterceptor internalTokenInterceptor) {
        this.internalTokenInterceptor = internalTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalTokenInterceptor)
                .addPathPatterns("/internal/**");
    }
}

