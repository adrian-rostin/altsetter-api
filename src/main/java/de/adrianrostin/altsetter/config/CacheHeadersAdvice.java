package de.adrianrostin.altsetter.config;

import de.adrianrostin.altsetter.controller.ApiPaths;
import de.adrianrostin.altsetter.entity.Language;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Cache-Header für die API. Die Daten ändern sich nur mit einem Deploy, deshalb dürfen Browser und
 * CDN Antworten fünf Minuten behalten; danach fragen sie per ETag nach und bekommen meist nur ein
 * 304. Fehler werden nie gecacht.
 *
 * <p>Die Antwort hängt von {@code Accept-Language} ab: {@code Vary} sagt Caches, dass sie je Sprache
 * getrennt speichern müssen, {@code Content-Language} nennt die gelieferte Sprache.
 */
@RestControllerAdvice
public class CacheHeadersAdvice implements ResponseBodyAdvice<Object> {

    private static final CacheControl CACHEABLE =
            CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public @Nullable Object beforeBodyWrite(
            @Nullable Object body,
            MethodParameter returnType,
            MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (request.getURI().getPath().startsWith(ApiPaths.V1 + "/")) {
            CacheControl cacheControl = body instanceof ProblemDetail ? CacheControl.noStore() : CACHEABLE;
            response.getHeaders().setCacheControl(cacheControl);
            response.getHeaders().add(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
            response.getHeaders()
                    .set(
                            HttpHeaders.CONTENT_LANGUAGE,
                            Language.of(LocaleContextHolder.getLocale()).getCode());
        }
        return body;
    }
}
