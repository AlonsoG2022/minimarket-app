# Implementation Roadmap

## Como usar este archivo
Este roadmap sirve para saber:

- que ya esta implementado
- que esta en curso
- que falta
- que orden conviene seguir

Cada bloque debe actualizarse cuando una implementacion cambie de estado.

Estados sugeridos:

- `Pendiente`
- `En progreso`
- `Implementado`
- `Primera fase`

---

## 1. Nucleo comercial

### Login y roles
- Estado: `Implementado`
- Notas:
  - roles base `admin` y `cajero`
  - ventas usan el usuario autenticado

### Productos
- Estado: `Implementado`
- Notas:
  - barcode unico compra/venta
  - importacion y exportacion Excel
  - fecha de caducidad

### Compras
- Estado: `Implementado`
- Notas:
  - compra por proveedor
  - paquete a unidad
  - creacion rapida de producto desde compras

### Ventas
- Estado: `Implementado`
- Notas:
  - buscador rapido
  - ticket actual
  - soporte de lector

---

## 2. Caja

### Apertura, movimientos y cierre
- Estado: `Implementado`
- Incluye:
  - apertura con monto inicial
  - ingresos
  - retiros
  - gastos
  - cierre con diferencia

### Integracion de ventas con caja
- Estado: `Implementado`
- Incluye:
  - bloqueo de venta sin caja abierta
  - venta en efectivo suma a caja

---

## 3. Ticket e impresion

### Ticket imprimible por navegador
- Estado: `Primera fase`
- Incluye:
  - vista previa
  - detalle de productos
  - impresion con `window.print`

### Fase 1: Ticket operativo mejorado
- Estado: `Implementado`
- Objetivo:
  - dejar el ticket mas cercano a operacion real de minimarket sin depender todavia de boleta/factura formal
- Debe incluir:
  - nombre comercial del negocio
  - RUC
  - direccion y telefono
  - fecha y hora
  - numero de venta
  - cajero
  - cliente simple o consumidor final
  - columnas de detalle:
    - cantidad
    - descripcion
    - precio unitario
    - importe
  - subtotal o total operativo segun aplique
  - mensaje final comercial
- Nota:
  - esta fase reemplaza el ticket muy basico actual, pero todavia no representa una boleta electronica formal
  - la vista previa web y los workers de impresion deben mantenerse consistentes en esta fase

### Fase 2: Ticket documental preparado para boleta/factura
- Estado: `Pendiente`
- Objetivo:
  - evolucionar el ticket a una estructura documental lista para enlazarse con boleta/factura y SUNAT
- Debe incluir:
  - tipo de documento
  - serie
  - correlativo
  - subtotal
  - IGV
  - total
  - monto en letras
  - cliente mas formal
  - codigo de barras o QR si se decide usar
- Dependencias:
  - modelo documental de venta
  - series y correlativos
  - reglas tributarias futuras

### Servicio local de impresion
- Estado: `Primera fase`
- Incluye:
  - worker `.NET` para Windows Service
  - worker `Java Spring Boot` equivalente
  - lectura de cola `TrabajosImpresion`
  - impresion automatica de tickets
  - reencolado manual desde ventas
  - la venta no se bloquea si falla el encolado
  - estados:
    - pendiente
    - imprimiendo
    - impreso
    - error

### Compatibilidad con ZKTeco
- Estado: `Pendiente de validacion`
- Notas:
  - validar driver Windows
  - validar ancho real de ticket
  - evaluar texto plano / ESC-POS / spooler

---

## 4. Documentos de venta

### Boleta y factura
- Estado: `Pendiente`
- Alcance deseado:
  - tipo de documento
  - serie
  - correlativo
  - cliente
  - detalle documental

### Series y correlativos
- Estado: `Pendiente`
- Sugerencia:
  - tabla `SeriesDocumentos`
  - correlativo por tipo y serie

### Cola de impresion
- Estado: `Implementado`
- Incluye:
  - tabla `TrabajosImpresion`
  - payload snapshot del ticket en JSON
  - estados:
    - pendiente
    - imprimiendo
    - impreso
    - error
  - reintentos por reencolado manual

---

## 5. Preparacion SUNAT

### Estructura minima recomendada
- Estado: `Pendiente`
- Debe contemplar:
  - tipo documento
  - serie
  - correlativo
  - fecha emision
  - cliente
  - subtotal
  - IGV
  - total
  - moneda
  - estado de envio

### Datos tributarios de producto
- Estado: `Pendiente`
- Sugeridos:
  - afectacion IGV
  - unidad de medida SUNAT
  - codigo SUNAT si se decide usar

---

## 6. Arquitectura recomendada para impresion

### Opcion recomendada
- Estado: `Primera fase implementada`
- Arquitectura:
  - la app principal registra la venta
  - se crea trabajo de impresion en BD con snapshot del ticket
  - un servicio local lo procesa
  - cambia el estado a impreso o error

### Implementacion requerida
- `.NET Worker Service`
- `Java Spring Boot` equivalente, si se quiere mantener paridad

### Recomendacion practica
Para produccion Windows, priorizar `.NET Worker Service` como servicio real.

---

## 7. Proximas implementaciones sugeridas

1. Modelo documental de boleta/factura
2. Series y correlativos
3. Validacion real con ZKTeco
4. Reintentos automaticos con limite
5. Seleccion/configuracion de impresora desde UI o archivo
6. Preparacion SUNAT

---

## 8. Mantenimiento SQL

### Script unico de despliegue
- Estado: `Implementado`
- Script recomendado:
  - `db/minimarket.safe-upgrade.sql`
- Incluye:
  - creacion de base si no existe
  - creacion o actualizacion del login `minimarket_user`
  - creacion del usuario dentro de `MinimarketDb`
  - permisos `db_owner`
  - esquema idempotente sin `DROP`

### Script destructivo de desarrollo
- Estado: `Implementado`
- Script:
  - `db/minimarket.sql`
- Nota:
  - usarlo solo cuando quieras recrear toda la base desde cero en desarrollo
