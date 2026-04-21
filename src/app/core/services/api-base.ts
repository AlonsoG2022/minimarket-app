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

export const API_BASE_URL = API_TARGETS[ACTIVE_API_TARGET];
  
