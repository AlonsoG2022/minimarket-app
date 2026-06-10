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
  - stock minimo global configurable desde `Configuracion` (no editable por producto)
  - aviso de stock minimo compacto (conteo + productos mas bajos)

### Compras
- Estado: `Implementado`
- Notas:
  - compra por proveedor
  - paquete a unidad
  - creacion rapida de producto desde compras
  - guarda `SubTotal`, `IGV` y `Total` en cabecera
  - regla actual: el costo/precio unitario registrado ya incluye IGV

### Ventas
- Estado: `Implementado`
- Notas:
  - buscador rapido
  - ticket actual
  - soporte de lector
  - guarda `SubTotal`, `IGV` y `Total` en cabecera
  - regla actual: el precio unitario ya incluye IGV
  - el ticket muestra `Subtotal`, `IGV (18%)` y `Total` con los montos reales de la venta
  - vista previa del ticket tras la venta configurable (`MostrarVistaPreviaTicket`)
  - aviso de stock minimo compacto al cobrar (conteo + productos mas bajos)

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

### Base tributaria en compras y ventas
- Estado: `Implementado`
- Alcance:
  - tablas `Compras` y `Ventas` persistiendo `SubTotal`, `IGV` y `Total`
  - calculo inicial con IGV incluido en precio/costo unitario
  - formula actual de cabecera:
    - `SubTotal = Total / 1.18`
    - `IGV = Total - SubTotal`
- Nota:
  - esta base sirve para preparar SUNAT, pero todavia no reemplaza una logica tributaria completa por tipo de documento o afectacion

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

## 7. Configuracion de empresa

### Pantalla de configuracion
- Estado: `Implementado`
- Ruta: `/configuracion` (solo rol `admin`)
- Tabla: `ConfiguracionEmpresa` (fila unica Id = 1)
- Campos:
  - nombre comercial, razon social, RUC
  - direccion, telefono
  - eslogan/rubro del ticket
  - titulo del documento
  - etiqueta de cliente
  - mensaje de cierre (2 lineas)
  - mostrar vista previa del ticket tras la venta (`MostrarVistaPreviaTicket`)
  - stock minimo global de alerta (`StockMinimoDefault`, por defecto `5`)
- Endpoints: `GET /api/company` y `PUT /api/company`
- El ticket de ventas y la pantalla de productos cargan los datos desde la API al iniciar
- Al cambiar el stock minimo, el backend sincroniza `Productos.StockMinimo` de todo el inventario
- Preparado para extenderse con series y correlativos en Fase 2

---

## 8. Proximas implementaciones sugeridas

1. Modelo documental de boleta/factura
2. Series y correlativos
3. Validacion real con ZKTeco
4. Reintentos automaticos con limite
5. Seleccion/configuracion de impresora desde UI o archivo
6. Preparacion SUNAT

---

## 9. Mantenimiento SQL

### Script unico de despliegue
- Estado: `Implementado`
- Script recomendado:
  - `db/minimarket.safe-upgrade.sql`
- Incluye:
  - creacion de base si no existe
  - creacion o actualizacion del login `minimarket_user` dentro de un `TRY/CATCH`
    (tolerante a permisos: si se ejecuta como `minimarket_user` no aborta, solo avisa)
  - creacion del usuario dentro de `MinimarketDb`
  - permisos `db_owner`
  - esquema idempotente sin `DROP TABLE` / `DELETE` (no borra datos)
  - manejo de unicidades de `Productos` como `UNIQUE CONSTRAINT` o como indice
    (detecta cual existe y la elimina/recrea segun corresponda; homogeneo en dev y produccion)
  - sincronizacion del stock minimo de los productos con `ConfiguracionEmpresa.StockMinimoDefault`
- Datos base:
  - categorias y usuarios `admin`/`cajero` solo se insertan si no existen
  - el script ya no resetea contraseñas existentes ni inserta productos/proveedor de ejemplo

### Script destructivo de desarrollo
- Estado: `Implementado`
- Script:
  - `db/minimarket.sql`
- Nota:
  - usarlo solo cuando quieras recrear toda la base desde cero en desarrollo
