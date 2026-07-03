namespace Minimarket.Api.DTOs;

// Imagen de la boleta (base64) que sube el usuario para leerla con IA.
public record ReceiptScanRequestDto(string ImageBase64, string? MediaType);

// Una linea propuesta tras leer la boleta (con match sugerido contra el catalogo).
public record ReceiptScanLineDto(
    string Description,        // nombre tal como sale en la boleta
    decimal Quantity,
    int PackUnits,            // unidades por paquete/tira (para pasar a costo por unidad)
    decimal UnitCost,         // costo por unidad con IGV
    string SuggestedName,     // nombre limpio propuesto
    string SuggestedShortName,
    string SuggestedCategory,
    decimal SuggestedPrice,   // precio de venta sugerido (costo x margen, redondeado)
    int? MatchProductId,      // mejor coincidencia en el catalogo (null = crear nuevo)
    string? MatchProductName,
    double MatchScore);

public record ReceiptScanResultDto(
    string SupplierName,
    string? SupplierRuc,
    int? SupplierId,          // proveedor emparejado por RUC (null si no existe)
    IReadOnlyCollection<ReceiptScanLineDto> Lines,
    IReadOnlyCollection<string> Warnings);

// Item confirmado por el usuario en la pantalla de revision.
public record ReceiptConfirmItemDto(
    string Action,            // "match" | "create" | "skip"
    int? ProductId,           // para "match"
    string Name,
    string ShortName,
    string CategoryName,
    decimal Price,
    decimal Cost);

public record ReceiptConfirmRequestDto(
    int SupplierId,
    IReadOnlyCollection<ReceiptConfirmItemDto> Items);

public record ReceiptConfirmResultDto(
    int Created,
    int Updated,
    int CostsRecorded,
    IReadOnlyCollection<string> Warnings);
