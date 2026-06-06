using Microsoft.EntityFrameworkCore;
using Minimarket.PrintWorker.Data;
using Minimarket.PrintWorker.Options;
using Minimarket.PrintWorker.Services;

var builder = Host.CreateApplicationBuilder(args);

builder.Services.Configure<PrintingOptions>(builder.Configuration.GetSection("Printing"));
builder.Services.AddDbContext<PrintWorkerDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("MinimarketConnection")));

builder.Services.AddSingleton<TicketTextRenderer>();
builder.Services.AddSingleton<IPrinterDispatcher, WindowsPrinterDispatcher>();
builder.Services.AddHostedService<PrintQueueWorker>();

var host = builder.Build();
host.Run();
