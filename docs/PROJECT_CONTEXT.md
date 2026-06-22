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
- cabecera con `SubTotal`, `IGV` y `Total` persistidos en BD

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
- stock minimo global configurable desde `Configuracion` (por defecto `5`, no editable por producto)
- aviso compacto de productos en stock minimo (resumen con conteo + los mas bajos)
- nombre corto por producto (`NombreCorto`) que se muestra en el ticket en vez del nombre largo
  - editable en el formulario de producto; si se deja vacio, el backend lo genera del nombre largo
  - en el formulario hay sugerencia en vivo mientras se escribe el nombre (se puede editar)
- importacion masiva desde Excel (incluye columna `NombreCorto`)
  - si una fila trae una categoria que no existe, el backend la crea automaticamente
- exportacion de productos en Excel con dos hojas: `Productos` y `Categorias`
  (la hoja de categorias lista las existentes al momento de exportar)

### Compras
- compras por proveedor
- productos con unidades de compra y venta
- soporte para compras por paquete y venta por unidad
- alta rapida de producto desde compra si el codigo no existe
- cabecera con `SubTotal`, `IGV` y `Total` persistidos en BD

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
  - mostrar vista previa del ticket despues de cada venta (`MostrarVistaPreviaTicket`)
  - stock minimo global de alerta (`StockMinimoDefault`, por defecto `5`)
  - tema visual de la app (`Tema`: `orange` / `dark` / `light` / `el11`, por defecto `orange`)
- endpoint `GET /api/company` y `PUT /api/company` disponible en .NET y Java
- el ticket de venta y la pantalla de productos cargan estos datos desde la API al iniciar
- al guardar el stock minimo se sincroniza `Productos.StockMinimo` de todos los productos con el valor global
- preparado para extenderse con series, correlativos y datos SUNAT en Fase 2

### Vista previa del ticket
- tras registrar la venta, el ticket se encola para impresion automatica en la etiquetera
- ademas se puede mostrar un modal de vista previa (con boton imprimir) como respaldo visual
- ese modal es configurable: si se desactiva `MostrarVistaPreviaTicket`, ya no aparece tras la venta
- el ticket muestra `Subtotal`, `IGV (18%)` y `Total` con los montos reales de la venta

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

### Montos tributarios base
- las tablas `Ventas` y `Compras` deben persistir:
  - `SubTotal`
  - `IGV`
  - `Total`
- regla actual:
  - el precio unitario de venta ya incluye IGV
  - el costo/precio unitario de compra registrado tambien se considera con IGV incluido
- calculo actual de cabecera:
  - `Total` = suma final de lineas
  - `SubTotal` = `Total / 1.18`
  - `IGV` = `Total - SubTotal`
- estos campos se guardan para preparar la futura implementacion de boleta/factura y envio a SUNAT

### Stock
- el alta manual de producto inicia con `stock = 0`
- el stock real debe entrar por compras

### Stock minimo
- valor global configurable desde `Configuracion` (campo `StockMinimoDefault`, por defecto `5`)
- es unico para todos los productos: no se edita por producto
- al cambiarlo, el backend sincroniza `Productos.StockMinimo` de todo el inventario con el nuevo valor
- los avisos de stock minimo (en Productos y al cobrar) y el conteo del dashboard usan este valor

### Caja
- cada venta debe estar asociada a una caja abierta
- solo ventas con metodo `Efectivo` afectan el efectivo esperado de caja

### Impresion local
- por ahora solo se procesa `ticket`
- el worker lee directamente la tabla `TrabajosImpresion`
- la cola guarda un snapshot JSON **autocontenido** del ticket: incluye datos de empresa, eslogan,
  titulo, etiqueta de cliente, mensajes de cierre y el desglose `SubTotal` / `IGV` / `Total`,
  tal como estaban al momento de la venta
- el ticket fisico que imprime el worker (.NET y Java) sale **identico a la vista previa web**
- si un ticket viejo en cola no trae esos datos, el worker usa como respaldo su configuracion local
- si la cola de impresion falla, la venta igual debe registrarse
- aun no se implementan boleta, factura, serie, correlativo ni estados SUNAT
- el modal de vista previa posterior a la venta funciona como respaldo manual y confirmacion visual, y es configurable (`MostrarVistaPreviaTicket`); si se desactiva, el ticket igual se imprime de forma automatica
- el ticket (fisico y vista previa) muestra el `NombreCorto` del producto si existe; si no, el nombre largo

---

## Sincronizacion de catalogo de proveedor (idea registrada, pendiente de implementar)

### Necesidad
- se esta armando en Excel el catalogo de productos con codigo de barras, costo y precio sugerido
- aproximadamente el 40% de los productos se compran al proveedor Coca-Cola (plataforma Arca Continental, portal AIC Digital) y esa proporcion seguira creciendo
- cargar manualmente costo y precio sugerido producto por producto desde la web del proveedor consume mucho tiempo
- se consulto al proveedor por una API publica o un Excel de catalogo: no existen, el unico origen de estos datos es su portal web

### Fuente de datos identificada
- portal del proveedor: `https://acdigitalweb.azurewebsites.net/catalog-v2/all` (AIC Digital)
- revisando el DevTools del navegador se identificaron 2 APIs internas que usa ese portal (no oficiales, no documentadas por el proveedor):
  1. Categorias: `GET https://briolightapimgmt.arcacontal.com/product/api/v1/Category/GetHomePageCategories?customerId=2898397`
     - devuelve la lista de categorias; cada categoria trae un `id`
  2. Productos por categoria: `GET https://briolightapimgmt.arcacontal.com/product/api/v1/Portfolio?businessUnitId=4&CategoryId={id}&Phrase=&Type=4&Limit=500&Offset=1&CustomerId=2898397&order=0&searchCriteria=`
     - `CategoryId` es el `id` devuelto por la API de categorias
     - valores fijos para esta cuenta: `businessUnitId=4`, `Type=4`, `Limit=500`, `CustomerId=2898397`
- ambas APIs requieren token de autenticacion (Bearer) que se obtiene de la sesion activa del navegador en el portal; no hay credenciales oficiales de API

### Estrategia de token (decidida)
- manual, sin automatizar el login ni guardar la contrasena del proveedor:
  1. el usuario copia el token Bearer desde el DevTools de su sesion activa en el portal del proveedor
  2. lo pega en un campo de configuracion del sistema (pantalla a definir, ej. extension de `/configuracion` o pantalla propia)
  3. un boton "Sincronizar con AIC Digital" dispara la sincronizacion usando ese token pegado
- la sincronizacion no es un job programado, es una accion manual que el usuario dispara cuando la necesita
- si el token esta vencido o la API responde 401, el sistema debe avisar claramente que hay que pegar un token nuevo (no reintentar solo ni fallar en silencio)

### Mapeo de campos propuesto (API del proveedor -> tabla `Productos`)
- `sku` del proveedor -> `SKU` (sirve para relacionar y actualizar productos ya sincronizados en corridas futuras)
- nombre del producto del proveedor -> `Nombre`
- descripcion corta del proveedor -> `NombreCorto`
- precio de venta sugerido = `salePrice / units` -> precio de venta del producto
- precio de costo = `customerPrice / units` -> costo del producto
- unidad de compra y unidad de venta -> siempre `"Unidad"` para los productos sincronizados de este proveedor
- codigo de barras: el proveedor no lo expone; se sigue completando manualmente (escaneo durante las compras), igual que hoy

### Notas
- esto ya esta listo para implementar (idea + estrategia de token decididas), todavia no se ha desarrollado nada
- al implementarse, definir si el proceso corre en `.NET`, `Java` o ambos, y donde vive exactamente el campo de token y el boton de sincronizacion en el Angular admin
- ver seguimiento detallado en `docs/IMPLEMENTATION_ROADMAP.md`

---

## Pendientes mayores

1. Validacion fisica con impresora ZKTeco
2. Boleta / factura con serie y correlativo
3. Preparacion estructural para SUNAT
4. Mejoras visuales y operativas de ticketera
5. Series y correlativos (tabla `SeriesDocumentos`) para Fase 2 del ticket
6. Evolucionar de calculo tributario fijo (IGV 18% incluido) a reglas tributarias configurables por documento/producto
7. Sincronizacion de catalogo del proveedor Coca-Cola / AIC Digital (costo y precio sugerido por SKU) - ver seccion dedicada arriba

---

## Nota de mantenimiento
Cada vez que se implemente una funcionalidad relevante, este archivo debe actualizarse para reflejar:

- que quedo implementado
- en que fase esta
- si la solucion es temporal o definitiva
