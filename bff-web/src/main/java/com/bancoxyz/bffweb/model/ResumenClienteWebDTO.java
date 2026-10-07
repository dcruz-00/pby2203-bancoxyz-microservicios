package com.bancoxyz.bffweb.model;

import java.util.List;

/**
 * Vista completa del cliente para la banca web, armada con datos de clientes,
 * cuentas y pagos en una sola respuesta.
 *
 * @param advertencias secciones que no se pudieron obtener (el resto de la
 *                     respuesta sigue siendo válida)
 */
public record ResumenClienteWebDTO(
        ClienteWebDTO cliente,
        List<CuentaWebDTO> cuentas,
        Double saldoTotal,
        List<PagoWebDTO> ultimosPagos,
        List<NotificacionWebDTO> notificaciones,
        List<String> advertencias
) {}
