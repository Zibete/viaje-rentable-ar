# 🧱 Arquitectura

Viaje Rentable AR mantiene una arquitectura simple, local y orientada a responsabilidades claras.

El objetivo principal es que la app sea fácil de entender, probar y mejorar sin introducir complejidad innecesaria.

---

## Flujo principal

```text
Captura autorizada
        ↓
Preprocesamiento de imagen
        ↓
OCR con ML Kit
        ↓
Parser defensivo
        ↓
Reglas de zonas / datos incompletos
        ↓
Cálculo de rentabilidad
        ↓
Overlay visual
```

---

## Paquetes principales

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

| Paquete | Responsabilidad |
|---|---|
| `calculator` | Cálculo puro de rentabilidad y decisión final. |
| `capture` | MediaProjection, monitoreo, frame gate y buffer OCR. |
| `config` | Configuración local con DataStore. |
| `debug` | Logging liviano y trazabilidad. |
| `ocr` | OCR, sanitización, parser, presence score y pipeline. |
| `overlay` | Servicio y estado visual del overlay flotante. |
| `ui` | Pantalla principal en Jetpack Compose. |
| `zones` | Reglas por textos o zonas detectadas. |

---

## Decisiones técnicas

### Procesamiento local

La app procesa la información en el dispositivo. No requiere backend ni login para funcionar.

### MediaProjection

Se usa captura autorizada por el usuario para leer información visible en pantalla mediante OCR.

### No AccessibilityService

El proyecto no usa AccessibilityService en esta etapa porque no busca operar sobre otras apps ni automatizar acciones.

### DataStore

La configuración del conductor se guarda localmente mediante DataStore.

### OCR defensivo

El OCR puede devolver texto incompleto o imperfecto. Por eso el parser debe ser tolerante, testeado y conservador.

### Overlay informativo

El overlay solo muestra una recomendación visual. No acepta ni rechaza viajes.

---

## Tests

Las reglas críticas deben mantenerse cubiertas con tests unitarios:

- cálculo de rentabilidad;
- parser OCR;
- frame gate;
- buffer de candidatos;
- presence score;
- pipeline de decisión;
- configuración local;
- estado visual del overlay.

---

## Fuera de alcance arquitectónico

Por ahora no se busca incorporar:

- backend;
- login;
- sincronización remota;
- Room;
- Hilt;
- automatización de clicks;
- APIs privadas de plataformas de viaje.
