package com.tt.tripbook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// When the React build is packaged inside the jar (Dockerfile.render), the browser may refresh on
// a client-side route such as /passengers. Those paths are not files, so they are forwarded to
// index.html and React Router takes over. Without the build in the jar (local development, tests)
// the forward finds nothing and answers 404, which is harmless.
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    // Keep in sync with the routes in frontend/src/App.tsx and the permit rules in SecurityConfig
    public static final String[] CLIENT_ROUTES = {"/login", "/register", "/dashboard", "/trips", "/passengers", "/destinations"};

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String route : CLIENT_ROUTES) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }
}
