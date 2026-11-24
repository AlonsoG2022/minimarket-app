using Microsoft.Data.SqlClient;

namespace Minimarket.Api.Tenants;

public sealed class TenantResolver(IConfiguration configuration) : ITenantResolver
{
    public async Task<TenantRecord?> ResolveAsync(string? host, CancellationToken cancellationToken = default)
    {
        var connectionString = configuration.GetConnectionString("MirooCoreConnection");
        if (string.IsNullOrWhiteSpace(connectionString))
        {
            throw new InvalidOperationException("MirooCoreConnection es obligatoria cuando MirooTenancy:Enabled=true.");
        }

        var slug = ResolveSlug(host);
        await using var connection = new SqlConnection(connectionString);
        await connection.OpenAsync(cancellationToken);
        await using var command = new SqlCommand("""
            SELECT Id, Code, Name, Slug, DatabaseName, IsActive
            FROM dbo.Tenants
            WHERE Slug = @slug
            """, connection);
        command.Parameters.AddWithValue("@slug", slug);

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        if (!await reader.ReadAsync(cancellationToken))
        {
            return null;
        }

        var tenant = new TenantRecord(
            reader.GetInt32(0),
            reader.GetString(1),
            reader.GetString(2),
            reader.GetString(3),
            reader.GetString(4),
            reader.GetBoolean(5));

        var operationalDatabase = configuration["MirooTenancy:OperationalDatabaseName"] ?? "MinimarketDb";
        return tenant.IsActive && string.Equals(tenant.DatabaseName, operationalDatabase, StringComparison.OrdinalIgnoreCase)
            ? tenant
            : null;
    }

    private string ResolveSlug(string? host)
    {
        if (string.IsNullOrWhiteSpace(host))
        {
            return LocalHostSlug();
        }

        var normalizedHost = host.Split(':')[0].ToLowerInvariant();
        if (normalizedHost is "localhost" or "127.0.0.1" or "::1")
        {
            return LocalHostSlug();
        }

        var dotIndex = normalizedHost.IndexOf('.');
        return dotIndex > 0 ? normalizedHost[..dotIndex] : normalizedHost;
    }

    private string LocalHostSlug() => configuration["MirooTenancy:LocalHostSlug"] ?? "market11";
}
