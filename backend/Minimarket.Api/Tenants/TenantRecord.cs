namespace Minimarket.Api.Tenants;

public sealed record TenantRecord(
    int Id,
    string Code,
    string Name,
    string Slug,
    string DatabaseName,
    bool IsActive);
