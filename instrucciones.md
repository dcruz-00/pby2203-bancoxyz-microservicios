# Instrucciones para ejecutar y probar cada componente

Paso a paso para levantar BancoXYZ y verificar cada componente: plataforma Spring Cloud, seguridad, microservicios, mensajería, BFF, gateway, resiliencia, escalado y procesos batch. La arquitectura se describe en el [README](README.md).

Los comandos están escritos para una terminal Linux o macOS (bash o zsh). Las mismas solicitudes se pueden hacer con Insomnia o Postman, desactivando la validación de certificados ("Validate certificates"), porque el certificado del proyecto es autofirmado.

## 1. Requisitos

| Herramienta | Versión | Uso |
|---|---|---|
| Docker Engine + Docker Compose | Compose 2.24 o superior | Construir y ejecutar todo el sistema |
| curl | cualquiera | Probar las APIs |
| jq | cualquiera | Extraer el token de las respuestas (`sudo dnf install jq` o `sudo apt install jq`) |
| JDK 21 (opcional) | 21 | Solo para compilar o ejecutar fuera de Docker |

Equipo recomendado: 8 GB de RAM libres (son 11 servicios Java, 4 PostgreSQL y Kafka). Los puertos usados en el equipo son 5433 a 5436, 8080, 8081, 8084, 8091 a 8093, 8443, 8761, 8888, 9000 y 9092.

## 2. Levantar el sistema

```zsh
git clone https://github.com/dcruz-00/PBY2203-bancoxyz-microservicios.git
cd PBY2203-bancoxyz-microservicios
docker compose up -d --build
```

La primera construcción descarga las dependencias de Maven de cada servicio y tarda varios minutos. Los healthchecks definen el orden de arranque: primero la infraestructura (PostgreSQL y Kafka), luego Config Server, Eureka y auth-server, y al final los microservicios, los BFF y el gateway.

Verificar el estado:

```zsh
docker compose ps -a
```

**Resultado esperado**: todos los servicios en estado `healthy`, salvo `kafka-init`, que crea los tópicos y termina, y `batch-jobs`, que ejecuta los procesos batch y termina con `Exited (0)`.

> Los servicios aparecen en Eureka unos 30 segundos después de estar `healthy`. Si una llamada a través de un BFF responde 503 justo después de arrancar, espera ese tiempo y repite.

Para seguir los logs de un servicio: `docker compose logs -f pagos`.

## 3. Plataforma Spring Cloud

### 3.1 Registro de servicios (Eureka)

Abrir `http://localhost:8761`.

**Esperado**: AUTH-SERVER, CUENTAS, PAGOS, CLIENTES, BFF-WEB, BFF-MOVIL, BFF-CAJEROS y GATEWAY en estado UP.

### 3.2 Configuración centralizada (Config Server)

```zsh
curl -s http://localhost:8888/pagos/docker | jq '.propertySources[].name'
```

**Esperado**: dos fuentes. Primero `pagos-docker.properties`, que tiene prioridad y solo sobrescribe lo que cambia en Docker, y después `pagos.properties`, con la configuración base.

### 3.3 Rutas del gateway

```zsh
curl -sk https://localhost:8443/actuator/gateway/routes | jq '.[] | {id: .route_id, uri}'
```

**Esperado**: las rutas `bff-web`, `bff-movil` y `bff-cajeros`, con destino `lb://<bff>` (balanceo de carga vía Eureka).

## 4. Seguridad: tokens OAuth 2.0

Los microservicios exigen un token JWT emitido por `auth-server`. Hay que pedir los scopes de forma explícita: si no se piden, el token no incluye ninguno.

```zsh
# Ejecutivo del banco: lectura y operación en los tres servicios
TOKEN=$(curl -s -u cliente-backoffice:backoffice-secret-2026 \
  -d grant_type=client_credentials \
  -d scope="cuentas.leer cuentas.administrar clientes.leer clientes.escribir pagos.leer pagos.operar" \
  http://localhost:9000/oauth2/token | jq -r .access_token)

# Aplicación de solo lectura
CONSULTA=$(curl -s -u cliente-consulta:consulta-secret-2026 \
  -d grant_type=client_credentials -d scope="cuentas.leer pagos.leer clientes.leer" \
  http://localhost:9000/oauth2/token | jq -r .access_token)

echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null; echo
```

**Esperado**: el contenido del token muestra `sub` (el cliente), `scope` y `iss: http://auth-server:9000`. El emisor está fijado, así que un token pedido desde el equipo es válido para los servicios dentro de Docker. Vigencia: 5 minutos; si una prueba responde 401, pide el token otra vez.

Comprobar las reglas de acceso:

```zsh
# Sin token: 401
curl -sk -o /dev/null -w "%{http_code}\n" https://localhost:8080/api/cuentas
# Token de solo lectura intentando abrir una cuenta: 403 (insufficient_scope)
curl -sk -o /dev/null -w "%{http_code}\n" -X POST https://localhost:8080/api/cuentas \
  -H "Authorization: Bearer $CONSULTA" -H "Content-Type: application/json" \
  -d '{"clienteId":1,"tipo":"ahorro","saldoInicial":0}'
```

## 5. Microservicios

Los datos iniciales incluyen 6 clientes (ids 1 a 6) y sus cuentas (101, 102, 103, 104, 107 y 108). Por ejemplo, la cuenta 101 es de John Doe (cliente 1) y tiene saldo 4700.

### 5.1 clientes

```zsh
curl -sk https://localhost:8084/api/clientes/1 -H "Authorization: Bearer $TOKEN" | jq

# Registrar un cliente (201)
curl -sk -X POST https://localhost:8084/api/clientes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"rut":"12345678-5","nombre":"Ana Pérez","email":"ana.perez@correo.cl","telefono":"+56977777777","fechaNacimiento":"1995-04-12"}' | jq

# Validación: email inválido (400 con el detalle del campo)
curl -sk -X POST https://localhost:8084/api/clientes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"rut":"1-1","nombre":"X","email":"no-es-email"}' | jq
```

**Esperado**: el registro responde 201 con `id: 7`. La segunda solicitud responde 400 con los errores de `rut` y `email`. Repetir el mismo RUT responde 409.

### 5.2 cuentas

```zsh
# Cuentas de un cliente
curl -sk "https://localhost:8080/api/cuentas?clienteId=1" -H "Authorization: Bearer $TOKEN" | jq

# Apertura para el cliente 7 (valida al titular en clientes)
curl -sk -X POST https://localhost:8080/api/cuentas \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"clienteId":7,"tipo":"ahorro","saldoInicial":0}' | jq

# Cliente inexistente: 422
curl -sk -o /dev/null -w "%{http_code}\n" -X POST https://localhost:8080/api/cuentas \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"clienteId":999,"tipo":"ahorro","saldoInicial":0}'

# Cierre de la cuenta nueva (saldo cero): estado CERRADA
curl -sk -X PATCH https://localhost:8080/api/cuentas/1001/cierre -H "Authorization: Bearer $TOKEN" | jq

# Cierre de una cuenta con saldo: 409
curl -sk -X PATCH https://localhost:8080/api/cuentas/101/cierre -H "Authorization: Bearer $TOKEN" | jq
```

**Esperado**: la cuenta nueva es la 1001, con el nombre y la edad del titular tomados de clientes. El cierre de la 101 responde 409 con el saldo actual.

### 5.3 pagos (depósitos, pagos y transferencias)

```zsh
# Depósito de 500 en la 101
curl -sk -X POST https://localhost:8081/api/pagos/depositos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"cuentaId":101,"monto":500}' | jq

# Pago de un servicio
curl -sk -X POST https://localhost:8081/api/pagos/servicios \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"cuentaId":101,"monto":120,"comercio":"Compañía eléctrica"}' | jq

# Transferencia de 1200 de la 101 a la 102 (genera además una alerta MONTO_ELEVADO)
curl -sk -X POST https://localhost:8081/api/pagos/transferencias \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"cuentaOrigenId":101,"cuentaDestinoId":102,"monto":1200}' | jq

# Saldo insuficiente: 409 y el pago queda RECHAZADO
curl -sk -X POST https://localhost:8081/api/pagos/servicios \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"cuentaId":104,"monto":50,"comercio":"Supermercado"}' | jq

# Historial de la cuenta
curl -sk "https://localhost:8081/api/pagos?cuentaId=101" -H "Authorization: Bearer $TOKEN" | jq
```

**Esperado**: los tres primeros responden 201 con `estado: COMPLETADO` y el saldo resultante: 5200, 5080 y 3880. La cuenta 104 tiene saldo 0, así que el pago responde 409 y queda `RECHAZADO` en el historial.

### 5.4 Retiro (saga con Kafka)

```zsh
CAJERO=$(curl -s -u cliente-cajero:cajero-secret-2026 \
  -d grant_type=client_credentials -d scope="cuentas.leer cuentas.retirar" \
  http://localhost:9000/oauth2/token | jq -r .access_token)

curl -sk -i -X PATCH https://localhost:8080/api/cuentas/101/retiro \
  -H "Authorization: Bearer $CAJERO" -H "Content-Type: application/json" -d '{"monto":10}'
```

**Esperado**: 200 con el nuevo saldo y la cabecera `X-Id-Operacion`. Con ese id, `GET https://localhost:8081/api/movimientos/<id>` muestra el movimiento que pagos registró al consumir `retiro-realizado`, y el log de cuentas indica `Operación ... CONFIRMADA`.

### 5.5 Eventos: notificaciones y alertas

```zsh
curl -sk https://localhost:8084/api/clientes/1/notificaciones -H "Authorization: Bearer $TOKEN" | jq
curl -sk https://localhost:8084/api/clientes/2/notificaciones -H "Authorization: Bearer $TOKEN" | jq
```

**Esperado**:

- Cliente 1: notificaciones `DEPOSITO`, `PAGO`, `TRANSFERENCIA_ENVIADA` y `ALERTA_MONTO_ELEVADO` (la transferencia de 1200 superó el umbral de 1000).
- Cliente 2: `TRANSFERENCIA_RECIBIDA`.
- Cliente 7: `ALERTA_CUENTA_CERRADA`, por el cierre de la cuenta 1001.

Tópicos y grupos de consumidores:

```zsh
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups
```

## 6. BFF a través del gateway

Todas las solicitudes de los canales entran por el gateway (`https://localhost:8443`), que las enruta al BFF correspondiente. Cada canal se autentica con sus propias credenciales; el BFF obtiene por su cuenta el token para llamar a los microservicios.

### 6.1 Web (datos completos)

```zsh
# Resumen del cliente: perfil, cuentas, saldo total, últimos pagos y notificaciones
curl -sk -u web-client:web-secret https://localhost:8443/web/clientes/1/resumen | jq

curl -sk -u web-client:web-secret https://localhost:8443/web/cuentas/101 | jq

curl -sk -u web-client:web-secret -X POST https://localhost:8443/web/transferencias \
  -H "Content-Type: application/json" -d '{"cuentaOrigenId":102,"cuentaDestinoId":101,"monto":100}' | jq
```

**Esperado**: el resumen trae las secciones completas y `advertencias: []`. La cuenta incluye todos sus campos (nombre, edad, interés, estado...).

### 6.2 Móvil (respuestas livianas)

```zsh
curl -sk -u movil-client:movil-secret https://localhost:8443/movil/cuentas/101 | jq
curl -sk -u movil-client:movil-secret https://localhost:8443/movil/cuentas/101/movimientos | jq

curl -sk -u movil-client:movil-secret -X POST https://localhost:8443/movil/pagos \
  -H "Content-Type: application/json" -d '{"cuentaId":101,"monto":30,"comercio":"Café"}' | jq

# Sobre el límite del canal (2000): 422 sin llamar a pagos
curl -sk -u movil-client:movil-secret -X POST https://localhost:8443/movil/transferencias \
  -H "Content-Type: application/json" -d '{"cuentaOrigenId":101,"cuentaDestinoId":102,"monto":2500}' | jq
```

**Esperado**: la cuenta solo trae `cuentaId`, `saldo`, `tipo` y `estado`; los movimientos, solo `tipo`, `monto`, `estado` y `fecha`; el pago, solo `idOperacion`, `estado` y `saldo`.

### 6.3 Cajeros (PIN y respuesta mínima)

```zsh
# Sin PIN: 403
curl -sk -u cajero-client:cajero-secret https://localhost:8443/cajero/cuentas/101/saldo | jq

curl -sk -u cajero-client:cajero-secret -H "X-Pin: 1234" https://localhost:8443/cajero/cuentas/101/saldo | jq

curl -sk -i -u cajero-client:cajero-secret -H "X-Pin: 1234" -X PATCH \
  https://localhost:8443/cajero/cuentas/101/retiro -H "Content-Type: application/json" -d '{"monto":20}'

# Sobre el máximo por retiro (500): 422
curl -sk -u cajero-client:cajero-secret -H "X-Pin: 1234" -X PATCH \
  https://localhost:8443/cajero/cuentas/101/retiro -H "Content-Type: application/json" -d '{"monto":800}' | jq
```

**Esperado**: el saldo y el retiro responden solo `cuentaId` y `saldo`; el retiro incluye la cabecera `X-Id-Operacion`. Con credenciales de otro canal (por ejemplo `web-client`), el BFF de cajeros responde 401.

## 7. Resiliencia

### 7.1 Circuit Breaker y fallback en pagos → cuentas

```zsh
docker compose stop cuentas
for i in 1 2 3 4 5; do
  curl -sk -o /dev/null -w "%{http_code} %{time_total}s\n" -X POST https://localhost:8081/api/pagos/depositos \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"cuentaId":101,"monto":10}'
done
docker compose logs --tail=40 pagos
docker compose start cuentas
```

**Esperado**:

- Las primeras solicitudes responden 503 después de los reintentos (unos segundos cada una).
- Al acumular fallas, el log de pagos muestra `circuito abierto` y las siguientes responden 503 de inmediato, sin contactar a cuentas.
- `GET /api/pagos?cuentaId=101` muestra esos depósitos como `FALLIDO`, sin cambio de saldo.
- Tras levantar cuentas y esperar unos 20 s (más el registro en Eureka), los depósitos vuelven a completarse.

### 7.2 Degradación controlada en el BFF web

```zsh
docker compose stop pagos
curl -sk -u web-client:web-secret https://localhost:8443/web/clientes/1/resumen | jq '{cuentas: (.cuentas|length), ultimosPagos, advertencias}'
docker compose start pagos
```

**Esperado**: 200 con las cuentas y el perfil, `ultimosPagos: []` y la advertencia de que no se pudieron obtener los datos de pagos.

### 7.3 Fallback en la apertura de cuentas (cuentas → clientes)

```zsh
docker compose stop clientes
curl -sk -X POST https://localhost:8080/api/cuentas -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"clienteId":1,"tipo":"corriente","saldoInicial":0}' | jq
docker compose start clientes
```

**Esperado**: 503 "No fue posible validar al cliente... No se abrió la cuenta".

### 7.4 Compensación de la saga (de la S8)

```zsh
docker compose stop postgres-pagos
curl -sk -i -X PATCH https://localhost:8080/api/cuentas/101/retiro \
  -H "Authorization: Bearer $CAJERO" -H "Content-Type: application/json" -d '{"monto":10}'
docker compose logs --tail=30 pagos cuentas
docker compose start postgres-pagos
```

**Esperado**: el retiro responde 200, porque el registro es asíncrono. pagos intenta guardarlo 3 veces, publica `movimiento-fallido`, y cuentas devuelve el monto y marca la operación `REVERTIDA`.

## 8. Escalado horizontal

```zsh
docker compose -f docker-compose.yaml -f docker-compose.escalado.yaml up -d \
  --scale cuentas=2 --scale pagos=2 --scale clientes=2
docker compose ps cuentas pagos clientes
```

Hacer varias transferencias a través del gateway y revisar qué réplica atendió cada una:

```zsh
for i in 1 2 3 4 5 6; do
  curl -sk -o /dev/null -w "%{http_code}\n" -u web-client:web-secret -X POST https://localhost:8443/web/transferencias \
    -H "Content-Type: application/json" -d '{"cuentaOrigenId":102,"cuentaDestinoId":101,"monto":1}'
done
docker compose logs pagos | grep COMPLETADO
docker compose logs cuentas | grep APLICADA
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group pagos
```

**Esperado**:

- Dos contenedores por servicio (por ejemplo `...-pagos-1` y `...-pagos-2`) y, en Eureka, dos instancias de CUENTAS, PAGOS y CLIENTES, cada una con su IP.
- Las líneas de `COMPLETADO` (pagos) y `APLICADA` (cuentas) aparecen repartidas entre las dos réplicas: el prefijo de cada línea del log indica el contenedor. El BFF web reparte entre las réplicas de pagos, y pagos entre las de cuentas.
- Las 3 particiones de `retiro-realizado` quedan repartidas entre los dos consumidores del grupo `pagos`.

En este modo, cuentas, pagos y clientes no publican puertos en el equipo: se accede a ellos a través del gateway y los BFF. Para volver al modo normal: `docker compose up -d`.

## 9. Procesos batch

`batch-jobs` se ejecuta al levantar el sistema con el set `semana_3` de [fin_legacy_data](https://github.com/KariVillagran/fin_legacy_data) (1000 registros por archivo) y la fecha de hoy como fecha de proceso.

```zsh
docker compose logs batch-jobs | grep -E "==>|Paso|Integridad|COMPLETADO|reintent"
```

**Esperado**: los tres jobs en `COMPLETED`, cada paso con sus líneas, escritos y rechazados, e `Integridad OK`.

| Job | Escritos | Rechazados |
|---|---|---|
| `transaccionesJob` | 392 (+ 239 días en el resumen) | 608 |
| `interesesMensualesJob` | 50 | 950 |
| `cuentasAnualesJob` | 497 (+ 20 estados de cuenta) | 503 |

Consultar los resultados:

```zsh
docker exec -it postgres-batch psql -U batch_user -d batch_db
```

```sql
SELECT job, codigo, COUNT(*) FROM registros_rechazados GROUP BY job, codigo ORDER BY job, 3 DESC;
SELECT * FROM resumen_transacciones_diarias ORDER BY fecha LIMIT 5;
SELECT tipo, COUNT(*), SUM(interes_generado) FROM intereses_calculados GROUP BY tipo;
SELECT * FROM estados_cuenta_anuales ORDER BY cuenta_id, anio;
SELECT job_execution_id, status, exit_code, start_time FROM batch_job_execution ORDER BY 1;
```

### 9.1 Política de finalización: no reprocesar una fecha ya completada

El mismo día de la ejecución inicial (o indicando esa fecha con `-e BATCH_FECHA_PROCESO=AAAA-MM-DD`):

```zsh
docker compose run --rm batch-jobs
```

**Esperado**: los tres jobs informan "ya estaba COMPLETADO para estos parámetros: no se vuelve a procesar" y el proceso termina con código 0.

### 9.2 Reejecución automática ante un fallo crítico

Con una fecha de proceso nueva y la simulación de fallo activa, el job de intereses falla en el registro 350 y se reanuda solo:

```zsh
docker compose run --rm -e BATCH_FECHA_PROCESO=2026-11-01 -e BATCH_SIMULAR_FALLO=true batch-jobs \
  | grep -E "==>|Paso interes|Fallo crítico|Causa|intento|Reejecución"
```

**Esperado**:

1. Primera ejecución de `interesesMensualesJob`: `FAILED`, con la causa "Fallo crítico simulado en el registro 350" y 300 líneas procesadas (3 bloques confirmados).
2. "Reejecución automática en 5 s".
3. Segunda ejecución de la misma instancia: `COMPLETED`, procesando solo las 700 líneas restantes (desde el registro 301), y el mensaje "COMPLETADO en el intento 2 tras reanudar la ejecución fallida".

En `batch_job_execution` quedan las dos ejecuciones de la misma instancia (una `FAILED` y otra `COMPLETED`). Los cálculos del período 2026-11 suman igual 50 cuentas: nada se perdió ni se duplicó.

### 9.3 Otro set de datos

```zsh
docker compose run --rm -e BATCH_DATOS=classpath:data/semana_1/ -e BATCH_FECHA_PROCESO=2026-12-01 batch-jobs
```

También se puede usar una carpeta del equipo montándola como volumen (`-v $PWD/datos:/datos -e BATCH_DATOS=file:/datos/`), con los tres archivos del legacy con sus nombres originales.

## 10. Ejecución sin Docker (opcional)

Cada servicio se puede ejecutar con `./mvnw -f <servicio>/pom.xml spring-boot:run`. Se necesitan Kafka y las bases de datos de Docker en marcha (`docker compose up -d kafka kafka-init postgres-cuentas postgres-pagos postgres-clientes postgres-batch`). El orden de arranque es: config-server, service-registry, auth-server, microservicios, BFF y gateway. La configuración por defecto (perfil sin `docker`) apunta a `localhost`.

## 11. Detener y limpiar

```zsh
docker compose down        # detiene y elimina los contenedores (los datos se conservan)
docker compose down -v     # además elimina los volúmenes: la próxima vez parte con los datos iniciales
```
