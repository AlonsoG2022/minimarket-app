# Codex Prompts Base

## Como usar este archivo
Estos prompts estan pensados para reutilizarse cuando quieras pedirme una implementacion nueva sin volver a explicar todo el contexto.

Puedes copiarlos y adaptarlos.

---

## Prompt 1: Implementacion alineada completa

```md
Implementa la siguiente funcionalidad en mi aplicacion minimarket.

Reglas:
- Manten alineado Angular, backend .NET, backend Java y SQL Server.
- Revisa y actualiza tambien los archivos `.md` de `docs/`.
- Si la solucion tiene fases, indicalo claramente.
- Antes de finalizar, explicame el funcionamiento y que debo desplegar.

Funcionalidad a implementar:
[DESCRIBIR AQUI]
```

---

## Prompt 2: Mejora sobre modulo existente

```md
Quiero mejorar un modulo ya implementado.

Reglas:
- Respeta el comportamiento actual que ya funciona.
- Aplica el cambio en frontend, .NET, Java y SQL si corresponde.
- Actualiza `docs/PROJECT_CONTEXT.md` y `docs/IMPLEMENTATION_ROADMAP.md`.
- Explicame que cambio y como probarlo.

Modulo:
[VENTAS / COMPRAS / PRODUCTOS / CAJA / REPORTES / OTRO]

Cambio solicitado:
[DESCRIBIR AQUI]
```

---

## Prompt 3: Nueva fase de arquitectura

```md
Vamos a entrar a una nueva fase del sistema.

Necesito que:
- propongas una arquitectura mantenible
- implementes la primera fase
- dejes preparado el sistema para fases futuras
- actualices la documentacion viva en `docs/`

Tema:
[IMPRESION / SUNAT / FACTURACION / CAJA / SERVICIO WINDOWS / OTRO]

Objetivo:
[DESCRIBIR AQUI]
```

---

## Prompt 3A: Solo servicio de impresion

```md
Quiero que implementes solamente la parte del servicio de impresion.

Toma como referencia:
- docs/PROJECT_CONTEXT.md
- docs/IMPLEMENTATION_ROADMAP.md

Alcance de esta implementacion:
- servicio local de impresion
- cola de impresion
- estados de impresion

No implementes todavia:
- envio a SUNAT
- logica completa de boleta/factura ante SUNAT
- datos tributarios avanzados del producto

Ademas:
- manten alineado Angular, .NET, Java y SQL donde corresponda
- actualiza los archivos `.md` al finalizar
- explicame que debo desplegar y como probar la cola local
```

---

## Prompt 3B: Mejorar ticket por fases

```md
Quiero mejorar el ticket de venta tomando como referencia `docs/PROJECT_CONTEXT.md` y `docs/IMPLEMENTATION_ROADMAP.md`.

Trabaja solo la fase indicada:
- Fase 1: ticket operativo mejorado para minimarket
- Fase 2: ticket documental preparado para boleta/factura

Reglas:
- no mezcles fases sin avisar
- si la fase depende de otra estructura pendiente, dejalo documentado
- mantén alineado frontend, backend y worker de impresion donde corresponda
- actualiza los archivos `.md` al finalizar

Fase a implementar:
[FASE 1 / FASE 2]

Objetivo o referencia visual:
[DESCRIBIR AQUI]
```

---

## Prompt 4: Despliegue

```md
Necesito preparar esta version para despliegue.

Quiero que:
- generes o dejes listos los artefactos necesarios
- me indiques que archivos copiar
- me indiques que scripts SQL ejecutar
- me expliques el orden exacto de despliegue
- actualices la documentacion si cambio algo del proceso

Entorno destino:
[MAQUINA NUEVA / IIS / SQL SERVER EXPRESS / JAVA / .NET / OTRO]
```

---

## Prompt 4A: Script SQL unico e idempotente

```md
Quiero que revises los scripts SQL del proyecto y dejes un solo script idempotente para instalacion o upgrade.

Reglas:
- no uses `DROP`
- no debe perder datos existentes
- debe incluir el login/usuario SQL de conexion si corresponde
- deja claro cual script es el recomendado y cual queda solo para desarrollo
- actualiza la documentacion en `docs/`
```

---

## Prompt 5: Diagnostico

```md
Tengo un problema en este modulo:
[DESCRIBIR ERROR]

Quiero que:
- revises frontend, backend y base si corresponde
- identifiques la causa mas probable
- implementes la correccion
- actualices la documentacion si la correccion cambia una regla del sistema
```

---

## Convencion de actualizacion documental
Cada implementacion importante debe revisar y actualizar al menos:

- `docs/PROJECT_CONTEXT.md`
- `docs/IMPLEMENTATION_ROADMAP.md`

Si una nueva funcionalidad requiere guia de uso o prompts nuevos, tambien debe actualizar:

- `docs/CODEX_PROMPTS.md`
