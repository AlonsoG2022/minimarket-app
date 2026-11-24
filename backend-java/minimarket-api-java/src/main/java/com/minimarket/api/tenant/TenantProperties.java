package com.minimarket.api.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "miroo.tenancy")
public class TenantProperties {

    private boolean enabled;
    private String operationalDatabaseName = "MinimarketDb";
    private String localHostSlug = "market11";
    private final CoreDatasource coreDatasource = new CoreDatasource();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOperationalDatabaseName() {
        return operationalDatabaseName;
    }

    public void setOperationalDatabaseName(String operationalDatabaseName) {
        this.operationalDatabaseName = operationalDatabaseName;
    }

    public String getLocalHostSlug() {
        return localHostSlug;
    }

    public void setLocalHostSlug(String localHostSlug) {
        this.localHostSlug = localHostSlug;
    }

    public CoreDatasource getCoreDatasource() {
        return coreDatasource;
    }

    public static class CoreDatasource {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }
}
