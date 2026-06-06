namespace Minimarket.PrintWorker.Options;

public class PrintingOptions
{
    public int PollSeconds { get; set; } = 5;
    public string? PrinterName { get; set; }
    public string BusinessName { get; set; } = "Minimarket";
    public int LineWidth { get; set; } = 42;
}
