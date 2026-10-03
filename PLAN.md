# UltimateTasks — Plan de tareas

Reglas: una tarea cada vez, en su rama `feat/<tarea>`, con `./gradlew check` en verde antes de cerrarla. Marca `[x]` al completar. Cada tarea debe poder verificarse (test o prueba manual descrita).

## Fase 0 — Cimientos y prototipos de riesgo
- [ ] **T00 Proyecto base**: copiar de UltimateDeck la estructura Gradle (KTS, `libs.versions.toml`, Hilt, Compose, Material 3), tema (claro/oscuro/AMOLED/Material You), `strings.xml` en/es, cabeceras SPDX, `LICENSE`, `.editorconfig`, `.gitignore`. Paquete `com.qtekfun.ultimatetasks`.
  - *Verificación:* `./gradlew assembleDebug` compila y la app arranca con pantalla vacía.
- [ ] **T01 CI y calidad**: detekt, ktlint, Lint (warnings como errores), Kover con umbrales, verificación de dependencias, chequeo de licencias/Play Services, GitHub Actions, Dependabot (copiado y adaptado).
  - *Verificación:* PR de prueba en verde; una dependencia de Play Services añadida a propósito la hace fallar.
- [ ] **T02 Prototipo CalDAV + iCalendar**: evaluar `dav4jvm` + `ical4j` frente a implementación propia (tamaño del APK, funcionamiento en API 26, licencias). Corpus `.ics` real (Nextcloud Tasks, Apple, Thunderbird, tasks.org, Deck) con ida y vuelta sin pérdidas. Comprobar contra el servidor: descubrimiento, `sync-collection`, listas de Deck (¿escribibles?), formato `ATTACH` que muestra la web.
  - *Verificación:* tests del corpus; decisiones anotadas en `SPEC.md` §9 (preguntar antes de añadir dependencias).
- [ ] **T02b Prototipo de fiabilidad de avisos**: alarma exacta, modo alarma, exención de batería y aviso de prueba en el móvil del autor (ColorOS) con la app cerrada y el móvil en reposo. Elegir `USE_EXACT_ALARM` o `SCHEDULE_EXACT_ALARM`.
  - *Verificación:* informe con retrasos medidos; decisión en `SPEC.md` §9.

## Fase 1 — Datos y red
- [ ] **T03 Modelo de dominio y Room**: cuenta, lista (color, icono local, orden, visible, sync-token), tarea (campos de RF-05, padre, orden, `.ics` original, ETag, campos sucios), etiqueta, adjunto, aviso pospuesto, cola de operaciones. Migraciones con test.
- [ ] **T04 Mapeo iCalendar ⇄ dominio**: `VTODO` ↔ tarea conservando lo desconocido; `PRIORITY`, `CATEGORIES`, `RELATED-TO`, `VALARM`, `RRULE`, `DUE` con/sin hora y zonas horarias.
  - *Verificación:* corpus de ida y vuelta + tests por propiedad.
- [ ] **T05 Cliente CalDAV**: descubrimiento, listar colecciones, `sync-collection`/ctag, GET/PUT/DELETE/MOVE con ETags, MKCALENDAR, PROPPATCH (nombre, color, orden). Tests con MockWebServer (4xx/5xx, timeouts, 412). Fijar versión mínima de Nextcloud.
- [ ] **T06 Login Flow v2**: copiar de UltimateDeck (Keystore, cierre de sesión que limpia datos).

## Fase 2 — Sincronización y lógica
- [ ] **T07 Cola de operaciones**: idempotente, backoff, persistente. **100% de cobertura.**
- [ ] **T08 Resolutor de conflictos**: reglas de `SPEC.md` §5 con fusión a tres bandas. **100% de cobertura.**
- [ ] **T09 Motor de sincronización**: pull incremental, push de la cola, WorkManager periódico, al abrir/volver/tirar para refrescar.
- [ ] **T10 Motor de recurrencia**: presets, personalizada, siguiente repetición, completar recurrentes según la decisión de §9. **100% de cobertura.**
- [ ] **T11 Vistas inteligentes**: consultas Hoy / Programados / Todos / Completados y contadores, respetando listas visibles.
- [ ] **T12 Tests de sync offline**: caídas de red, cambios concurrentes, reintentos, app cerrada a mitad de sync.

## Fase 3 — Interfaz (experiencia Apple)
- [ ] **T13 Login + asistente de fiabilidad** (RF-01), incluyendo aviso de prueba.
- [ ] **T14 Home**: búsqueda (sin funcionar aún), 4 botones de colores con contadores, "Mis listas", nueva tarea / añadir lista (RF-02).
- [ ] **T15 Vista de lista**: checkbox redondo, título, notas, fecha, prioridad, etiquetas, iconos; completar con animación y deshacer; añadir en línea; mostrar completadas (RF-03, RF-04).
- [ ] **T16 Vistas inteligentes en la UI**: Hoy (vencidas arriba), Programados (agrupados por día), Todos (agrupados por lista), Completados (por fecha de completado).
- [ ] **T17 Detalle de tarea** (RF-05): todos los campos, guardado automático, diálogo de conflicto.
- [ ] **T18 Editor de recurrencia** (RF-06).
- [ ] **T19 Subtareas** (RF-07): crear, indentar/desindentar, plegar.
- [ ] **T20 Gestión de listas** (RF-08): crear, renombrar, color, icono, reordenar listas y tareas; borrar solo con el ajuste "Permitir borrar listas" activado.
- [ ] **T21 Avisos y posponer** (RF-10): planificador (**100% de cobertura**), notificaciones con acciones, reprogramación en arranque/cambio de hora/actualización, modo alarma, posponer tarea.
- [ ] **T22 Ajustes** (RF-14): tema, idioma, lista por defecto, permitir borrar listas, listas visibles (RF-09), hora de las tareas de todo el día, modo alarma, asistente, cuenta, versión.
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
