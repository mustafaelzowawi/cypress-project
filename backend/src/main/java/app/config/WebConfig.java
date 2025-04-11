package app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
    
    /**
     * Security header filter bean to add various security headers to HTTP responses.
     * These headers help protect against common web vulnerabilities.
     */
    @Bean
    public OncePerRequestFilter securityHeadersFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                           HttpServletResponse response, 
                                           FilterChain filterChain) throws ServletException, IOException {
                // Content-Security-Policy: prevents XSS attacks by specifying which dynamic resources are allowed
                response.setHeader("Content-Security-Policy", 
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://unpkg.com https://cdn.jsdelivr.net; " +
                    "img-src 'self' data: https://*.tile.openstreetmap.org https://unpkg.com; " +
                    "style-src 'self' https://unpkg.com; " +
                    "font-src 'self'; " +
                    "connect-src 'self' localhost:8080 localhost:8081");
                
                // X-Content-Type-Options: prevents MIME type sniffing
                response.setHeader("X-Content-Type-Options", "nosniff");
                
                // X-Frame-Options: prevents clickjacking attacks
                response.setHeader("X-Frame-Options", "DENY");
                
                // X-XSS-Protection: enables the browser's built-in XSS filter
                response.setHeader("X-XSS-Protection", "1; mode=block");
                
                // Referrer-Policy: controls how much information is sent in the Referer header
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                
                // Strict-Transport-Security: ensures HTTPS connections
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                
                filterChain.doFilter(request, response);
            }
        };
    }
} 