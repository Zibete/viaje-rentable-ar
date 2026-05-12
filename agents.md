# AGENTS.md

## Proyecto

Este repo contiene una app Android privada para asistir a conductores de plataformas como Uber, DiDi y Cabify a decidir si conviene aceptar, rechazar o revisar un viaje ofrecido.

La app funciona como asistente visual: analiza datos visibles en pantalla, calcula rentabilidad estimada y muestra una recomendación mediante un overlay flotante.

El producto no debe automatizar acciones sobre apps externas. La decisión final siempre pertenece al conductor.

---

## Alineacion del repositorio

Antes de editar codigo, el agente debe verificar ruta, rama, remoto y wrapper de Gradle.

Si la ruta o rama no coincide con la indicada por el usuario, debe corregir el checkout antes de modificar archivos.

No debe crear una rama con el mismo nombre si el usuario indico que ya existe.

Si detecta desalineacion, debe reportarla y no avanzar con cambios funcionales hasta resolverla.

---

## Objetivo del producto

Construir una app Android privada que permita configurar reglas personales de rentabilidad para viajes.

La app debe poder mostrar una decisión clara:

- `ACCEPT`: aceptar.
- `REJECT`: rechazar.
- `REVIEW`: revisar manualmente.

La decisión se calcula a partir de:

- Tarifa ofrecida.
- Distancia hasta el pasajero.
- Tiempo hasta el pasajero.
- Distancia del viaje.
- Tiempo del viaje.
- Rentabilidad por kilómetro.
- Rentabilidad por hora.
- Costo estimado.
- Ganancia neta estimada.
- Zonas configuradas como no deseadas o de revisión.

---

## Principio clean-room

Este proyecto puede inspirarse en la categoría funcional de apps de asistencia para conductores, pero debe implementarse de forma independiente.

No copiar:

- Código de terceros.
- Assets de terceros.
- Logos.
- Nombre de producto.
- Textos propietarios.
- UI exacta.
- Flujo visual idéntico.
- Comportamiento interno no documentado de ninguna app existente.

La app debe tener identidad, lógica, diseño y arquitectura propios.

---

## Restricciones obligatorias

La app NO debe:

- Aceptar viajes automáticamente.
- Rechazar viajes automáticamente.
- Hacer clicks automáticos.
- Usar APIs privadas de Uber, DiDi, Cabify u otras plataformas.
- Manipular tarifas.
- Alterar, bloquear o interferir con apps externas.
- Ocultar ubicación.
- Simular comportamiento humano dentro de otra app.
- Usar AccessibilityService en el MVP.
- Publicarse en Google Play sin revisión previa de permisos, disclosures y cumplimiento.

La app SÍ puede:

- Solicitar permiso de overlay.
- Solicitar permiso de captura de pantalla con consentimiento del usuario.
- Leer texto visible mediante OCR.
- Calcular rentabilidad.
- Mostrar una recomendación visual.
- Guardar configuración propia local en el dispositivo.
- Guardar historial propio de análisis.
- Usar configuración remota propia en una etapa futura, siempre como capa opcional y no como dependencia crítica.

---

## Decisión arquitectónica actual: configuración local primero

La fuente de verdad de configuración para el MVP debe estar en el dispositivo.

Usar:

- `DataStore` para configuración local del conductor.
- `Room` más adelante para historial de viajes analizados.
- `Firebase Realtime Database` solo en una etapa futura como override remoto, defaults remotos, backup, feature flags o sincronización.

No usar Firebase como fuente principal en el MVP.

Motivo:

- La app se usará manejando y debe funcionar con mala señal o sin internet.
- Las reglas de rentabilidad son personales del conductor.
- La decisión debe ser rápida, local y confiable.
- Firebase agregaría dependencia externa prematura para un producto privado de uso personal.

Regla obligatoria:

```text
DataStore = source of truth local.
Firebase = capa opcional futura.
```

La app nunca debe romperse ni bloquear el cálculo por no tener internet.

---

## Stack técnico

Usar Android nativo.

Stack recomendado para el MVP:

- Kotlin.
- Jetpack Compose.
- Material 3.
- MVVM.
- Hilt para dependency injection cuando el proyecto ya esté preparado para DI.
- Kotlin Coroutines.
- Flow / StateFlow.
- DataStore para configuración local persistente.
- ML Kit Text Recognition para OCR, cuando se implemente captura real.
- MediaProjection para captura de pantalla autorizada.
- Foreground Service para mantener análisis activo.
- SYSTEM_ALERT_WINDOW para overlay flotante.

Stack reservado para etapas posteriores:

- Firebase Realtime Database para defaults remotos, backup o feature flags.
- Room para historial persistente de viajes.

---

## Idioma y nombres

Código:

- Clases, funciones, variables, paquetes y commits técnicos en inglés.
- UI visible al usuario en español.
- Documentación del repo en español.
- PRs en español.

Ejemplos válidos:

- `DriverProfitCalculator`
- `TripOfferInput`
- `TripDecisionResult`
- `AvoidZoneRule`
- `TripOfferTextParser`
- `DriverDecisionOverlayService`
- `DataStoreDriverConfigRepository`

Evitar nombres genéricos como:

- `Manager`
- `Helper`
- `Utils`
- `Data`
- `Thing`

---

## Arquitectura esperada

Separar responsabilidades por dominio.

Estructura base sugerida:

```text
app/src/main/java/.../driverassistant/
  calculator/
  capture/
  config/
  ocr/
  overlay/
  permissions/
  zones/
  ui/
```

### calculator

Responsable de calcular rentabilidad y decisión.

Archivos esperados:

```text
calculator/
  DriverProfitCalculator.kt
  DriverDecision.kt
  TripOfferInput.kt
  TripDecisionResult.kt
```

### config

Responsable de obtener y persistir configuración.

Para el MVP, la configuración debe vivir en el dispositivo mediante DataStore.

Archivos esperados:

```text
config/
  DriverConfig.kt
  DriverConfigRepository.kt
  DataStoreDriverConfigRepository.kt
  DriverConfigDataStoreSerializer.kt si se usa Proto DataStore
```

Firebase no debe implementarse en el MVP salvo que se pida explícitamente en una rama posterior.

Si en el futuro se agrega Firebase, debe entrar como implementación secundaria, sin desplazar a DataStore como fuente local principal.

### ocr

Responsable de transformar texto crudo en candidatos de viaje.

Archivos esperados:

```text
ocr/
  TripOfferTextParser.kt
  TripOfferCandidate.kt
  ParsedTripField.kt
```

### zones

Responsable de reglas de zonas a evitar o revisar.

Archivos esperados:

```text
zones/
  AvoidZoneRule.kt
  AvoidZonePolicy.kt
  AvoidZoneMatcher.kt
  ZoneMatchResult.kt
```

### overlay

Responsable de mostrar la card flotante.

Archivos esperados:

```text
overlay/
  DriverDecisionOverlayService.kt
  OverlayPermissionHelper.kt
  OverlayCardState.kt
```

### capture

Responsable de captura de pantalla autorizada.

Archivos esperados:

```text
capture/
  ScreenCaptureManager.kt
  MediaProjectionController.kt
```

---

## Modelo de configuración

Crear `DriverConfig` con estos campos mínimos:

```kotlin
data class DriverConfig(
    val minArsPerKm: Double,
    val minArsPerHour: Double,
    val minNetProfit: Double,
    val costPerKm: Double,
    val costPerMinute: Double,
    val maxPickupKm: Double,
    val maxPickupMinutes: Double,
    val reviewTolerancePercent: Double,
    val rejectIfUnknownFare: Boolean,
    val rejectIfUnknownDistance: Boolean,
    val rejectIfAvoidZoneDetected: Boolean,
    val enabledPlatforms: Map<String, Boolean>,
    val avoidZones: List<AvoidZoneRule>
)
```

Config inicial local sugerida:

```json
{
  "minArsPerKm": 600,
  "minArsPerHour": 7000,
  "minNetProfit": 1000,
  "costPerKm": 280,
  "costPerMinute": 30,
  "maxPickupKm": 3,
  "maxPickupMinutes": 10,
  "reviewTolerancePercent": 10,
  "rejectIfUnknownFare": true,
  "rejectIfUnknownDistance": false,
  "rejectIfAvoidZoneDetected": true,
  "enabledPlatforms": {
    "uber": true,
    "didi": true,
    "cabify": true
  },
  "avoidZones": []
}
```

Reglas:

- `DriverConfigRepository` debe exponer configuración como `Flow<DriverConfig>` o suspend functions claras.
- `DataStoreDriverConfigRepository` debe persistir y leer la configuración local.
- Debe existir fallback local seguro si DataStore no tiene valores guardados.
- No ocultar errores críticos de persistencia sin dejar evidencia en logs o estado.

---

## Motor de cálculo

Crear `DriverProfitCalculator` como clase pura, testeable y sin dependencias de Android.

Input:

```kotlin
data class TripOfferInput(
    val fareAmount: Double?,
    val pickupKm: Double?,
    val tripKm: Double?,
    val pickupMinutes: Double?,
    val tripMinutes: Double?,
    val platform: String?,
    val rawText: String? = null
)
```

Output:

```kotlin
data class TripDecisionResult(
    val decision: DriverDecision,
    val fareAmount: Double?,
    val arsPerKm: Double?,
    val arsPerHour: Double?,
    val estimatedCost: Double?,
    val estimatedNetProfit: Double?,
    val totalKm: Double?,
    val totalMinutes: Double?,
    val rejectionReasons: List<String>,
    val reviewReasons: List<String>
)
```

Enum:

```kotlin
enum class DriverDecision {
    ACCEPT,
    REJECT,
    REVIEW
}
```

Fórmulas:

```text
totalKm = pickupKm + tripKm
totalMinutes = pickupMinutes + tripMinutes
arsPerKm = fareAmount / totalKm
arsPerHour = fareAmount / (totalMinutes / 60)
estimatedCost = (totalKm * costPerKm) + (totalMinutes * costPerMinute)
estimatedNetProfit = fareAmount - estimatedCost
```

Reglas:

`ACCEPT` si:

- `arsPerKm >= minArsPerKm`.
- `arsPerHour >= minArsPerHour`.
- `estimatedNetProfit >= minNetProfit`.
- `pickupKm <= maxPickupKm`.
- `pickupMinutes <= maxPickupMinutes`.
- No hay zona bloqueada.

`REJECT` si:

- $/km está por debajo del mínimo fuera de tolerancia.
- $/hora está por debajo del mínimo fuera de tolerancia.
- ganancia neta estimada está por debajo del mínimo.
- distancia al pasajero supera el máximo.
- tiempo al pasajero supera el máximo.
- se detecta zona bloqueada con política `REJECT`.

`REVIEW` si:

- Algún dato importante falta.
- El OCR es ambiguo.
- El resultado está dentro de la tolerancia.
- Se detecta zona con política `REVIEW`.
- La plataforma no se reconoce.

---

## Zonas a evitar

Implementar zonas primero por texto OCR.

Modelo:

```kotlin
data class AvoidZoneRule(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val policy: AvoidZonePolicy,
    val activeFrom: String? = null,
    val activeTo: String? = null,
    val enabled: Boolean = true
)
```

Enum:

```kotlin
enum class AvoidZonePolicy {
    REJECT,
    REVIEW
}
```

Reglas:

- Comparar keywords contra texto OCR normalizado.
- La comparación debe ignorar mayúsculas, minúsculas y acentos.
- Si una zona coincide y está activa, devolver `ZoneMatchResult`.
- Si policy es `REJECT`, la decisión final debe ser `REJECT` salvo que falten datos críticos y corresponda `REVIEW` por ambigüedad.
- Si policy es `REVIEW`, la decisión final debe ser como mínimo `REVIEW`.

No implementar mapa ni geocoding en el MVP.

Dejar la arquitectura preparada para una futura versión con:

- Coordenadas.
- Radios.
- Polígonos.
- Geocoding.

---

## Parser OCR inicial

Crear `TripOfferTextParser` como clase pura y testeable.

Input:

```kotlin
fun parse(rawText: String): TripOfferCandidate?
```

Output:

```kotlin
data class TripOfferCandidate(
    val fareAmount: Double?,
    val pickupKm: Double?,
    val tripKm: Double?,
    val pickupMinutes: Double?,
    val tripMinutes: Double?,
    val platform: String?,
    val rawText: String,
    val confidence: Double
)
```

Patrones iniciales:

Montos:

- `ARS 5.127`
- `$ 5.127`
- `$5127`
- `5127 pesos`

Distancias:

- `8.0 km`
- `8,0 km`
- `3 km`
- `41 min (8.0 km) total`

Tiempos:

- `41 min`
- `12 minutos`
- `1-2 min`

El parser inicial puede usar heurísticas simples, pero debe tener tests.

Cuando haya capturas reales, ajustar regex y agregar casos reales anonimizados como fixtures de test.

---

## Overlay

El overlay debe ser compacto, legible y no debe bloquear toda la pantalla.

Estados visuales:

- `ACCEPT`: verde.
- `REJECT`: rojo.
- `REVIEW`: amarillo/ámbar.

Texto visible:

- Decisión.
- Tarifa.
- $/h.
- $/km.
- Tiempo total.
- Km total.
- Motivo corto cuando sea rechazo o revisión.

Ejemplo de contenido:

```text
ACEPTAR
$ 5.127
$ 7.503/h · $ 641/km
41 min · 8.0 km
```

No copiar el diseño exacto de ninguna app existente.

---

## Pantalla técnica mínima

El MVP no necesita UI final pulida.

Crear una pantalla inicial con:

- Estado permiso overlay.
- Estado permiso captura.
- Botón para pedir permiso overlay.
- Botón para pedir permiso captura.
- Botón iniciar servicio.
- Botón detener servicio.
- Botón probar overlay con datos simulados.
- Última decisión calculada.
- Configuración local actual.

La edición completa de configuración puede entrar en una rama posterior.

---

## Permisos

Declarar solo permisos necesarios.

Permisos esperados:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

MediaProjection siempre debe iniciarse con consentimiento explícito del usuario.

---

## Firebase

No implementar Firebase en el MVP salvo pedido explícito.

Firebase queda documentado como opción futura para:

- Defaults remotos.
- Feature flags.
- Backup de configuración.
- Sincronización entre dispositivos.
- Kill switch si algún día el producto deja de ser solo personal.

Si se implementa en el futuro:

- No debe reemplazar a DataStore como fuente local principal.
- Debe funcionar como override opcional.
- Debe tener fallback local inmediato.
- La app debe seguir calculando aunque Firebase no responda.
- No requiere login en el MVP ni en la primera integración remota.

Ruta futura sugerida:

```text
/driverAssistantConfig
```

---

## Tests obligatorios

Agregar tests unitarios para `DriverProfitCalculator`:

- Acepta viaje rentable.
- Rechaza viaje por $/km bajo.
- Rechaza viaje por $/hora bajo.
- Rechaza viaje por ganancia neta baja.
- Rechaza viaje por pickupKm mayor al máximo.
- Rechaza viaje por pickupMinutes mayor al máximo.
- Marca `REVIEW` cuando está dentro de tolerancia.
- Calcula correctamente km total.
- Calcula correctamente minutos totales.
- Calcula correctamente $/km.
- Calcula correctamente $/hora.
- Calcula correctamente costo estimado.
- Calcula correctamente ganancia neta.

Agregar tests unitarios para `TripOfferTextParser`:

- `ARS 5.127 41 min 8.0 km`
- `$ 5127 41 min 8 km`
- `5.127 pesos 41 minutos 8,0 km`
- `Aceptar $ 5.127 7.503/h 641/km 41 min 8.0 km total`
- `1-2 min $ 5.127 8 km`

Agregar tests unitarios para `AvoidZoneMatcher`:

- Detecta keyword exacta.
- Detecta keyword ignorando mayúsculas.
- Detecta keyword ignorando acentos.
- No detecta zona deshabilitada.
- Devuelve policy `REJECT`.
- Devuelve policy `REVIEW`.

Cuando se implemente DataStore:

- Testear serialización/defaults si se usa Proto DataStore.
- Testear que repository devuelve defaults seguros si no hay configuración guardada.
- Testear actualización de campos críticos de configuración.

---

## Manejo de errores

No ocultar errores críticos con defaults silenciosos.

Preferir estados explícitos:

- Datos completos.
- Datos incompletos.
- OCR ambiguo.
- Config local cargada.
- Config local no disponible.
- Permiso faltante.
- Servicio detenido.

Si falta información crítica del viaje, devolver `REVIEW` salvo que la configuración indique rechazo explícito.

---

## Estilo de cambios

Hacer diffs mínimos y enfocados.

No mezclar en un mismo cambio:

- Refactor general.
- Nueva feature.
- Cambios visuales grandes.
- Cambios de arquitectura.
- Actualización masiva de dependencias.

Cada PR debe tener un objetivo claro.

---

## Commits

Usar Conventional Commits.

Ejemplos:

```text
chore(project): create android app baseline
feat(calculator): add driver profit decision engine
feat(ocr): add initial trip offer text parser
feat(zones): add avoid zone matcher
feat(overlay): add decision overlay service
feat(config): add datastore driver config repository
test(calculator): cover trip decision rules
test(ocr): cover initial trip text parsing
```

Cuando se entregue el resumen al usuario, incluir traducción al español de los mensajes de commit.

---

## PRs

Los PRs deben escribirse en español.

Formato esperado:

```markdown
# Título

## Qué se cambió

## Por qué

## Cómo validar

## Tests

## Archivos principales

## Limitaciones / fuera de alcance

## Commits sugeridos
```

El PR debe mencionar explícitamente:

- Qué compila.
- Qué tests se corrieron.
- Qué queda pendiente.
- Qué no se implementó por alcance.

---

## Roadmap inicial

### PR 1 — Base técnica Android

Objetivo:

- Crear proyecto Android.
- Crear motor de cálculo.
- Crear parser OCR inicial con texto simulado.
- Crear matcher de zonas por texto.
- Crear overlay service simulado o placeholder seguro.
- Crear pantalla técnica mínima.
- Agregar tests unitarios.

No incluir:

- Captura real.
- ML Kit real.
- Firebase.
- Historial.
- Mapa.
- Geocoding.
- UI final.

### PR 2 — Configuración local persistente

Objetivo:

- Implementar DataStore como fuente local de `DriverConfig`.
- Persistir configuración del conductor.
- Exponer configuración desde `DriverConfigRepository`.
- Agregar defaults seguros.
- Preparar UI mínima o acciones internas para actualizar valores críticos.

No incluir:

- Firebase.
- Login.
- Backend admin.

### PR 3 — Overlay real

Objetivo:

- Convertir el placeholder en overlay funcional.
- Mostrar card flotante con decisión y métricas.
- Permitir prueba manual con datos simulados.
- Respetar permisos y foreground service.

### PR 4 — Captura real + OCR

Objetivo:

- Integrar MediaProjection.
- Capturar pantalla con consentimiento.
- Integrar ML Kit Text Recognition.
- Procesar texto real.
- Conectar OCR con parser y motor.

### PR 5 — Ajuste con capturas reales

Objetivo:

- Usar capturas reales anonimizadas.
- Ajustar regex.
- Mejorar detección de monto, km, minutos y zona.
- Agregar fixtures de test.

### PR 6 — Historial

Objetivo:

- Guardar viajes analizados.
- Mostrar totales por día.
- Exportar CSV.
- Calcular métricas acumuladas.

### PR 7 — UX privada

Objetivo:

- Mejorar pantalla principal.
- Agregar configuración local editable completa.
- Mejorar overlay.
- Agregar botón flotante de análisis manual.

### PR futura — Firebase opcional

Objetivo:

- Agregar defaults remotos u override remoto.
- Mantener DataStore como fuente local principal.
- Sincronizar solo si aporta valor real.
- No bloquear funcionamiento offline.

---

## Criterio de done para MVP 1

La tarea está terminada cuando:

- La app compila.
- Existe pantalla técnica mínima.
- Se puede probar cálculo con datos simulados.
- Existe motor de cálculo testeado.
- Existe parser OCR inicial testeado.
- Existe matcher de zonas testeado.
- Existe configuración local default.
- No se automatizan clicks.
- No se usan APIs privadas de plataformas.
- El README o PR indica limitaciones pendientes.

Firebase no forma parte del done del MVP 1.

---

## Preguntas de producto pendientes

Antes de avanzar a una UI final, definir:

- Nombre definitivo de la app.
- Valores iniciales de $/km y $/hora.
- Costo real estimado por km.
- Costo real estimado por minuto.
- Distancia máxima aceptable hasta pasajero.
- Tiempo máximo aceptable hasta pasajero.
- Lista inicial de zonas a evitar.
- Diferencia entre zona de rechazo y zona de revisión.
- Si el análisis será manual mediante botón o automático por intervalo.

---

## Prioridad actual

La prioridad actual es crear una base sólida, offline-first y testeable.

Orden recomendado:

1. `AGENTS.md`.
2. Proyecto Android base.
3. Motor de cálculo.
4. Matcher de zonas.
5. Parser OCR inicial.
6. Overlay simulado.
7. Config local default.
8. Pantalla técnica mínima.
9. Tests.
10. DataStore para persistir configuración.

No avanzar a Firebase hasta que la app funcione localmente con cálculo, parser simulado, zonas, overlay y configuración persistente.
