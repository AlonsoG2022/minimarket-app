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
  - importacion y exportacion Excel (con columna `NombreCorto`)
  - al importar, las categorias que no existen se crean automaticamente
  - el Excel exportado trae dos hojas: `Productos` y `Categorias`
  - fecha de caducidad
  - stock minimo global configurable desde `Configuracion` (no editable por producto)
  - aviso de stock minimo compacto (conteo + productos mas bajos)
  - nombre corto por producto (`NombreCorto`) para el ticket; editable en el formulario,
    con sugerencia en vivo y autogeneracion en el backend si llega vacio (formulario e import)

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
  - el snapshot encolado es autocontenido (datos de empresa + `SubTotal`/`IGV`/`Total`),
    de modo que el ticket impreso queda identico a la vista previa web; ambos renderers
    (.NET y Java) pintan ese snapshot con respaldo a su config local para tickets antiguos
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

### Tema de la aplicacion
- Estado: `Implementado`
- Combo en la pantalla de Configuracion (solo `admin`) para cambiar el tema visual de la app
- Temas disponibles:
  - `orange` (por defecto)
  - `dark` (oscuro, predominio de grises)
  - `light` (claro, predominio de azul claro)
  - `el11` (verde neon sobre negro, inspirado en el letrero del local; en prueba)
- Como funciona:
  - variables CSS por tema en `styles.css` + atributo `data-theme` en el elemento raiz
  - `ThemeService` aplica el tema y lo cachea en `localStorage` (se aplica al instante al iniciar para evitar parpadeo)
  - es configuracion **global**: se guarda en `ConfiguracionEmpresa.Tema` y la app la sincroniza al cargar
  - el combo muestra la vista previa en vivo; al guardar queda como tema de la tienda
  - el backend valida el tema (solo `orange`/`dark`/`light`, por defecto `orange`)
- Nota: el ticket de la vista previa mantiene colores fijos de papel (no cambia con el tema)

---

## 8. Proximas implementaciones sugeridas

1. Modelo documental de boleta/factura
2. Series y correlativos
3. Validacion real con ZKTeco
4. Reintentos automaticos con limite
5. Seleccion/configuracion de impresora desde UI o archivo
6. Preparacion SUNAT
7. Sincronizacion de catalogo de proveedores (ver seccion 10)

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

---

## 10. Sincronizacion de catalogo de proveedores

### Sincronizacion con proveedor Coca-Cola (AIC Digital / Arca Continental)
- Estado: `Pendiente`
- Contexto:
  - ~40% del catalogo actual se compra a este proveedor y la proporcion seguira creciendo
  - el proveedor confirmo que no tiene API publica ni catalogo en Excel; los datos solo estan en su portal web
  - hoy se completa costo y precio sugerido a mano, producto por producto, desde la web del proveedor
- Investigacion ya realizada (idea registrada, sin desarrollar todavia):
  - portal: `https://acdigitalweb.azurewebsites.net/catalog-v2/all`
  - API de categorias: `GET https://briolightapimgmt.arcacontal.com/product/api/v1/Category/GetHomePageCategories?customerId=2898397`
  - API de productos por categoria: `GET https://briolightapimgmt.arcacontal.com/product/api/v1/Portfolio?businessUnitId=4&CategoryId={id}&Phrase=&Type=4&Limit=500&Offset=1&CustomerId=2898397&order=0&searchCriteria=`
    - `CategoryId` sale del `id` de cada categoria devuelta por la API anterior
    - fijos para esta cuenta: `businessUnitId=4`, `Type=4`, `Limit=500`, `CustomerId=2898397`
  - ambas APIs son internas (no oficiales/no documentadas) y requieren token Bearer de la sesion activa del navegador; no hay credenciales de API oficiales
- Estrategia de token (decidida): manual
  - el usuario copia el token Bearer desde el DevTools de su sesion activa en el portal del proveedor
  - lo pega en un campo de configuracion del sistema (pantalla a definir, ej. extension de `/configuracion` o pantalla propia)
  - un boton "Sincronizar con AIC Digital" dispara el proceso usando ese token pegado
  - no se automatiza el login ni se guarda la contrasena del proveedor
  - si el token esta vencido o la API responde 401, el sistema debe avisar claramente que hay que volver a pegar un token nuevo (no reintentar solo ni fallar en silencio)
  - no es un job programado: la sincronizacion es una accion manual que el usuario dispara cuando la necesita
- Mapeo de campos definido (API del proveedor -> tabla `Productos`):

  | Campo en `Productos` | Valor a insertar |
  |----------------------|------------------|
  | `Sku` | propiedad `sku` de la respuesta; si no viene, autogenerarlo como ya lo hace hoy el sistema |
  | `Nombre` | propiedad `longDescription` |
  | `NombreCorto` | propiedad `shortDescription` |
  | `Costo` | `customerPrice / units` |
  | `Precio` | `salePrice / units`, redondeado a la decima mas cercana (1 decimal, medio hacia arriba). Ej.: `5.51` -> `5.50`, `5.55` -> `5.60` |
  | `CodigoBarras` | vacio (se completa luego, escaneo manual) |
  | `CodigoBarrasCompra` | vacio (se completa luego, escaneo manual) |
  | `Descripcion` | vacio (se completa luego) |
  | `FechaCaducidad` | fecha actual + 2 anios |
  | `UnidadVenta` | `"Unidad"` |
  | `UnidadCompra` | `"Unidad"` |
  | `UnidadesPorCompra` | `1` |
  | `Activo` | `1` |
  | `CategoriaId` | propiedad `categoryId` de la respuesta del proveedor |

- Nueva tabla `ProveedorProducto` (historico de costo por proveedor):

  | Campo | Valor a insertar |
  |-------|------------------|
  | `ProveedorId` | `Id` de `Proveedores` donde `NumeroDocumento` = numero de documento del proveedor seleccionado en la pantalla de sincronizacion |
  | `ProductoId` | `Id` del producto insertado/actualizado en `Productos` |
  | `UltimoCosto` | el ultimo valor insertado en el campo `Precio` del producto (sirve como historico) |
  | `Fecha` | fecha actual |

- Pantalla de configuracion de la sincronizacion (Angular admin):
  - una caja de texto para pegar el token Bearer (obligatoria)
  - un combo para seleccionar el proveedor; internamente se usa su `NumeroDocumento` (obligatorio)
  - ambos datos son obligatorios para poder ejecutar la sincronizacion del catalogo Coca-Cola / AIC Digital
  - boton "Sincronizar con AIC Digital" que dispara el proceso con el token pegado y el proveedor elegido

- Pendiente de definir en diseno tecnico (antes de implementar):
  - donde corre el proceso: `.NET`, `Java` o ambos (paridad de backends)
  - logica de alta vs. actualizacion cuando el `Sku` ya existe en `Productos`
  - manejo de paginacion/`Offset` si una categoria supera `Limit=500` productos
  - manejo de errores si cambia la estructura de las APIs del proveedor (al ser internas pueden cambiar sin aviso)

- Sugerencias / puntos a confirmar antes de implementar (ver detalle en `docs/PROJECT_CONTEXT.md`):
  - `CategoriaId`: la propiedad `categoryId` del proveedor NO coincide con los `Id` de la tabla local `Categorias`; hay que mapearla o crear/buscar la categoria local antes de guardar, o el FK quedara invalido
  - `UltimoCosto` guarda el `Precio` (no el `Costo`); confirmar si es intencional o si deberia guardar `Costo`
  - el redondeo se aplica solo a `Precio`; confirmar si `Costo` tambien debe redondearse
  - proteger la division contra `units = 0` (evitar division por cero)
  - `shortDescription` puede exceder los 60 caracteres de `NombreCorto`; truncar si hace falta
  - validar que `longDescription` no exceda el largo de `Nombre`
  - el `Stock` no lo entrega el proveedor: los productos sincronizados quedan en `0` y el stock entra por compras (regla actual del sistema)
  - `ProveedorProducto` deberia tener una PK propia (`Id` identity) y FKs a `Proveedores` y `Productos`; al ser historico, se conservan multiples filas por producto/proveedor en el tiempo
- Nota: contexto de negocio completo en `docs/PROJECT_CONTEXT.md`
