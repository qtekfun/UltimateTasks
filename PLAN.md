# UltimateTasks — Plan de tareas

Reglas: una tarea cada vez, en su rama `feat/<tarea>`, con `./gradlew check` en verde antes de cerrarla. Marca `[x]` al completar. Cada tarea debe poder verificarse (test o prueba manual descrita).

## Fase 0 — Cimientos y prototipos de riesgo
- [x] **T00 Proyecto base**: copiar de UltimateDeck la estructura Gradle (KTS, `libs.versions.toml`, Hilt, Compose, Material 3), tema (claro/oscuro/AMOLED/Material You), `strings.xml` en/es, cabeceras SPDX, `LICENSE`, `.editorconfig`, `.gitignore`. Paquete `com.qtekfun.ultimatetasks`.
  - *Verificación:* `./gradlew assembleDebug` compila y la app arranca con pantalla vacía.
- [x] **T01 CI y calidad**: detekt, ktlint, Lint (warnings como errores), Kover con umbrales, verificación de dependencias, chequeo de licencias/Play Services, GitHub Actions, Dependabot (copiado y adaptado).
  - *Verificación:* PR de prueba en verde; una dependencia de Play Services añadida a propósito la hace fallar.
- [x] **T02 Prototipo CalDAV + iCalendar**: evaluar `dav4jvm` + `ical4j` frente a implementación propia (tamaño del APK, funcionamiento en API 26, licencias). Corpus `.ics` real (Nextcloud Tasks, Apple, Thunderbird, tasks.org, Deck) con ida y vuelta sin pérdidas. Comprobar contra el servidor: descubrimiento, `sync-collection`, listas de Deck (¿escribibles?), formato `ATTACH` que muestra la web.
  - *Verificación:* tests del corpus; decisiones anotadas en `SPEC.md` §9 (preguntar antes de añadir dependencias).
  - *Resultado:* implementación propia sin dependencias; las comprobaciones contra el servidor pasan a T05.
- [x] **T02b Prototipo de fiabilidad de avisos**: alarma exacta, modo alarma, exención de batería y aviso de prueba en el móvil del autor (ColorOS) con la app cerrada y el móvil en reposo. Elegir `USE_EXACT_ALARM` o `SCHEDULE_EXACT_ALARM`.
  - *Verificación:* informe con retrasos medidos; decisión en `SPEC.md` §9.
  - *Resultado:* se copia la solución de UltimateDeck; medidas y permisos en `SPEC.md` §9.

## Fase 1 — Datos y red
- [x] **T03 Modelo de dominio y Room**: cuenta, lista (color, icono local, orden, visible, sync-token), tarea (campos de RF-05, padre, orden, `.ics` original, ETag, campos sucios), etiqueta, adjunto, aviso pospuesto, cola de operaciones. Migraciones con test.
  - *Resultado:* cuenta, lista, tarea (con el `.ics` del servidor como base de la fusión), cola y DAOs con tests. Adjuntos y avisos pospuestos llegan con sus tareas (T26, T21) mediante migraciones.
- [x] **T04 Mapeo iCalendar ⇄ dominio**: `VTODO` ↔ tarea conservando lo desconocido; `PRIORITY`, `CATEGORIES`, `RELATED-TO`, `VALARM`, `RRULE`, `DUE` con/sin hora y zonas horarias.
  - *Verificación:* corpus de ida y vuelta + tests por propiedad.
- [x] **T05 Cliente CalDAV**: descubrimiento, listar colecciones, `sync-collection`/ctag, GET/PUT/DELETE/MOVE con ETags, MKCALENDAR, PROPPATCH (nombre, color, orden). Tests con MockWebServer (4xx/5xx, timeouts, 412). Fijar versión mínima de Nextcloud.
  - *Pendiente, pasa a T09:* ejecutar `tools/caldav-probe.py` contra el servidor (versión mínima de Nextcloud, listas de Deck, `sync-collection`, ETags, `ATTACH`) y añadir las tareas capturadas al corpus.
- [x] **T06 Login Flow v2**: copiar de UltimateDeck (Keystore, cierre de sesión que limpia datos).

## Fase 2 — Sincronización y lógica
- [x] **T07 Cola de operaciones**: idempotente, backoff, persistente. **100% de cobertura.**
- [x] **T08 Resolutor de conflictos**: reglas de `SPEC.md` §5 con fusión a tres bandas. **100% de cobertura.**
- [x] **T09 Motor de sincronización**: pull incremental, push de la cola, WorkManager periódico, al abrir/volver/tirar para refrescar.
  - *Verificación:* sync real en el móvil contra el servidor del autor (7 listas, 3 de Deck de solo lectura); detalles en `SPEC.md` §9.
- [x] **T10 Motor de recurrencia**: presets, personalizada, siguiente repetición, completar recurrentes según la decisión de §9. **100% de cobertura.**
  - *Resultado:* reglas diarias a anuales con BYDAY, BYMONTHDAY, BYMONTH, BYSETPOS, COUNT y UNTIL; completar avanza la misma tarea; reglas no soportadas se muestran sin poder marcarse.
- [x] **T11 Vistas inteligentes**: consultas Hoy / Programados / Todos / Completados y contadores, respetando listas visibles.
- [ ] **T12 Tests de sync offline**: caídas de red, cambios concurrentes, reintentos, app cerrada a mitad de sync.

## Fase 3 — Interfaz (experiencia Apple)
- [ ] **T13 Login + asistente de fiabilidad** (RF-01), incluyendo aviso de prueba.
- [x] **T14 Home**: búsqueda (sin funcionar aún), 4 botones de colores con contadores, "Mis listas", nueva tarea / añadir lista (RF-02).
  - *Hecho sin la barra de búsqueda (llega con T24) ni "Añadir lista" (T20).*
- [x] **T15 Vista de lista**: checkbox redondo, título, notas, fecha, prioridad, etiquetas, iconos; completar con animación y deshacer; añadir en línea; mostrar completadas (RF-03, RF-04).
  - *Las tareas que se repiten no se pueden marcar hasta T10; desplegar y añadir a listas de solo lectura no aplica.*
- [x] **T16 Vistas inteligentes en la UI**: Hoy (vencidas arriba), Programados (agrupados por día), Todos (agrupados por lista), Completados (por fecha de completado).
- [x] **T17 Detalle de tarea** (RF-05): todos los campos, guardado automático, diálogo de conflicto.
  - *Resultado:* título, notas, URL, fecha y hora, aviso anticipado, prioridad, etiquetas, mover de lista, eliminar; guardado automático; conflictos y tareas borradas en el servidor. Repetir se muestra; su editor es T18.
- [x] **T18 Editor de recurrencia** (RF-06).
  - *Resultado:* presets y personalizado (cada N días/semanas/meses/años, días de la semana, mensual por día o "el 3er viernes"/"el último sábado", fin nunca/tras N/hasta fecha); la fecha salta a la primera repetición.
- [ ] **T19 Subtareas** (RF-07): crear, indentar/desindentar, plegar.
- [x] **T20 Gestión de listas** (RF-08): crear, renombrar, color, icono, reordenar listas y tareas; borrar solo con el ajuste "Permitir borrar listas" activado.
  - *Resultado:* crear, editar (nombre, color, icono local), reordenar listas con botones subir/bajar y borrar (si se permite en Ajustes); "Nueva tarea" y "Añadir lista" en la pantalla de inicio. Reordenar tareas dentro de la lista queda pendiente (orden manual del servidor se respeta).
- [x] **T21 Avisos y posponer** (RF-10): planificador (**100% de cobertura**), notificaciones con acciones, reprogramación en arranque/cambio de hora/actualización, modo alarma, posponer tarea.
  - *Resultado:* planificador puro (100 %), alarmas exactas o modo alarma, Completar/15 min/1 hora/Mañana, reprogramación en arranque, actualización y cambios de hora; Ajustes → Avisos con permisos, batería, modo alarma, hora de todo el día y aviso de prueba.
- [x] **T22 Ajustes** (RF-14): tema, idioma, lista por defecto, permitir borrar listas, listas visibles (RF-09), hora de las tareas de todo el día, modo alarma, asistente, cuenta, versión.
- [ ] **T23 Copia de seguridad cifrada**: copiar de UltimateDeck y adaptar: ajustes + sesión opcional con contraseña, restaurable desde Ajustes y desde el login.
- [ ] **T24 Búsqueda** (RF-12).
- [ ] **T25 Compartir a la app** (RF-13).
- [ ] **T26 Adjuntos** (RF-11): cámara/galería/archivos, cola de subida con reintentos, descarga bajo demanda.
- [ ] **T27 Tablet** (RF-15): diseño adaptativo de 2–3 paneles.

## Fase 4 — Pulido y publicación
- [ ] **T28 Accesibilidad y rendimiento**: TalkBack, 48 dp, fuente al 200%, contraste; medidas de arranque y scroll anotadas en `SPEC.md`.
- [ ] **T29 Tests de UI** en el móvil: login falso, completar con deshacer, crear en línea, Hoy.
- [ ] **T30 Documentación**: `README.md` con capturas, `PRIVACY.md` (cada permiso explicado), `CONTRIBUTING.md`, `CHANGELOG.md`.
- [ ] **T31 Release y F-Droid**: versión en `gradle.properties`, firma por variables de entorno, build reproducible, workflow de release por tag, metadatos fastlane en/es, receta `fdroid/` y `RELEASING.md` (copiado y adaptado de UltimateDeck).
