using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.FileProviders;
using Minimarket.Api.Data;
using Minimarket.Api.Helpers;
using Minimarket.Api.Repositories;
using Minimarket.Api.Services;
using Minimarket.Api.Tenants;

var builder = WebApplication.CreateBuilder(args);
builder.Configuration.AddJsonFile("appsettings.Local.json", optional: true, reloadOnChange: true);

builder.Services.AddControllers();
builder.Services.AddOpenApi();
builder.Services.AddHttpClient();
builder.Services.AddHealthChecks();
builder.Services.AddDbContext<MinimarketDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("MinimarketConnection")));

builder.Services.AddScoped<TenantContext>();
builder.Services.AddScoped<ITenantResolver, TenantResolver>();

builder.Services.AddCors(options =>
{
    options.AddPolicy("frontend", policy =>
    {
        var allowedOrigins = builder.Configuration.GetSection("Cors:AllowedOrigins").Get<string[]>()
            ?? ["http://localhost:4200", "http://localhost:8081", "http://localhost:8080"];
        policy
            .WithOrigins(allowedOrigins)
            .AllowAnyHeader()
            .AllowAnyMethod();
    });
});

builder.Services.AddScoped<ICompanyRepository, CompanyRepository>();
builder.Services.AddScoped<IProductRepository, ProductRepository>();
builder.Services.AddScoped<ICategoryRepository, CategoryRepository>();
builder.Services.AddScoped<ISupplierRepository, SupplierRepository>();
builder.Services.AddScoped<IUserRepository, UserRepository>();
builder.Services.AddScoped<ICashSessionRepository, CashSessionRepository>();
builder.Services.AddScoped<ISaleRepository, SaleRepository>();
builder.Services.AddScoped<IPrintJobRepository, PrintJobRepository>();
builder.Services.AddScoped<IPurchaseRepository, PurchaseRepository>();
builder.Services.AddScoped<ISupplierProductRepository, SupplierProductRepository>();

builder.Services.AddScoped<ICompanyService, CompanyService>();
builder.Services.AddScoped<IProductService, ProductService>();
builder.Services.AddScoped<ICategoryService, CategoryService>();
builder.Services.AddScoped<ISupplierService, SupplierService>();
builder.Services.AddScoped<IUserService, UserService>();
builder.Services.AddScoped<ICashSessionService, CashSessionService>();
builder.Services.AddScoped<ISaleService, SaleService>();
builder.Services.AddScoped<IPrintJobService, PrintJobService>();
builder.Services.AddScoped<IPurchaseService, PurchaseService>();
builder.Services.AddScoped<IReportService, ReportService>();
builder.Services.AddScoped<ISupplierSyncService, SupplierSyncService>();
builder.Services.AddScoped<IReceiptService, ReceiptService>();
builder.Services.AddScoped<IPasswordHasher, Sha256PasswordHasher>();

var app = builder.Build();
var spaRootPath = Path.Combine(app.Environment.ContentRootPath, "wwwroot", "browser");
var spaExists = Directory.Exists(spaRootPath) && File.Exists(Path.Combine(spaRootPath, "index.html"));

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}


app.UseCors("frontend");
if (builder.Configuration.GetValue<bool>("MirooTenancy:Enabled"))
{
    app.UseMiddleware<TenantResolutionMiddleware>();
}
if (spaExists)
{
    app.UseDefaultFiles(new DefaultFilesOptions
    {
        FileProvider = new PhysicalFileProvider(spaRootPath)
    });

    app.UseStaticFiles(new StaticFileOptions
    {
        FileProvider = new PhysicalFileProvider(spaRootPath)
    });
}

app.UseAuthorization();
app.MapControllers();
app.MapHealthChecks("/actuator/health");

if (spaExists)
{
    app.MapFallback(async context =>
    {
        if (context.Request.Path.StartsWithSegments("/api"))
        {
            context.Response.StatusCode = StatusCodes.Status404NotFound;
            return;
        }

        context.Response.ContentType = "text/html; charset=utf-8";
        await context.Response.SendFileAsync(Path.Combine(spaRootPath, "index.html"));
    });
}

app.Run();
