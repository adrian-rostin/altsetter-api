package de.adrianrostin.altsetter.config;

import de.adrianrostin.altsetter.service.ExerciseService;
import de.adrianrostin.altsetter.service.MuscleService;
import java.util.List;
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feste Liste der Caches. Ein Tippfehler in einem {@code @Cacheable} fällt so beim ersten Aufruf auf,
 * statt still einen neuen, unbegrenzten Cache anzulegen.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    CacheManagerCustomizer<CaffeineCacheManager> fixedCacheNames() {
        return manager -> manager.setCacheNames(List.of(
                MuscleService.CACHE, ExerciseService.SUMMARY_CACHE, ExerciseService.DETAIL_CACHE));
    }
}
