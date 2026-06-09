# Project Context: Minimarket POS

## Objetivo del sistema
Aplicacion para operacion diaria de un minimarket con:

- autenticacion por usuario
- ventas
- compras
- inventario
- proveedores
- categorias
- caja por turnos
- ticket imprimible
- preparacion para facturacion electronica y envio a SUNAT

Este archivo sirve como contexto base para cualquier implementacion futura.

---

## Stack actual

### Frontend
- Angular
- rutas protegidas por rol
- despliegue web en IIS usando `dist/minimarket1-app/browser`

### Backend
- `.NET` API en `backend/Minimarket.Api`
- `Java Spring Boot` API en `backend-java/minimarket-api-java`
- worker local `.NET` en `backend/Minimarket.PrintWorker`
- worker local `Java Spring Boot` en `backend-java/minimarket-print-worker-java`
- ambos backends API deben mantenerse alineados funcionalmente

### Base de datos
- SQL Server
- scripts base:
  - `db/minimarket.safe-upgrade.sql` -> script unico recomendado para instalar o actualizar sin perder datos
  - `db/minimarket.sql` -> reseteo destructivo solo para desarrollo

---

## Principios del proyecto

1. Todo cambio funcional importante debe quedar alineado en:
   - Angular
   - backend `.NET`
   - backend `Java`
   - SQL

2. Si una mejora toca reglas de negocio, no debe resolverse solo en frontend.

3. Si una implementacion es temporal o primera fase, debe quedar documentada aqui o en el roadmap.

4. Para produccion actual, el backend mas usado es `Java`, pero la paridad con `.NET` se mantiene.

---

## Estado funcional actual

### Autenticacion
- login implementado
- sesion local en frontend
- roles iniciales:
  - `admin`
  - `cajero`

### Ventas
- busqueda rapida de productos
- soporte de lector de codigo de barras
- usuario autenticado asociado a la venta
- aviso visual de stock minimo
- ticket imprimible desde navegador

### Caja
- apertura de caja con monto inicial
- movimientos manuales:
  - ingreso
  - retiro
  - gasto
- ventas bloqueadas si no hay caja abierta
- ventas en efectivo suman automaticamente al disponible esperado
- cierre de caja con monto contado y diferencia

### Productos
- creacion y edicion
- codigo de barras unico para compra y venta
- stock minimo fijo en `5`
- importacion masiva desde Excel
- exportacion de productos

### Compras
- compras por proveedor
- productos con unidades de compra y venta
- soporte para compras por paquete y venta por unidad
- alta rapida de producto desde compra si el codigo no existe

### Configuracion de empresa
- pantalla accesible solo para `admin` en `/configuracion`
- datos almacenados en tabla `ConfiguracionEmpresa` (fila unica con Id = 1)
- campos configurables:
  - nombre comercial
  - razon social
  - RUC
  - direccion
  - telefono
  - eslogan / rubro
  - titulo del ticket
  - etiqueta de cliente
  - mensaje de cierre (linea 1 y 2)
- endpoint `GET /api/company` y `PUT /api/company` disponible en .NET y Java
- el ticket de venta carga estos datos desde la API al iniciar el componente
- preparado para extenderse con series, correlativos y datos SUNAT en Fase 2

### Ticket e impresion
- ticket de navegador en primera fase
- vista previa luego de la venta
- detalle de productos vendido en el ticket
- cola `TrabajosImpresion` creada para tickets
- cada venta encola automaticamente un trabajo de impresion
- worker local disponible en `.NET` y `Java` para procesar tickets
- estados actuales de impresion:
  - `pendiente`
  - `imprimiendo`
  - `impreso`
  - `error`
- reencolado manual disponible desde ventas
- roadmap visual de ticket definido en dos fases:
  - `Fase 1`: implementada con encabezado comercial, datos de venta, cliente simple, detalle tabular por producto, subtotal, total y mensaje final
  - `Fase 2`: ticket documental preparado para boleta/factura con serie, correlativo, subtotal, IGV, monto en letras y base para SUNAT

---

## Reglas de negocio importantes

### Codigo de barras
- un solo codigo de barras puede usarse para compra y venta del mismo producto
- no debe repetirse entre productos distintos

### Stock
- el alta manual de producto inicia con `stock = 0`
- el stock real debe entrar por compras

### Stock minimo
- valor fijo global actual: `5`
- no editable desde la UI

### Caja
- cada venta debe estar asociada a una caja abierta
- solo ventas con metodo `Efectivo` afectan el efectivo esperado de caja

### Impresion local
- por ahora solo se procesa `ticket`
- el worker lee directamente la tabla `TrabajosImpresion`
- la cola guarda un snapshot JSON del ticket para no depender del estado futuro del producto
- si la cola de impresion falla, la venta igual debe registrarse
- aun no se implementan boleta, factura, serie, correlativo ni estados SUNAT
- mientras la impresion automatica conviva con la vista previa web, el modal posterior a la venta funciona como respaldo manual y confirmacion visual

---

## Pendientes mayores

1. Validacion fisica con impresora ZKTeco
2. Boleta / factura con serie y correlativo
3. Preparacion estructural para SUNAT
4. Mejoras visuales y operativas de ticketera
5. Series y correlativos (tabla `SeriesDocumentos`) para Fase 2 del ticket

---

## Nota de mantenimiento
Cada vez que se implemente una funcionalidad relevante, este archivo debe actualizarse para reflejar:

- que quedo implementado
- en que fase esta
- si la solucion es temporal o definitiva
