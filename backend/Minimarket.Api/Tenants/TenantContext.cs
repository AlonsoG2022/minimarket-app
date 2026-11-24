namespace Minimarket.Api.Tenants;

public sealed class TenantContext
{
    public TenantRecord? Current { get; set; }
}
