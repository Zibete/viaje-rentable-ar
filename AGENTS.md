# AGENTS.md

## Proyecto

Viaje Rentable AR es una app Android privada que asiste visualmente a conductores durante solicitudes de viaje.

La app lee información visible con captura autorizada y OCR, calcula métricas locales de rentabilidad y muestra un overlay con una recomendación.

El producto es solo informativo. No debe ejecutar acciones sobre apps externas ni reemplazar la decisión del conductor.

## Estado actual

El repo ya contiene:

- Android nativo en Kotlin;
- UI principal en Jetpack Compose;
- configuración local editable con DataStore;
- captura autorizada con MediaProjection;
- servicios foreground para monitoreo y overlay;
- OCR con ML Kit Text Recognition;
- preprocesamiento previo al OCR;
- `OcrFrameGate` para reducir OCR en frames estables;
- parser OCR para solicitudes de viaje;
- pipeline OCR -> parser -> zonas -> cálculo -> overlay;
- overlay flotante movible;
- estado visual neutro/blanco para datos incompletos;
- `traceId` para diagnóstico en Logcat;
- buffer liviano de candidatos OCR;
- presence score conservador;
- tests unitarios para cálculo, parser, overlay, gate, tracing, buffer y presence score.

No tratar este repo como un MVP inicial desde cero.

## Reglas de producto

- Mantener la app como asistente visual.
- Mantener el cálculo local y rápido.
- Mantener configuración local como fuente de verdad.
- No agregar backend, login, sincronización ni configuración remota sin pedido explícito.
- No agregar dependencias nuevas sin justificación clara.
- No guardar imágenes, bitmaps, screenshots ni archivos de debug salvo pedido explícito.
- No usar AccessibilityService en esta etapa.
- No copiar diseño, marca, textos propietarios, assets ni flujo visual exacto de apps existentes.

## Stack vigente

- Kotlin.
- Jetpack Compose.
- Material 3.
- MVVM simple con ViewModel/Factory manual.
- Coroutines / Flow / StateFlow.
- DataStore.
- ML Kit Text Recognition.
- MediaProjection.
- Foreground Service.
- SYSTEM_ALERT_WINDOW.
- Tests unitarios JVM.

No introducir Hilt, Room, backend ni login salvo pedido explícito.

## Arquitectura actual

Mantener responsabilidades separadas por paquete:

```text
app/src/main/java/com/zibete/driverassistant/
  calculator/
  capture/
  config/
  debug/
  ocr/
  overlay/
  ui/
  zones/
```

Reglas:

- `calculator`: cálculo puro y testeable, sin dependencias Android.
- `capture`: MediaProjection, frame gate, monitoreo, buffer y trazabilidad.
- `config`: configuración local con DataStore.
- `debug`: logging liviano y helpers de diagnóstico.
- `ocr`: OCR, sanitización, parser, presence score y pipeline.
- `overlay`: estado visual y servicio de overlay.
- `ui`: pantalla técnica principal.
- `zones`: reglas de zonas por texto OCR.

## OCR y eficiencia

Priorizar estabilidad del dispositivo.

Reglas:

- no ejecutar OCR extra sin necesidad;
- no guardar imágenes ni bitmaps;
- no procesar múltiples variantes de preprocesamiento en runtime;
- mantener cambios de OCR medibles con tests y logs;
- usar `traceId` en logs relevantes;
- mantener el buffer de candidatos liviano: snapshots de datos, no imágenes;
- candidatos completos pueden mostrarse inmediatamente;
- candidatos incompletos pueden esperar una ventana corta para elegir mejor lectura;
- datos incompletos deben mostrarse con overlay neutro/blanco.

## Overlay

Estados visuales:

- datos completos + `ACCEPT`: verde;
- datos completos + `REJECT`: rojo;
- datos completos + `REVIEW`: amarillo/ámbar;
- datos incompletos: blanco/neutro con título `DATOS INCOMPLETOS`.

El overlay debe ser compacto, legible, movible y no debe bloquear toda la pantalla.

## Parser OCR

El parser debe ser defensivo y testeado con casos reales/probables de OCR.

Reglas:

- no usar montos adicionales como tarifa principal si no existe tarifa base;
- no tomar métricas propias del overlay como tarifa;
- no tomar `$/km` ni `$/h` como tarifa;
- evitar matches parciales de precio;
- soportar errores OCR comunes en `min`, `km`, separadores y caracteres parecidos;
- cuando se ajuste un bug real de OCR, agregar test unitario con el texto observado o una variante anonimizada.

Caso prioritario actual:

- corregir precios con separador de miles, por ejemplo `5.677 ARS` leído o parseado como `677`.

## Flujo de trabajo para agentes

Antes de modificar código:

1. Verificar ruta del repo.
2. Verificar remoto.
3. Verificar rama actual.
4. Actualizar `main` con `origin/main` cuando el usuario indique partir desde main.
5. Verificar wrapper de Gradle.
6. Crear rama nueva si el usuario lo pidió.
7. No crear una rama con el mismo nombre si ya existe.
8. Si hay desalineación, reportar y no avanzar con cambios funcionales hasta resolverla.

## Estilo de cambios

Hacer diffs mínimos y enfocados.

No mezclar en una misma iteración:

- refactor general;
- nueva feature;
- cambio visual grande;
- cambio de arquitectura;
- actualización masiva de dependencias;
- cambio de parser no relacionado.

Cada PR debe tener un objetivo claro y reversible.

## Idioma y nombres

- Código, clases, funciones, variables, paquetes y commits técnicos en inglés.
- UI visible al usuario en español.
- Documentación y PRs en español.
- Incluir traducción al español de los mensajes de commit en el resumen al usuario.

Evitar nombres genéricos salvo que ya existan y no sea parte del cambio renombrarlos.

## Commits

Usar Conventional Commits.

Ejemplos:

```text
fix(ocr): parse fare thousands variants
test(ocr): cover fare thousands parsing
feat(monitor): add lightweight ocr candidate buffer
debug(monitor): add trace id to ocr flow
```

## Validación obligatoria

Antes de entregar una iteración, correr:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Si algún comando falla, reportar el error exacto y no afirmar que la iteración está validada.

## PRs

Los PRs deben estar en español y ser concretos.

Formato recomendado:

```markdown
# título

## Qué se cambió
## Por qué
## Rama
## Commits
## Archivos modificados
## Validación
## Fuera de alcance
## Validación manual pendiente
```

El PR debe mencionar comandos ejecutados, resultado, archivos principales, fuera de alcance y riesgos pendientes.

## Prioridad actual

La prioridad actual es mejorar precisión OCR/parser sin perder eficiencia.

Orden inmediato:

1. Corregir parseo de tarifas con separador de miles.
2. Validar con capturas reales e imágenes generadas para prueba.
3. Usar Logcat con `DriverAssistantDebug` y `traceId` para clasificar fallos.
4. Ajustar parser solo con evidencia.
5. Tocar preprocesamiento únicamente si los logs muestran que crop/scale/contraste causan fallos.
