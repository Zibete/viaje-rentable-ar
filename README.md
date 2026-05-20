[![Licencia](https://img.shields.io/github/license/Zibete/viaje-rentable-ar?v=1)](LICENSE)
[![Último commit](https://img.shields.io/github/last-commit/Zibete/viaje-rentable-ar)](https://github.com/Zibete/viaje-rentable-ar/commits/main)
[![Issues](https://img.shields.io/github/issues/Zibete/viaje-rentable-ar)](https://github.com/Zibete/viaje-rentable-ar/issues)
[![PRs welcome](https://img.shields.io/badge/PRs-welcome-2ea44f)](CONTRIBUTING.md)

![Kotlin](https://img.shields.io/badge/Kotlin-%E2%9C%94-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-%E2%9C%94-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-%E2%9C%94-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-%E2%9C%94-757575?logo=materialdesign&logoColor=white)
![ML Kit](https://img.shields.io/badge/ML%20Kit-OCR-4285F4?logo=google&logoColor=white)
![DataStore](https://img.shields.io/badge/DataStore-Local%20config-FF6F00)

# 🚗 Viaje Rentable AR — Asistente gratuito para conductores de apps de viaje

---

> **Viaje Rentable AR** es una app Android gratuita y open source que ayuda a conductores de apps de viaje en Argentina a evaluar rápidamente si una solicitud conviene o no, usando **OCR**, cálculo local de rentabilidad y una recomendación visual superpuesta.

---

## ⚠️ Estado del proyecto

Este proyecto se encuentra en etapa **alpha experimental**.

La app ya cuenta con una base funcional, pero todavía requiere validación real prolongada, mejoras de documentación, ajustes de precisión OCR y pruebas en distintos dispositivos Android.

---

## 🎯 Objetivo

Ayudar al conductor a tomar una decisión más informada durante una solicitud de viaje.

La app analiza datos visibles en pantalla, como:

- tarifa ofrecida;
- distancia hasta el pasajero;
- distancia del viaje;
- minutos hasta el pasajero;
- minutos del viaje;
- posibles zonas o textos relevantes.

Con esos datos calcula métricas locales como:

- `$ / km`;
- `$ / hora`;
- costo estimado;
- ganancia neta estimada.

Luego muestra una recomendación visual:

| Decisión | Significado |
|---|---|
| 🟢 **ACEPTAR** | El viaje parece rentable según la configuración actual. |
| 🟡 **REVISAR** | Hay datos grises o condiciones que conviene mirar manualmente. |
| 🔴 **RECHAZAR** | El viaje parece poco conveniente según las reglas configuradas. |
| ⚪ **DATOS INCOMPLETOS** | El OCR no pudo leer toda la información necesaria. |

---

## ✅ Qué hace

- Lee información visible en pantalla con permiso explícito del usuario.
- Usa OCR local mediante ML Kit Text Recognition.
- Calcula rentabilidad estimada del viaje.
- Muestra una tarjeta flotante con la recomendación.
- Permite configurar costos y mínimos de rentabilidad.
- Procesa los datos localmente en el dispositivo.
- Ayuda a comparar viajes con criterios más consistentes.

---

## 🚫 Qué NO hace

- No acepta viajes automáticamente.
- No rechaza viajes automáticamente.
- No toca botones de Uber, DiDi, Cabify ni ninguna otra app.
- No usa APIs privadas de plataformas de viaje.
- No modifica tarifas.
- No oculta ubicación.
- No manipula otras aplicaciones.
- No guarda capturas de pantalla.
- No vende datos.
- No reemplaza la decisión final del conductor.

La app es solo un **asistente visual e informativo**.

---

## 🔗 Accesos rápidos

| Recurso | Enlace |
|---|---|
| 🤝 Cómo contribuir | [CONTRIBUTING.md](CONTRIBUTING.md) |
| 🔐 Privacidad | [docs/PRIVACY.md](docs/PRIVACY.md) |
| 🧱 Arquitectura | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| 🗺️ Roadmap | [docs/ROADMAP.md](docs/ROADMAP.md) |
| ⚖️ Licencia | [LICENSE](LICENSE) |
| 🐞 Reportar un problema | [Issues](https://github.com/Zibete/viaje-rentable-ar/issues) |

---

## 🧠 Qué demuestra

Este proyecto también funciona como caso de portfolio técnico Android.

Demuestra:

- desarrollo Android nativo con Kotlin;
- UI moderna con Jetpack Compose y Material 3;
- captura autorizada de pantalla con MediaProjection;
- OCR con ML Kit;
- procesamiento eficiente de frames;
- parser defensivo para textos imperfectos de OCR;
- cálculo de rentabilidad aislado y testeable;
- overlay flotante con foreground service;
- configuración local con DataStore;
- arquitectura simple por responsabilidades;
- tests unitarios para reglas críticas;
- uso de IA y agentes de código como apoyo al desarrollo, validación y documentación.

---

## ✨ Features principales

- 📲 **Monitoreo de pantalla autorizado**
  - Usa MediaProjection con consentimiento explícito del usuario.

- 🔎 **OCR local**
  - Extrae texto visible de solicitudes de viaje.

- 🧮 **Motor de rentabilidad**
  - Calcula `$ / km`, `$ / hora`, costo estimado y ganancia neta.

- 🧠 **Reglas de decisión**
  - Recomienda aceptar, rechazar o revisar.

- 🪟 **Overlay flotante**
  - Muestra la recomendación sobre otras apps sin bloquear toda la pantalla.

- ⚙️ **Configuración editable**
  - Permite ajustar mínimos y costos según el criterio del conductor.

- 🧪 **Diagnóstico**
  - Incluye herramientas internas para probar OCR, overlay y última decisión.

- 🧱 **Base testeable**
  - Tests unitarios para parser, cálculo, frame gate, buffer y pipeline.

---

## 🛠️ Stack técnico

### 📱 Android & UI

![Kotlin](https://img.shields.io/badge/Kotlin-Coroutines%20%2B%20Flow-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-Design%20System-757575?logo=materialdesign&logoColor=white)

### 🧠 OCR & Procesamiento

![ML Kit](https://img.shields.io/badge/ML%20Kit-Text%20Recognition-4285F4?logo=google&logoColor=white)
![MediaProjection](https://img.shields.io/badge/MediaProjection-Screen%20Capture-3DDC84)
![Foreground Service](https://img.shields.io/badge/Foreground%20Service-Monitoring-FF9800)

### 🧱 Arquitectura & Persistencia

![MVVM](https://img.shields.io/badge/MVVM-StateFlow-4CAF50)
![DataStore](https://img.shields.io/badge/DataStore-Preferences-FF6F00)
![JUnit](https://img.shields.io/badge/JUnit-Unit%20Tests-25A162)

---

## 🧩 Cómo funciona

```text
Captura autorizada
        ↓
Preprocesamiento de imagen
        ↓
OCR con ML Kit
        ↓
Parser defensivo de solicitud
        ↓
Reglas de zonas / datos incompletos
        ↓
Cálculo de rentabilidad
        ↓
Overlay visual
```

El procesamiento se realiza localmente y busca minimizar consumo de CPU/memoria mediante:

- reducción de tamaño de captura;
- `OcrFrameGate` para evitar OCR en frames estables;
- buffer liviano de candidatos OCR;
- preprocesamiento controlado;
- reciclado de bitmaps procesados;
- logs de diagnóstico con `traceId`.

---

## 📐 Configuración actual

La app permite configurar criterios locales como:

| Parámetro | Descripción |
|---|---|
| Mínimo `$ / km` | Rentabilidad mínima esperada por kilómetro. |
| Mínimo `$ / hora` | Rentabilidad mínima esperada por tiempo. |
| Ganancia neta mínima | Piso estimado luego de costos. |
| Costo por km | Costo operativo estimado por distancia. |
| Costo por minuto | Costo operativo estimado por tiempo. |
| Tolerancia de revisión | Margen para enviar casos grises a `REVISAR`. |
| Rechazo por tarifa no detectada | Define si una solicitud sin tarifa debe rechazarse. |
| Rechazo por distancia no detectada | Define si una solicitud sin distancia debe rechazarse. |
| Zonas a evitar | Textos o zonas que pueden activar revisión/rechazo. |

---

## 🚀 Cómo compilar

```bash
# Clonar el repo
git clone https://github.com/Zibete/viaje-rentable-ar.git

# Entrar al proyecto
cd viaje-rentable-ar

# Build de debug
./gradlew :app:assembleDebug

# Tests unitarios
./gradlew :app:testDebugUnitTest
```

En Windows:

```bash
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

---

## 📲 Instalación

Por ahora el proyecto está orientado a build local desde Android Studio o Gradle.

En próximas versiones se podrá publicar una APK desde la sección de Releases.

> ⚠️ La app no está publicada en Google Play. Antes de cualquier publicación formal se deben revisar permisos, disclosures, privacidad y cumplimiento de políticas.

---

## 🗂️ Estructura del proyecto

```text
📦 app/
├─ 📂 src/
│  ├─ 📂 main/
│  │  ├─ 📂 java/com/zibete/driverassistant/
│  │  │  ├─ 🧮 calculator/  ← cálculo puro de rentabilidad
│  │  │  ├─ 📸 capture/     ← MediaProjection, monitoreo, frame gate y buffer
│  │  │  ├─ ⚙️ config/      ← configuración local con DataStore
│  │  │  ├─ 🪵 debug/       ← logs y trazabilidad
│  │  │  ├─ 🔎 ocr/         ← OCR, parser, sanitización y pipeline
│  │  │  ├─ 🪟 overlay/     ← servicio y estado del overlay flotante
│  │  │  ├─ 🎨 ui/          ← pantalla principal en Compose
│  │  │  └─ 🗺️ zones/       ← reglas por zonas/textos detectados
│  │  └─ 📂 res/            ← recursos Android
│  └─ 🧪 test/              ← tests unitarios JVM
└─ ⚙️ build.gradle.kts
```

---

## 🧪 Tests

El proyecto incluye tests unitarios para reglas críticas:

- cálculo de rentabilidad;
- parser OCR;
- detección de presencia de oferta;
- frame gate;
- buffer de candidatos OCR;
- pipeline de decisión;
- configuración local;
- estado visual del overlay.

Comando principal:

```bash
./gradlew :app:testDebugUnitTest
```

---

## 🗺️ Roadmap

### Corto plazo

- Mejorar documentación para usuarios y desarrolladores.
- Agregar capturas reales de la app.
- Crear guía de instalación.
- Agregar templates de Issues y Pull Requests.
- Validar consumo de memoria/CPU en uso prolongado.
- Ajustar OCR con casos reales anonimizados.
- Consolidar modo diagnóstico.

### Mediano plazo

- Historial local de viajes analizados.
- Estadísticas por día, semana o mes.
- Exportación CSV.
- Mejor onboarding para conductores.
- Mejor explicación de cada decisión.
- Configuraciones sugeridas por tipo de uso.

### Fuera de alcance

- Automatizar aceptación o rechazo de viajes.
- Usar AccessibilityService para operar apps externas.
- Usar APIs privadas.
- Crear backend obligatorio.
- Guardar capturas sin consentimiento.
- Copiar diseño, marca o textos propietarios de apps comerciales.

---

## 🤝 Contribuir

Este proyecto está abierto a colaboración.

Se aceptan PRs para:

- mejorar OCR/parser;
- agregar tests con textos anonimizados;
- optimizar memoria o CPU;
- mejorar UI/UX;
- mejorar documentación;
- reportar falsos positivos o falsos negativos;
- proponer configuraciones más claras para conductores.

No se aceptan PRs que busquen:

- automatizar clicks;
- aceptar o rechazar viajes automáticamente;
- manipular otras apps;
- usar APIs privadas;
- guardar capturas sin consentimiento;
- copiar assets, marca o UI exacta de terceros.

➡️ Ver: [CONTRIBUTING.md](CONTRIBUTING.md)

---

## 🔐 Privacidad

La app está pensada para funcionar de forma local.

Principios actuales:

- no requiere login;
- no usa backend;
- no sube capturas a servidores;
- no guarda imágenes;
- no vende datos;
- la configuración queda en el dispositivo.

➡️ Ver: [docs/PRIVACY.md](docs/PRIVACY.md)

---

## ⚖️ Licencia

MIT — ver [LICENSE](LICENSE)

---

## 👤 Autor

**Matías Abel Peralta**

[![GitHub](https://img.shields.io/badge/GitHub-Zibete-181717?logo=github&logoColor=white)](https://github.com/Zibete)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Matías%20Abel%20Peralta-0077B5?logo=linkedin&logoColor=white)](https://www.linkedin.com/in/mat%C3%ADasabelperalta/)

---

## 🙌 Nota final

Viaje Rentable AR nace como una herramienta comunitaria, gratuita y transparente para conductores.

La idea no es reemplazar la experiencia del conductor, sino aportar una referencia rápida y configurable para decidir mejor.
