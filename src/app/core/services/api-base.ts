export const API_TARGETS = {
  dotnet: 'http://localhost:5210/api',
  java: 'http://localhost:5214/api',
} as const;

export type ApiTarget = keyof typeof API_TARGETS;

// Cambia este valor a 'java' cuando quieras probar el backend Spring Boot.
export const ACTIVE_API_TARGET: ApiTarget = 'java';

export const API_BASE_URL = API_TARGETS[ACTIVE_API_TARGET];
  