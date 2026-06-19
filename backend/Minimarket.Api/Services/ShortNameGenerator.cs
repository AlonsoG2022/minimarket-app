using System.Globalization;
using System.Text;
using System.Text.RegularExpressions;

namespace Minimarket.Api.Services;

/// <summary>
/// Genera un nombre corto (para el ticket) a partir del nombre largo de un producto,
/// quitando palabras de relleno, abreviando terminos comunes y limitando la longitud.
/// Se usa como respaldo cuando no se ingresa un nombre corto manualmente.
/// </summary>
public static class ShortNameGenerator
{
    private const int MaxLength = 60;
    private const int TargetLength = 26;

    private static readonly Regex SizeRegex = new(
        @"(\d+([.,]\d+)?\s?(ml|lt|l|kg|g|cm|mm|m)\b)|(\d+\s?(uni|unidades|rollos?|latas?|hojas?|packs?))",
        RegexOptions.IgnoreCase | RegexOptions.Compiled);

    private static readonly string[] Filler =
    {
        "sabor", "en polvo", "pet", "retornable", "aromatizado", "aromatizada", "premium", "gourmet",
        "instantanea", "instantaneo", "con vitamina c", "fragancia", "frangancia", "de mesa", "del valle",
        "original", "spray", "aerosol", "liquido", "complete", "intense", "pro-v", "pro v", "sin sal",
        "doble hoja", "mayor ahorro", "mayor resistencia", "aroma a", "aroma", "multiuso", "clasica", "clasico"
    };

    private static readonly (string From, string To)[] Abbr =
    {
        ("head & shoulders", "H&S"), ("head shoulder", "H&S"), ("johnnie walker", "JW"),
        ("faber-castell", "Faber"), ("faber castell", "Faber"), ("antitranspirante", "Antitr"),
        ("acondicionador", "Acond"), ("desodorante", "Desod"), ("detergente", "Deterg"),
        ("papel higienico", "PH"), ("barra de chocolate", "Choc"), ("tableta de chocolate", "Choc"),
        ("gaseosa", ""), ("lavavajilla", "Lavavaj"), ("ambientador", "Ambient"), ("prestobarba", "Prestob"),
        ("pasta dental", "P.Dental"), ("cepillo dental", "Cep.Dental"), ("servilletas", "Servill"),
        ("limpiatodo", "Limpiat"), ("galletas", "Gall"), ("galleta", "Gall"), ("shampoo", "Sh"), ("yogurt", "Yog")
    };

    /// <summary>Devuelve el nombre corto ingresado o, si esta vacio, uno generado del nombre.</summary>
    public static string Resolve(string? provided, string name)
    {
        var p = provided?.Trim();
        var result = string.IsNullOrWhiteSpace(p) ? Generate(name) : p;
        return result.Length > MaxLength ? result[..MaxLength] : result;
    }

    public static string Generate(string name)
    {
        if (string.IsNullOrWhiteSpace(name)) return string.Empty;

        var clean = StripAccents(name.Trim());
        var size = NormalizeSize(SizeRegex.Matches(clean).Cast<Match>().LastOrDefault()?.Value);
        var s = SizeRegex.Replace(clean, " ");

        foreach (var (from, to) in Abbr)
        {
            s = Regex.Replace(s, Regex.Escape(from), to, RegexOptions.IgnoreCase);
        }

        foreach (var word in Filler)
        {
            s = Regex.Replace(s, $@"\b{Regex.Escape(word)}\b", " ", RegexOptions.IgnoreCase);
        }

        s = Regex.Replace(s, @"\s+", " ").Trim();

        var limit = TargetLength - (size.Length > 0 ? size.Length + 1 : 0);
        if (s.Length > limit)
        {
            var words = s.Split(' ', StringSplitOptions.RemoveEmptyEntries);
            if (words.Length > 1)
            {
                var first = words[0];
                var last = words[^1];
                var acc = first;
                foreach (var x in words[1..^1])
                {
                    if ((acc + " " + x + " " + last).Length <= limit) acc += " " + x;
                }

                var candidate = (acc + " " + last).Trim();
                s = candidate.Length <= limit ? candidate : first + " " + last;
            }
            else
            {
                s = s[..Math.Min(limit, s.Length)];
            }
        }

        return (s + (size.Length > 0 ? " " + size : "")).Trim();
    }

    private static string NormalizeSize(string? raw)
    {
        if (string.IsNullOrWhiteSpace(raw)) return string.Empty;
        return Regex.Replace(raw, @"\s+", string.Empty)
            .Replace("unidades", "uni", StringComparison.OrdinalIgnoreCase);
    }

    private static string StripAccents(string value)
    {
        var normalized = value.Normalize(NormalizationForm.FormD);
        var builder = new StringBuilder();
        foreach (var c in normalized)
        {
            if (CharUnicodeInfo.GetUnicodeCategory(c) != UnicodeCategory.NonSpacingMark)
            {
                builder.Append(c);
            }
        }

        return builder.ToString().Normalize(NormalizationForm.FormC);
    }
}
