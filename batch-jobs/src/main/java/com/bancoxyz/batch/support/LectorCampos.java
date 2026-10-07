package com.bancoxyz.batch.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Conversión tolerante de los campos de los CSV legacy.
 *
 * Fechas: el legacy mezcla cuatro formatos. Se aceptan todos y se normalizan a
 * LocalDate. En los formatos con el día primero se verificó en los datos que el
 * primer número supera 12 en cientos de filas y el segundo nunca: es día-mes-año.
 *
 * Números vacíos: se convierten en null para que el processor los rechace con un
 * motivo claro ("monto vacío"), en vez de un error genérico de lectura.
 */
public final class LectorCampos {

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd"),
            DateTimeFormatter.ofPattern("uuuu/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-uuuu"),
            DateTimeFormatter.ofPattern("dd/MM/uuuu"));

    private LectorCampos() {
    }

    /** Devuelve null si el texto está vacío; lanza DateTimeParseException si no calza ningún formato. */
    public static LocalDate fecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String valor = texto.trim();
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(valor, formato);
            } catch (DateTimeParseException ignorada) {
                // se prueba el siguiente formato
            }
        }
        throw new DateTimeParseException("Fecha con formato no reconocido: " + valor, valor, 0);
    }

    public static Double decimal(String texto) {
        return texto == null || texto.isBlank() ? null : Double.valueOf(texto.trim());
    }

    public static Integer entero(String texto) {
        return texto == null || texto.isBlank() ? null : Integer.valueOf(texto.trim());
    }

    public static Long largo(String texto) {
        return texto == null || texto.isBlank() ? null : Long.valueOf(texto.trim());
    }

    public static String texto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
