# 🤝 Contribuir a Viaje Rentable AR

Gracias por tu interés en colaborar.

Viaje Rentable AR es una app Android gratuita y open source para ayudar a conductores de apps de viaje a evaluar la rentabilidad de una solicitud mediante OCR, cálculo local y una recomendación visual.

El objetivo del proyecto es mantener una herramienta útil, transparente, local y segura.

---

## ✅ Contribuciones bienvenidas

Se aceptan Pull Requests para:

- Mejoras de OCR y parser.
- Casos de test con textos OCR anonimizados.
- Optimización de memoria, CPU o consumo de batería.
- Mejoras de UI/UX.
- Documentación para usuarios o desarrolladores.
- Issues reproducibles con logs y pasos claros.
- Mejoras en configuración local.
- Mejoras en diagnóstico y trazabilidad.

---

## 🚫 Fuera de alcance

No se aceptan cambios que busquen:

- Automatizar clicks.
- Aceptar viajes automáticamente.
- Rechazar viajes automáticamente.
- Operar sobre Uber, DiDi, Cabify u otras apps externas.
- Usar APIs privadas de plataformas de viaje.
- Manipular tarifas, ubicación o comportamiento de apps externas.
- Guardar capturas de pantalla sin consentimiento explícito.
- Subir capturas o datos sensibles a servidores externos.
- Copiar marca, assets, textos propietarios o UI exacta de apps comerciales.

La app debe mantenerse como un asistente visual e informativo.

---

## 🧭 Principios del proyecto

- Procesamiento local por defecto.
- Configuración local como fuente de verdad.
- Cambios chicos, enfocados y reversibles.
- Tests para reglas críticas.
- No agregar dependencias sin justificación clara.
- No mezclar refactors grandes con nuevas features.
- Priorizar estabilidad del dispositivo.
- Priorizar claridad para conductores no técnicos.

---

## 🧪 Antes de abrir un PR

Ejecutá:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

En Windows:

```bash
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Si algún comando falla, indicá el error exacto en el PR.

---

## 🧾 Cómo reportar bugs de OCR

Para reportar un falso positivo o falso negativo, incluí:

- Qué esperabas que ocurra.
- Qué ocurrió realmente.
- Texto OCR observado, si está disponible.
- Logs relevantes con `DriverAssistantDebug` y `traceId`.
- Configuración usada.
- Modelo de dispositivo y versión de Android.

No compartas capturas con datos sensibles sin anonimizar.

---

## 🌱 Flujo sugerido para PRs

1. Abrí un Issue o comentá el problema que querés resolver.
2. Creá una rama con nombre claro.
3. Hacé cambios mínimos y enfocados.
4. Agregá o actualizá tests cuando corresponda.
5. Ejecutá build y tests.
6. Abrí el Pull Request con una descripción clara.

Ejemplos de ramas:

```text
fix/ocr-fare-parser
feat/onboarding-copy
docs/privacy-policy
perf/frame-gate-tuning
```

---

## 📝 Estilo de commits

Usar Conventional Commits:

```text
fix(ocr): parse fare thousands variants
test(ocr): cover fare parsing regression
docs(readme): clarify installation steps
perf(monitor): reduce bitmap allocations
```

---

## 🙌 Gracias

Toda mejora suma: código, documentación, pruebas, ideas, reportes claros o feedback de uso real.
