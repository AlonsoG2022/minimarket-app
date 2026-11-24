namespace Minimarket.Api.Tenants;

public sealed class TenantResolutionMiddleware(RequestDelegate next)
{
    public async Task InvokeAsync(HttpContext context, ITenantResolver tenantResolver, TenantContext tenantContext)
    {
        if (!context.Request.Path.StartsWithSegments("/api"))
        {
            await next(context);
            return;
        }

        var tenant = await tenantResolver.ResolveAsync(context.Request.Host.Host, context.RequestAborted);
        if (tenant is null)
        {
            context.Response.StatusCode = StatusCodes.Status404NotFound;
            await context.Response.WriteAsJsonAsync(new { message = "Negocio no disponible." }, context.RequestAborted);
            return;
        }

        tenantContext.Current = tenant;
        context.Items[nameof(TenantRecord)] = tenant;
        await next(context);
    }
}
