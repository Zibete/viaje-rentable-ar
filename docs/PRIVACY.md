# 🔐 Privacidad

Viaje Rentable AR está diseñado para funcionar de forma local en el dispositivo del usuario.

El objetivo de esta página es explicar, de forma simple, qué datos procesa la app y qué datos no procesa.

---

## ✅ Principios actuales

- No requiere login.
- No usa backend.
- No sube información a servidores propios.
- No vende datos.
- No guarda capturas de pantalla.
- No guarda imágenes procesadas.
- No comparte información con terceros desde la app.
- La configuración queda almacenada localmente en el dispositivo.

---

## 📲 Permisos utilizados

La app puede solicitar permisos sensibles porque su funcionalidad depende de mostrar información superpuesta y leer texto visible en pantalla con autorización del usuario.

| Permiso | Para qué se usa |
|---|---|
| Captura de pantalla / MediaProjection | Permite analizar información visible en pantalla mediante OCR, siempre con consentimiento explícito del usuario. |
| Mostrar sobre otras apps | Permite mostrar una tarjeta flotante con la recomendación de rentabilidad. |
| Foreground service | Permite mantener el monitoreo activo mientras el usuario usa otras apps. |
| Notificaciones | Puede ser requerido por Android para servicios en primer plano. |

---

## 🔎 OCR y capturas

La app usa OCR para leer texto visible en pantalla.

El procesamiento actual ocurre localmente en el dispositivo.

La app no debería:

- guardar capturas;
- guardar bitmaps;
- subir imágenes;
- almacenar archivos de debug con capturas;
- compartir imágenes con servidores externos.

---

## ⚙️ Configuración local

La configuración del conductor se guarda localmente mediante DataStore.

Puede incluir valores como:

- mínimo `$ / km`;
- mínimo `$ / hora`;
- costo estimado por kilómetro;
- costo estimado por minuto;
- ganancia neta mínima;
- reglas de revisión o rechazo.

Estos valores son usados solo para calcular la recomendación local.

---

## 🚫 Lo que la app no hace

- No acepta viajes automáticamente.
- No rechaza viajes automáticamente.
- No opera sobre botones de apps externas.
- No usa APIs privadas de plataformas de viaje.
- No manipula tarifas.
- No oculta ubicación.
- No reemplaza la decisión del conductor.

---

## ⚠️ Estado alpha

Este proyecto está en etapa alpha experimental.

Antes de una distribución pública masiva o publicación en tiendas, se debe revisar nuevamente:

- comportamiento real de permisos;
- textos de consentimiento;
- disclosure de captura de pantalla;
- cumplimiento de políticas de Android/Google Play;
- documentación para usuarios finales.

---

## 📬 Reportes

Si encontrás un comportamiento que contradiga esta política, abrí un Issue con la mayor cantidad de detalles posibles.
