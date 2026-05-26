# feat: agregar metrica de cercania al domicilio en overlay

## Que se cambio

- Se agrego la metrica auxiliar `Casa X/10` para indicar si el destino de una solicitud acerca o aleja al conductor de su domicilio configurado.
- Se agrego configuracion local de domicilio por direccion, con coordenadas resueltas localmente y persistidas en DataStore.
- Se agrego geocoding con `android.location.Geocoder`, cache en memoria y manejo controlado de errores/fallos.
- Se agrego proveedor de ubicacion actual con `FusedLocationProviderClient` y permisos de ubicacion aproximada/precisa.
- Se extendio el parser OCR para extraer opcionalmente una linea candidata de destino.
- Se integro el flujo OCR -> destino -> geocoding cacheado -> ubicacion actual -> score -> overlay sin cambiar la decision principal.
- Se actualizo el overlay para mostrar `Casa X/10` o `Casa ?`.
- Se actualizo `README.md` con funcionamiento, privacidad, permisos y limitaciones.

## Por que

La rentabilidad no siempre alcanza para decidir si una solicitud conviene: un viaje puede ser aceptable si acerca al conductor a su zona de regreso, o menos conveniente si lo aleja claramente. `Casa X/10` agrega esa informacion como referencia visual, sin modificar `ACEPTAR`, `RECHAZAR` ni `REVISAR`.

## Como funciona

1. El OCR corre con el flujo existente.
2. El parser intenta extraer `destinationText` de una linea candidata, normalmente despues de `Viaje ...`.
3. La decision de rentabilidad se calcula como antes.
4. Si hay domicilio resuelto, destino OCR y ubicacion actual, se geocodifica el destino con cache.
5. `HomeDirectionCalculator` combina progreso real hacia casa y direccion angular.
6. El overlay muestra `Casa X/10` si el resultado es confiable, o `Casa ?` si falta informacion.

El score usa una escala de 0 a 10:

- `10`: acerca claramente al domicilio.
- `5`: neutral o poco concluyente.
- `0`: aleja claramente del domicilio.

## Permisos y privacidad

- Agrega `ACCESS_COARSE_LOCATION` y `ACCESS_FINE_LOCATION`.
- No agrega `ACCESS_BACKGROUND_LOCATION`.
- No usa AccessibilityService.
- No automatiza clicks.
- No usa APIs privadas de Uber, DiDi, Cabify ni otras plataformas.
- No hardcodea el domicilio.
- No hardcodea API keys.
- No guarda capturas ni bitmaps.
- No envia capturas ni OCR crudo a servicios externos.
- El domicilio y coordenadas quedan en DataStore local.
- La ubicacion se usa para calculo local.
- El geocoding depende del proveedor Android disponible.

## Rama

`feat/home-direction-score`

## Commits

1. `feat(home): add home direction domain calculator`
2. `feat(home): add address based home configuration`
3. `feat(geocoding): resolve addresses and destinations with cache`
4. `feat(location): add driver location provider for home score`
5. `feat(ocr): parse optional destination text from trip offers`
6. `feat(pipeline): attach home direction score to overlay state`
7. `feat(overlay): show home direction score in trip overlay`
8. `docs(readme): document home direction score`
9. `docs(pr): add home direction score pull request draft`

## Archivos modificados

- `app/src/main/java/com/zibete/driverassistant/home/`
- `app/src/main/java/com/zibete/driverassistant/geocoding/`
- `app/src/main/java/com/zibete/driverassistant/location/`
- `app/src/main/java/com/zibete/driverassistant/config/`
- `app/src/main/java/com/zibete/driverassistant/ocr/`
- `app/src/main/java/com/zibete/driverassistant/capture/`
- `app/src/main/java/com/zibete/driverassistant/overlay/`
- `app/src/main/java/com/zibete/driverassistant/ui/`
- `app/src/test/java/com/zibete/driverassistant/`
- `README.md`
- `docs/PR_HOME_DIRECTION_SCORE.md`

## Validacion

Validaciones parciales ejecutadas durante la implementacion:

- `.\gradlew.bat :app:testDebugUnitTest --tests "com.zibete.driverassistant.home.HomeDirectionCalculatorTest"`: OK.
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.zibete.driverassistant.config.*"`: OK.
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.zibete.driverassistant.geocoding.*"`: OK.
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.zibete.driverassistant.location.*" --tests "com.zibete.driverassistant.home.HomeDirectionCalculatorTest"`: OK.
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.zibete.driverassistant.ocr.TripOfferTextParserTest" --tests "com.zibete.driverassistant.ocr.TripOfferDecisionPipelineTest" --tests "com.zibete.driverassistant.capture.TripOfferDetectionStateTest"`: OK.
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.zibete.driverassistant.home.HomeDirectionScoreResolverTest" --tests "com.zibete.driverassistant.capture.HomeDirectionScoreUpdateStateTest" --tests "com.zibete.driverassistant.overlay.OverlayCardStateTest" --tests "com.zibete.driverassistant.ocr.TripOfferDecisionPipelineTest"`: OK.
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.zibete.driverassistant.overlay.OverlayCardStateTest"`: OK.

Validacion final pendiente de ejecutar antes de entregar:

- `.\gradlew.bat :app:testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- `git diff --check`
- `git status --short`

## Como validar manualmente

- Configurar una direccion de domicilio real desde la pantalla de configuracion.
- Otorgar permiso de ubicacion aproximada o precisa.
- Iniciar monitoreo con permiso de captura y overlay.
- Mostrar una solicitud donde el destino sea visible.
- Verificar que el overlay muestre la decision principal inmediatamente.
- Verificar que `Casa ?` se actualice a `Casa X/10` cuando haya destino, geocoding y ubicacion confiables.
- Probar una solicitud sin destino visible y confirmar que se mantenga `Casa ?`.

## Limitaciones

- Depende de que el destino sea visible y el OCR lo lea con suficiente claridad.
- Depende del proveedor Android Geocoder disponible en el dispositivo.
- La confianza de geocoding es aproximada porque Android Geocoder no expone un score real.
- No se pide ubicacion en segundo plano en este PR.
- No se agregan Google Maps, Places, backend, login ni configuracion remota.

## Fuera de alcance

- Cambiar la logica principal de rentabilidad.
- Aceptar o rechazar viajes automaticamente.
- Usar AccessibilityService.
- Usar APIs privadas de plataformas.
- Guardar capturas, bitmaps o screenshots.
- Agregar historial, backend o sincronizacion.

## Validacion manual pendiente

- Probar en dispositivo Android con proveedor Geocoder disponible.
- Validar con solicitudes reales anonimizadas donde el destino sea visible.
- Revisar Logcat con `DriverAssistantDebug` y `traceId` para clasificar falsos positivos de destino.
