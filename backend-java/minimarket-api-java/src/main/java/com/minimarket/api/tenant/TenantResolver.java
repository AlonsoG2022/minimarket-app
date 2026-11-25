package com.minimarket.api.tenant;

import java.util.Locale;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "miroo.tenancy", name = "enabled", havingValue = "true")
public class TenantResolver {

    private final JdbcTemplate coreJdbcTemplate;
    private final TenantProperties properties;
    private final HikariDataSource coreDataSource;

    public TenantResolver(TenantProperties properties) {
        this.properties = properties;
        TenantProperties.CoreDatasource settings = properties.getCoreDatasource();
        if (!StringUtils.hasText(settings.getUrl())) {
            throw new IllegalStateException("MIROO_CORE_DB_URL es obligatoria cuando MIROO_TENANCY_ENABLED=true.");
        }

        this.coreDataSource = new HikariDataSource();
        this.coreDataSource.setJdbcUrl(settings.getUrl());
        this.coreDataSource.setUsername(settings.getUsername());
        this.coreDataSource.setPassword(settings.getPassword());
        this.coreDataSource.setDriverClassName(settings.getDriverClassName());
        this.coreDataSource.setPoolName("miroo-core-pool");
        this.coreDataSource.setMaximumPoolSize(3);
        this.coreDataSource.setMinimumIdle(0);
        this.coreJdbcTemplate = new JdbcTemplate(this.coreDataSource);
    }

    public TenantRecord resolve(String host) {
        String slug = resolveSlug(host);
        TenantRecord tenant = coreJdbcTemplate.query(
            "SELECT Id, Code, Name, Slug, DatabaseName, IsActive FROM dbo.Tenants WHERE Slug = ?",
            rs -> rs.next()
                ? new TenantRecord(
                    rs.getInt("Id"),
                    rs.getString("Code"),
                    rs.getString("Name"),
                    rs.getString("Slug"),
                    rs.getString("DatabaseName"),
                    rs.getBoolean("IsActive")
                )
                : null,
            slug
        );

        if (tenant == null || !tenant.active()) {
            return null;
        }

        // The current API is intentionally bound to Market11's operational schema only.
        if (!properties.getOperationalDatabaseName().equalsIgnoreCase(tenant.databaseName())) {
            return null;
        }

        return tenant;
    }

    private String resolveSlug(String host) {
        if (host == null || host.isBlank()) {
            return properties.getLocalHostSlug();
        }

        String normalizedHost = host.split(":")[0].toLowerCase(Locale.ROOT);
        String configuredSlug = resolveConfiguredHostSlug(normalizedHost);
        if (StringUtils.hasText(configuredSlug)) {
            return configuredSlug;
        }

        if (normalizedHost.equals("localhost") || normalizedHost.equals("127.0.0.1") || normalizedHost.equals("::1")) {
            return properties.getLocalHostSlug();
        }

        int dotIndex = normalizedHost.indexOf('.');
        return dotIndex > 0 ? normalizedHost.substring(0, dotIndex) : normalizedHost;
    }

    private String resolveConfiguredHostSlug(String normalizedHost) {
        if (!StringUtils.hasText(properties.getHostMappings())) {
            return null;
        }

        for (String mapping : properties.getHostMappings().split(",")) {
            String[] parts = mapping.trim().split("=", 2);
            if (parts.length == 2 && normalizedHost.equals(parts[0].trim().toLowerCase(Locale.ROOT))) {
                return parts[1].trim();
            }
        }

        return null;
    }

    @PreDestroy
    void close() {
        coreDataSource.close();
    }
}
