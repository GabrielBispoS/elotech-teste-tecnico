package com.elotech.taskmanager.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache em memoria do proprio processo (ConcurrentMapCacheManager, o default do Boot). Suficiente
 * para uma instancia; em multiplas instancias o relatorio precisaria de um cache compartilhado.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
