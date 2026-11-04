package de.adrianrostin.altsetter.config;

import de.adrianrostin.altsetter.controller.ApiPaths;
import de.adrianrostin.altsetter.entity.Language;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class WebConfig {

    /**
     * Sprache aus {@code Accept-Language}: unterstützt sind die Sprachen aus {@link Language}, auch mit
     * Region (z. B. {@code en-US}); alles andere bekommt Deutsch. Der Bean-Name ersetzt den Standard.
     */
    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(Language.supportedLocales());
        resolver.setDefaultLocale(Language.DEFAULT.getLocale());
        return resolver;
    }

    /**
     * ETag aus dem Inhalt jeder Antwort. Fragt ein Client mit {@code If-None-Match} nach und hat sich
     * nichts geändert, geht nur ein leeres 304 zurück. Läuft nach Request-ID und Rate-Limit.
     */
    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns(ApiPaths.V1 + "/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
