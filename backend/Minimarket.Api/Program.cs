using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.FileProviders;
using Minimarket.Api.Data;
using Minimarket.Api.Helpers;
using Minimarket.Api.Repositories;
using Minimarket.Api.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddOpenApi();
builder.Services.AddDbContext<MinimarketDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("MinimarketConnection")));

builder.Services.AddCors(options =>
{
    options.AddPolicy("frontend", policy =>
    {
        policy
            .WithOrigins("http://localhost:4200")
            .AllowAnyHeader()
            .AllowAnyMethod();
    });
});

builder.Services.AddScoped<IProductRepository, ProductRepository>();
builder.Services.AddScoped<ICategoryRepository, CategoryRepository>();
builder.Services.AddScoped<IUserRepository, UserRepository>();
builder.Services.AddScoped<ISaleRepository, SaleRepository>();

builder.Services.AddScoped<IProductService, ProductService>();
builder.Services.AddScoped<ICategoryService, CategoryService>();
builder.Services.AddScoped<IUserService, UserService>();
builder.Services.AddScoped<ISaleService, SaleService>();
builder.Services.AddScoped<IReportService, ReportService>();
builder.Services.AddScoped<IPasswordHasher, Sha256PasswordHasher>();

var app = builder.Build();
var spaRootPath = Path.Combine(app.Environment.ContentRootPath, "wwwroot", "browser");
var spaExists = Directory.Exists(spaRootPath) && File.Exists(Path.Combine(spaRootPath, "index.html"));

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}


app.UseCors("frontend");
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
