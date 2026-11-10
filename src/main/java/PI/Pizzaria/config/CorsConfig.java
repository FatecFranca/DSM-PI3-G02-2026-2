package PI.Pizzaria.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final AutorizacaoInterceptor autorizacaoInterceptor;
    private final String origensPermitidas;

    public CorsConfig(AutorizacaoInterceptor autorizacaoInterceptor,
                      @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
                      String origensPermitidas) {
        this.autorizacaoInterceptor = autorizacaoInterceptor;
        this.origensPermitidas = origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(origensPermitidas.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autorizacaoInterceptor)
                .addPathPatterns("/clientes", "/clientes/**", "/colaboradores", "/colaboradores/**",
                        "/pizzas", "/pizzas/**", "/pedidos", "/pedidos/**", "/auth/admin/**", "/auth/logout");
    }
}
