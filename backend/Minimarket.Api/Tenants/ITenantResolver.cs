namespace Minimarket.Api.Tenants;

public interface ITenantResolver
{
    Task<TenantRecord?> ResolveAsync(string? host, CancellationToken cancellationToken = default);
}
