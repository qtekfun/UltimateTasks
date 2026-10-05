# UltimateTasks — Especificación (SPEC)

> Fuente: entrevista con el autor (2026-10-03). Lo que no esté aquí no se inventa: se pregunta.

## 1. Objetivo
App Android de tareas contra **Nextcloud** (CalDAV, componentes `VTODO`) con una experiencia lo más parecida posible a **Recordatorios de Apple**: amigable, rápida, offline-first y con avisos que **llegan siempre**, también en móviles con capas agresivas (ColorOS, MIUI/HyperOS, EMUI, OneUI…).

### Problemas que resuelve
- Las apps actuales (Nextcloud Tasks web, OpenTasks + DAVx5, tasks.org, jtx) o dependen de otra app para sincronizar, o tienen una interfaz poco amable, o fallan con los avisos en móviles chinos.
- Deck publica sus tableros como listas de tareas CalDAV: en otras apps aparecen mezcladas con las tareas. Aquí el usuario **elige qué listas ve** (incluidas las de Deck).

## 2. Alcance y supuestos
- Una sola cuenta Nextcloud por instalación (Login Flow v2, contraseña de aplicación).
- Sincronización CalDAV **propia** (sin DAVx5 ni proveedores de tareas de Android).
- Servidor objetivo: Nextcloud con la app Tasks/Calendar (CalDAV en `/remote.php/dav`). Versión mínima a fijar en T05.
- Interoperable con la web de Nextcloud Tasks y otros clientes CalDAV: todo lo que se guarda es iCalendar estándar o la extensión que ya usa Nextcloud Tasks.
- Volumen de referencia: ~20 listas, ~1.000 tareas abiertas, ~5.000 completadas. Debe ir fluido con ese volumen.
- Teléfono y **tablet** (diseño adaptativo).

## 3. Requisitos funcionales (v1)

### RF-01 Inicio de sesión y asistente de avisos
- Login Flow v2 (copiado de UltimateDeck), credenciales cifradas con Keystore; restaurar copia de seguridad desde la pantalla de inicio.
- Tras el login, **asistente de fiabilidad** (se puede repetir desde Ajustes):
  1. Permiso de notificaciones (Android 13+).
  2. Alarmas exactas: `USE_EXACT_ALARM` (Android 13+, concedido al instalar) y `SCHEDULE_EXACT_ALARM` solo hasta Android 12L (decidido en T02b).
  3. Exención de optimización de batería (diálogo del sistema).
  4. Ajustes del fabricante detectado (inicio automático, "no cerrar en segundo plano"…) con instrucciones e intent directo cuando exista.
  5. **Aviso de prueba** en 10 s para comprobar que funciona.
- Cada paso muestra su estado (hecho / pendiente) y por qué se pide.

### RF-02 Pantalla de inicio (estilo Apple)
- Barra de búsqueda arriba (RF-12).
- **Cuatro botones de colores** en cuadrícula 2×2, cada uno con icono, nombre y número de tareas:
  - **Hoy** (azul): pendientes con fecha hoy o vencidas.
  - **Programados** (rojo): pendientes con fecha.
  - **Todos** (gris oscuro): todas las pendientes.
  - **Completados** (gris): completadas.
- Debajo, **"Mis listas"**: cada lista con su icono en círculo de color, nombre y número de pendientes. Reordenables (RF-08).
- Botones inferiores: **"Nueva tarea"** (en la lista por defecto o la abierta) y **"Añadir lista"**.
- Solo cuentan las listas **visibles** (RF-09).

### RF-03 Vista de lista
- Título grande con el color de la lista; tareas en orden manual.
- Cada tarea: **checkbox redondo** (borde del color de la lista; relleno al completar), título, descripción (2 líneas máx.), y debajo la fecha/hora programada (en rojo si está vencida), icono de repetición si es recurrente, prioridad como `!`/`!!`/`!!!` antes del título, etiquetas como `#etiqueta`.
- Subtareas indentadas bajo su padre, plegables.
- **Añadir en línea**: tocar el hueco bajo la última tarea crea una nueva y abre el teclado; Intro crea otra a continuación (como Apple).
- Deslizar: completar / posponer / borrar (con deshacer).
- Menú de la lista: "Mostrar/ocultar completadas", editar lista, ordenar (manual, fecha, prioridad, título).

### RF-04 Completar tareas (como Apple)
- Al marcar: el círculo se rellena con animación, la tarea se queda ~2 s y luego desaparece (salvo con "Mostrar completadas"). Snackbar con **Deshacer**.
- Se guarda `STATUS:COMPLETED`, `COMPLETED`, `PERCENT-COMPLETE:100`. Desmarcar lo revierte.
- Completar una tarea padre no completa las subtareas automáticamente (Apple tampoco); se pregunta si tiene subtareas pendientes.
- Tarea **recurrente**: ver RF-06.

### RF-05 Detalle de tarea
Hoja/pantalla con, en este orden:
- **Título** y **notas** (texto plano multilínea; los enlaces son tocables).
- **URL** (propiedad `URL`).
- **Fecha** (interruptor) y **Hora** (interruptor; sin hora = todo el día, `DUE;VALUE=DATE`).
- **Aviso anticipado**: ninguno, 5/15/30 min, 1 h, 1 día, 1 semana antes, o personalizado → `VALARM` con `TRIGGER` relativo a `DUE`.
- **Repetir** (RF-06).
- **Prioridad**: ninguna / baja / media / alta → `PRIORITY` 0 / 9 / 5 / 1 (mismo mapeo que Apple y Nextcloud Tasks).
- **Etiquetas** (`CATEGORIES`), con autocompletado de las existentes.
- **Lista** (mover a otra lista = mover el recurso CalDAV).
- **Subtareas** (RF-07).
- Guardado automático; cambios offline van a la cola.

### RF-06 Recurrencia
- Presets: diaria, entre semana, semanal, cada 2 semanas, mensual, cada 3 meses, cada 6 meses, anual.
- Personalizada: cada N días/semanas/meses/años; semanal en días concretos (L, M, X…); mensual por día del mes o "el segundo martes"; fin: nunca, en fecha, tras N veces.
- Se guarda como `RRULE` estándar. Reglas creadas fuera que el editor no represente se **muestran y respetan** ("Personalizada (avanzada)") y solo se pueden sustituir, no editar a medias.
- Al completar una recurrente: ver decisión en §9 (propuesta: avanzar `DUE`/`DTSTART` a la siguiente repetición en la misma tarea, como Nextcloud Tasks y tasks.org, y registrar la completada en el historial local de "Completados").
- Motor de recurrencia en `domain`, puro y con **100% de cobertura** (cambios de horario, fin de mes, 29 de febrero, zonas horarias).

### RF-07 Subtareas
- `RELATED-TO;RELTYPE=PARENT`. Un nivel visible de anidamiento en v1 (más niveles del servidor se muestran aplanados bajo el primer padre).
- Crear subtarea desde el detalle o deslizando una tarea a la derecha (indentar) en la lista.

### RF-08 Gestión de listas
- **Crear lista** (siempre disponible; requiere conexión) (calendario CalDAV con soporte `VTODO`), renombrar, **color** (paleta de Apple; se guarda en el servidor como `calendar-color`), **icono** (catálogo propio; **solo local**, Nextcloud no lo guarda).
- Borrar lista: **desactivado por defecto**, como en UltimateDeck; se activa en Ajustes ("Permitir borrar listas") y cada borrado pide confirmación indicando cuántas tareas contiene. Requiere conexión.
- Reordenar listas arrastrando en la home (`calendar-order`).
- Reordenar tareas arrastrando dentro de la lista (`X-APPLE-SORT-ORDER`, el que usa Nextcloud Tasks).
- Las listas de solo lectura (compartidas sin permisos de escritura) se muestran con candado y sin edición.

### RF-09 Listas visibles
- Ajustes → **Listas visibles**: un interruptor por lista. Al principio **todas visibles**; no hay filtros automáticos (tampoco para Deck).
- Una lista oculta desaparece de **todo**: home, Hoy/Programados/Todos/Completados, búsqueda, widgets futuros y **avisos**.
- Se sigue sincronizando (para poder mostrarla al instante) o no, según §9.

### RF-10 Avisos y posponer
- Aviso a la **hora de la tarea**; tareas de todo el día avisan a una hora configurable (por defecto 9:00).
- **Aviso anticipado** opcional por tarea (RF-05), sincronizado como `VALARM`.
- Acciones en la notificación: **Completar**, **Posponer** (15 min, 1 h, mañana a la misma hora). Posponer desde la notificación solo mueve el aviso (local, no toca el servidor).
- **Posponer la tarea** (desde la lista o el detalle): mueve la fecha (mañana, este fin de semana, la próxima semana, elegir) y se sincroniza.
- Fiabilidad:
  - Alarmas exactas reprogramadas en arranque, cambio de hora/zona, actualización de la app y tras cada sync.
  - **Modo alarma** opcional (`setAlarmClock`) en Ajustes para móviles que retrasan alarmas.
  - Canal de notificación de importancia alta; agrupación cuando hay varias.
  - Botón "Enviar aviso de prueba" en Ajustes.
  - **Avisos perdidos (T32):** algunas ROM (ColorOS, MIUI, OriginOS, MagicOS) congelan o detienen la app y sus alarmas se pierden. La app guarda qué avisos se mostraron (id + hora del aviso) y, al arrancar, al terminar cada sync y al recibir cualquier aviso, muestra los que ya pasaron sin mostrarse dentro de una ventana configurable (6 h, **24 h** por defecto, 48 h o nunca), con el texto «No llegó a su hora (10:30)». La primera vez tras actualizar solo registra lo pasado, sin mostrarlo.
  - **Modo robusto (T34):** opcional, desactivado por defecto y recomendado en móviles con ROM agresiva (`PhoneMaker` distinto de `OTHER`), en Ajustes → Avisos y en el asistente. Un servicio en primer plano (`specialUse`) con una notificación fija de importancia mínima mantiene vivo el proceso para que el sistema no lo mate y no se pierdan las alarmas; la notificación explica para qué sirve y permite desactivarlo. Solo ejecuta el latido cada 30 min; **no usa la red**. Funciona mientras el modo esté activo y haya sesión, y arranca tras reiniciar o actualizar la app si lo está.
- Lógica de planificación pura en `domain`, **100% de cobertura** (planificador y selección de avisos perdidos).

#### Diagnóstico en el móvil
Con el móvil conectado por adb:
- `adb shell dumpsys package com.qtekfun.ultimatetasks | grep stopped`: `stopped=true` significa que el sistema (o el usuario) forzó la detención; Android borra entonces **todas** las alarmas de la app hasta que se vuelva a abrir. `stopped=false` con avisos que no llegan apunta a congelación o retrasos de batería.
- `adb shell dumpsys alarm | grep com.qtekfun.ultimatetasks`: las alarmas programadas ahora mismo (una por aviso futuro). Si no sale nada y hay avisos pendientes, se perdieron.
- `adb shell dumpsys deviceidle whitelist`: si aparece `com.qtekfun.ultimatetasks`, la app está exenta de la optimización de batería (Doze).

### RF-12 Búsqueda
- Barra en la home. Busca en título, notas y etiquetas de las listas visibles; resultados agrupados por lista, con opción de incluir completadas.

### RF-13 Compartir a la app
- Compartir texto o enlace desde otra app abre "Nueva tarea" prerrellenada (título = asunto/primera línea; URL al campo URL) en la lista por defecto, con selector de lista.

### RF-14 Ajustes
- Tema claro/oscuro/sistema, negro AMOLED, Material You; idioma de la app (inglés/español).
- **Lista por defecto** para tareas nuevas.
- Listas visibles (RF-09); hora de aviso de las tareas de todo el día; modo alarma; asistente de fiabilidad; aviso de prueba.
- **Permitir borrar listas** (desactivado por defecto; RF-08).
- Copia de seguridad **igual que UltimateDeck**: exportar a un archivo los ajustes (incluidos visibilidad, iconos y orden local de listas) y, opcionalmente, la sesión cifrada con contraseña (AES-GCM); restaurable desde Ajustes y desde la pantalla de inicio de sesión.
- Cuenta y cierre de sesión (borra datos locales); versión de la app abajo.

### RF-15 Tablet y pantallas grandes
- Diseño adaptativo de dos o tres paneles: listas a la izquierda, tareas en el centro y detalle a la derecha cuando cabe (como Recordatorios en iPad). Funciona en apaisado y en ventanas redimensionadas.

## 4. Fuera de alcance (v1)
- **Avisos por ubicación** (llegar/salir de un sitio).
- **Adjuntos**: la app Tasks de Nextcloud no los muestra, así que tampoco se gestionan aquí (decidido el 2026-10-03 tras probarlos). Los `ATTACH` de otros clientes se conservan intactos.
- **Gestionar listas compartidas** (compartir con otros usuarios, permisos). Las listas compartidas contigo sí se ven y se usan.
- Bandera / vista "Marcadas".
- Lenguaje natural para fechas al escribir.
- Widgets, atajos de launcher y tiles de ajustes rápidos.
- Multicuenta y listas solo locales.
- Eventos de calendario (`VEVENT`): solo tareas.
- Cualquier servicio de Google, telemetría o analíticas.

## 5. Sincronización y conflictos
- Descubrimiento: `current-user-principal` → `calendar-home-set` → colecciones cuyo `supported-calendar-component-set` incluya `VTODO`.
- Cambios: `sync-collection` (RFC 6578) con `sync-token`; si no hay soporte, `getctag` + ETags.
- Escritura con `If-Match` (ETag) / `If-None-Match: *` al crear. Respuesta 412 → conflicto.
- El `.ics` original del servidor se guarda junto a la tarea: al escribir se modifica solo lo cambiado y se **conservan las propiedades y componentes desconocidos**.
- Política (como UltimateDeck):
  1. **Título y notas** editados aquí y en el servidor a la vez: no se pisa nada; diálogo "tu versión / la del servidor".
  2. **Resto de campos**: gana el último cambio por campo (fusión a tres bandas contra la última versión sincronizada).
  3. **Tarea borrada en el servidor** con cambios locales: se avisa y se ofrece conservar una copia o descartar.
  4. **Completar** gana a editar otros campos (no se "descompleta" una tarea por un cambio de fecha concurrente).
  5. Operaciones de la cola **idempotentes**, con backoff exponencial y que sobreviven a reinicios.
- Sync periódica (WorkManager, ~15 min), al abrir la app, al volver a primer plano y con tirar para refrescar.
- **Requisito de test:** resolutor y cola con **100% de cobertura**, casos por regla y fallos a mitad de operación (MockWebServer).

## 6. Requisitos no funcionales
- **Rendimiento:** arranque en frío con datos locales < 1,5 s; scroll a 60 fps con el volumen de referencia; marcar una tarea responde en < 100 ms (sin esperar a la red).
- **Privacidad:** sin telemetría ni terceros; los datos solo viajan entre el móvil y el Nextcloud del usuario. HTTPS obligatorio (se aceptan CA de usuario instaladas en el sistema).
- **Seguridad:** credenciales cifradas; `allowBackup` sin credenciales; sin logs de datos sensibles.
- **Accesibilidad:** TalkBack (cada tarea se lee como un elemento con estado del checkbox, fecha, prioridad y lista), objetivos táctiles ≥ 48 dp, fuente grande, contraste.
- **Robustez:** ninguna pérdida de datos ante cierres, falta de red o errores del servidor.
- **Transparencia:** `PRIVACY.md` explica cada permiso (avisos, alarmas exactas, batería, cámara…) y por qué.

### Medidas (T28, 2026-10-03)
Pixel 8 (1080×2400, Android 17), build release minificada firmada con la clave de depuración, datos locales reales (7 listas, ~80 tareas).
- **Arranque en frío** (`am start -W`, 10 veces tras `force-stop`): mediana **126 ms**, máximo **138 ms** (objetivo < 1,5 s).
- **Scroll** de "Todos" (`dumpsys gfxinfo`, 16 desplazamientos): 378 frames, **0,26 %** con tirones; p50 5 ms, p90 6 ms, p99 10 ms (60 fps = 16,7 ms).
- **Fuente al 200 %**: inicio, lista, detalle y ajustes sin cortes ni solapes (filas etiqueta/valor reparten el ancho; la prioridad pasa a chips).
- **TalkBack**: cada tarea es un elemento (título, prioridad en palabras, notas, fecha con "vencida", lista) con su estado (hecha/pendiente) y la acción de marcarla; abrir tiene etiqueta.
- No hace falta Baseline Profile: las cifras están muy por debajo del objetivo.

## 7. Calidad y CI
- Igual que UltimateDeck: GitHub Actions con build, detekt, ktlint, Android Lint, tests unitarios y Kover en cada PR; Dependabot; verificación de dependencias de Gradle; comprobación de licencias y de Play Services.
- **Cobertura:** ≥ 85% global en `domain`/`data`/`sync`; 100% en resolutor, cola, recurrencia y planificador de avisos.
- **Corpus iCalendar** (`app/src/test/resources/ics-corpus/`): ida y vuelta byte a byte de lo no modificado.
- **Tests de UI:** login (falso), completar con deshacer, crear tarea en línea, vista Hoy.
- **Releases:** SemVer, Conventional Commits, tags `vX.Y.Z`, build reproducible firmada por el autor, receta F-Droid.

## 8. Riesgos conocidos
1. **iCalendar y WebDAV**: elegir librerías libres que funcionen en Android y conserven lo desconocido, o escribir un parser propio. Se prototipa primero (T02).
2. **Recurrencia**: semántica de completar recurrentes entre clientes (Nextcloud Tasks, Apple, Thunderbird) y RRULE complejas.
3. **Avisos en capas de fabricantes**: no hay garantía total; se mitiga con el asistente, modo alarma y aviso de prueba, y se documenta por fabricante.
5. **Listas de Deck vía CalDAV**: comportamiento de escritura (¿se pueden completar o editar desde CalDAV?) a verificar contra el servidor.

## 9. Decisiones abiertas
- Diseño adaptativo para tablet: `material3-adaptive` (Apache-2.0, nueva dependencia, a consultar) o layout propio.
- Semántica de completar recurrentes (propuesta en RF-06), a validar contra Nextcloud Tasks web.
- ¿Las listas ocultas se siguen sincronizando? Propuesta: sí, solo metadatos y tareas (barato), para mostrarlas al instante al hacerlas visibles.
- Versión mínima de Nextcloud soportada.

### Decisiones tomadas (entrevista 2026-10-03)
- Sync CalDAV propia; una cuenta; nombre UltimateTasks (`com.qtekfun.ultimatetasks`); repo `qtekfun/UltimateTasks`.
- Código común copiado y adaptado desde UltimateDeck (no librería compartida).
- Recurrencia con presets + personalizada; posponer = aviso (local) y fecha (sincronizada).
- Campos: hora opcional, subtareas, prioridad, etiquetas, URL. Los adjuntos se implementaron y se retiraron (Nextcloud Tasks no los muestra).
- Visibilidad de listas manual en Ajustes, sin filtros automáticos; las ocultas desaparecen de todo, incluidos los avisos.
- Completar como Apple; aviso a la hora + anticipado; asistente de fiabilidad tras el login.
- Extras v1: búsqueda y compartir a la app; tablet incluida.
- **CalDAV e iCalendar propios (T02, 2026-10-03):** sin librerías externas.
  - `ical4j` 4.x descartada: en Android necesita *desugaring* y silenciar `java.time.zone.ZoneRulesProvider`, que no existe (riesgo de fallos en zonas horarias); añade ~970 KB al APK de release y, en el móvil, normalizó el texto (mayúsculas en parámetros) y unió líneas con finales LF.
  - `dav4jvm` descartada: solo se publica en JitPack y usa Ktor, un segundo cliente HTTP junto a OkHttp.
  - Implementación propia en `data/ical`: lector y escritor que conservan byte a byte lo no modificado (corpus de DAVx5 + casos límite, CRLF y LF). CalDAV irá sobre OkHttp (T05); la recurrencia, en el motor propio de T10.
- Crear listas siempre; borrarlas solo tras activarlo en Ajustes. Copias de seguridad como en UltimateDeck.
- **Avisos (T02b, 2026-10-03):** se copia la solución de UltimateDeck (planificador puro, alarma exacta, modo alarma opcional con `setAlarmClock`, petición de exención de batería, reprogramación en arranque).
  - Medido en el móvil del autor (OnePlus CPH2841, ColorOS, Android 16) sin exención de batería: las alarmas `setExactAndAllowWhileIdle` llegaron agrupadas, una 73 s tarde y otra 106 s **antes** de tiempo; `setAlarmClock` llegó al segundo. Por eso el asistente (RF-01) pide la exención de batería y ofrece el modo alarma.
  - Permisos: `USE_EXACT_ALARM` en Android 13+ (concedido al instalar; F-Droid no tiene la restricción de Play) y `SCHEDULE_EXACT_ALARM` con `maxSdkVersion` 32.
  - En ColorOS, `pm grant` de notificaciones por adb está bloqueado: los tests en dispositivo conceden el permiso a mano.
- **Avisos perdidos (T32, 2026-10-05):** recuperar en vez de confiar solo en las alarmas, porque una app detenida pierde todas las suyas y nada local las devuelve hasta que vuelve a ejecutarse.
  - Se registra cada aviso mostrado por id y hora del aviso (el id `2·tarea` se reutiliza cuando cambia la fecha), en Room (v7, tabla `shown_reminder`); los registros se borran pasadas 48 h.
  - Ventana por defecto 24 h: un aviso de hace más de un día ya no ayuda y molestaría. Una posposición vencida cuenta como la hora del aviso para recuperarla.
  - Al actualizar a esta versión no había registro: la primera ejecución marca lo pasado como mostrado para no inundar de avisos.
- **Modo robusto (T34, 2026-10-05):** servicio en primer plano de tipo `specialUse` porque ninguno de los tipos con nombre encaja (no reproduce, no sincroniza datos, no usa ubicación); la justificación va en `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`. F-Droid no tiene la revisión de tipos de Play.
  - Opcional y apagado por defecto: muestra una notificación fija y no es necesario en móviles con Android estándar (Pixel).
  - Android 12+ no deja arrancarlo desde segundo plano salvo excepciones: se arranca al activar el modo (app en pantalla), al iniciar sesión y desde `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` (permitidos para `specialUse`, también en Android 15); si el sistema lo rechaza en otro momento, se arranca la próxima vez que se pueda.
  - El trabajo periódico va tras la interfaz `ReminderBeat` (por ahora replanificar los avisos), para que el latido de T33 se conecte cambiando solo el binding.
- **Servidor real (T09, 2026-10-03, contra el Nextcloud del autor):**
  - Las listas de tareas normales admiten `sync-collection` (token de sincronización) y escritura.
  - Las listas que publica Deck ("Deck: <tablero>") son de **solo lectura** por CalDAV, responden `sync-collection` con HTTP 415 y no envían `getctag`: se descargan enteras (solo ETags y las tareas cambiadas) en cada sync. La app las muestra como listas de solo lectura (RF-08).
  - El parser DOM de Android no admite la opción `disallow-doctype-decl`; se activa solo donde existe (no resuelve entidades externas en ningún caso).
