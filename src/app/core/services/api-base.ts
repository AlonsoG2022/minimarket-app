type RuntimeConfig = {
  apiBaseUrl?: string;
};

const currentHost =
  typeof window !== 'undefined' && window.location.hostname
    ? window.location.hostname
    : 'localhost';

export const API_TARGETS = {
  dotnet: `http://${currentHost}:5210/api`,
  java: `http://${currentHost}:5214/api`,
} as const;

export type ApiTarget = keyof typeof API_TARGETS;

// Cambia este valor cuando quieras alternar entre .NET y Java.
export const ACTIVE_API_TARGET: ApiTarget = 'java';

function resolveApiBaseUrl(): string {
  const runtimeConfig = typeof window === 'undefined'
    ? undefined
    : (window as Window & { __MIROO_CONFIG__?: RuntimeConfig }).__MIROO_CONFIG__;
  const configuredUrl = runtimeConfig?.apiBaseUrl?.trim();

  if (configuredUrl && /^https?:\/\//i.test(configuredUrl)) {
    return configuredUrl.replace(/\/+$/, '');
  }

  return API_TARGETS[ACTIVE_API_TARGET];
}

// runtime-config.js se reemplaza durante el despliegue sin recompilar Angular.
export const API_BASE_URL = resolveApiBaseUrl();
  
