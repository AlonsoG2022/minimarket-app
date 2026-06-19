package com.minimarket.api.util;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Genera un nombre corto (para el ticket) a partir del nombre largo de un producto,
 * quitando palabras de relleno, abreviando terminos comunes y limitando la longitud.
 * Se usa como respaldo cuando no se ingresa un nombre corto manualmente.
 */
public final class ShortNameGenerator {

    private static final int MAX_LENGTH = 60;
    private static final int TARGET_LENGTH = 26;

    private static final Pattern SIZE = Pattern.compile(
        "(\\d+([.,]\\d+)?\\s?(ml|lt|l|kg|g|cm|mm|m)\\b)|(\\d+\\s?(uni|unidades|rollos?|latas?|hojas?|packs?))",
        Pattern.CASE_INSENSITIVE);

    private static final String[] FILLER = {
        "sabor", "en polvo", "pet", "retornable", "aromatizado", "aromatizada", "premium", "gourmet",
        "instantanea", "instantaneo", "con vitamina c", "fragancia", "frangancia", "de mesa", "del valle",
        "original", "spray", "aerosol", "liquido", "complete", "intense", "pro-v", "pro v", "sin sal",
        "doble hoja", "mayor ahorro", "mayor resistencia", "aroma a", "aroma", "multiuso", "clasica", "clasico"
    };

    private static final String[][] ABBR = {
        {"head & shoulders", "H&S"}, {"head shoulder", "H&S"}, {"johnnie walker", "JW"},
        {"faber-castell", "Faber"}, {"faber castell", "Faber"}, {"antitranspirante", "Antitr"},
        {"acondicionador", "Acond"}, {"desodorante", "Desod"}, {"detergente", "Deterg"},
        {"papel higienico", "PH"}, {"barra de chocolate", "Choc"}, {"tableta de chocolate", "Choc"},
        {"gaseosa", ""}, {"lavavajilla", "Lavavaj"}, {"ambientador", "Ambient"}, {"prestobarba", "Prestob"},
        {"pasta dental", "P.Dental"}, {"cepillo dental", "Cep.Dental"}, {"servilletas", "Servill"},
        {"limpiatodo", "Limpiat"}, {"galletas", "Gall"}, {"galleta", "Gall"}, {"shampoo", "Sh"}, {"yogurt", "Yog"}
    };

    private ShortNameGenerator() {
    }

    /** Devuelve el nombre corto ingresado o, si esta vacio, uno generado del nombre. */
    public static String resolve(String provided, String name) {
        var p = provided == null ? "" : provided.trim();
        var result = p.isBlank() ? generate(name) : p;
        return result.length() > MAX_LENGTH ? result.substring(0, MAX_LENGTH) : result;
    }

    public static String generate(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        var clean = stripAccents(name.trim());
        var size = "";
        var m = SIZE.matcher(clean);
        while (m.find()) {
            size = m.group().replaceAll("\\s+", "").replaceAll("(?i)unidades", "uni");
        }
        var s = SIZE.matcher(clean).replaceAll(" ");

        for (var pair : ABBR) {
            s = Pattern.compile(Pattern.quote(pair[0]), Pattern.CASE_INSENSITIVE)
                .matcher(s).replaceAll(Matcher.quoteReplacement(pair[1]));
        }
        for (var word : FILLER) {
            s = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE)
                .matcher(s).replaceAll(" ");
        }
        s = s.replaceAll("\\s+", " ").trim();

        var limit = TARGET_LENGTH - (size.isEmpty() ? 0 : size.length() + 1);
        if (s.length() > limit) {
            var words = s.split(" ");
            if (words.length > 1) {
                var first = words[0];
                var last = words[words.length - 1];
                var acc = new StringBuilder(first);
                for (var i = 1; i < words.length - 1; i++) {
                    if ((acc + " " + words[i] + " " + last).length() <= limit) {
                        acc.append(" ").append(words[i]);
                    }
                }
                var candidate = (acc + " " + last).trim();
                s = candidate.length() <= limit ? candidate : first + " " + last;
            } else {
                s = s.substring(0, Math.min(limit, s.length()));
            }
        }

        return (size.isEmpty() ? s : s + " " + size).trim();
    }

    private static String stripAccents(String value) {
        var normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }
}
