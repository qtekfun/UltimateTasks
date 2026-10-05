<!--
SPDX-FileCopyrightText: 2026 UltimateTasks contributors
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Privacy policy

*Español más abajo.*

UltimateTasks is a client for the tasks in your own Nextcloud. It has no servers of its own, no accounts of its own, no ads, no analytics and no telemetry. Nobody but you and your Nextcloud server sees your data.

## What data goes where

- **Your data only travels between your device and the Nextcloud server you sign in to**, always over HTTPS (CalDAV). Certificate validation is never disabled; certificates of your own CA installed on the device are accepted.
- **On the device** the app keeps a copy of your task lists and tasks so it works offline. It is deleted when you log out or uninstall the app.
- **Your app password** (created by Nextcloud's Login Flow v2, never your real password) is encrypted with a key stored in the Android Keystore. It never appears in logs or backups.
- **Android's cloud backup is disabled**, so none of this is copied by it.
- **Settings** (theme, language, visible lists, reminders) stay on the device.
- **Backups you export** are files you choose where to keep. They only include your signed-in session if you ask; then the app password inside is encrypted with a password you choose (AES-256-GCM, key derived with PBKDF2). Anyone with the file *and* that password could sign in as you, so keep both safe.
- **Text shared to the app** from another app becomes a new task; nothing else is read.

## Permissions and why

| Permission | Why |
|---|---|
| Internet (`INTERNET`) | To talk to your Nextcloud server. |
| Notifications (`POST_NOTIFICATIONS`) | Task reminders. Asked by the reminders wizard or from Settings → Reminders (Android 13+). |
| Exact alarms (`USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` on Android 12) | So reminders arrive at the time you set, not minutes later. On Android 13+ it is granted at install because reminders are the app's purpose; on Android 12 you allow it in the system settings. |
| Alarm clock mode (optional setting) | Some phones delay even exact alarms to save battery. In this mode reminders are set like an alarm clock, which no battery saver delays; Android then shows the alarm icon while one is pending. |
| Ignore battery optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | Only on request, from the reminders wizard or Settings → Reminders: it opens the system dialog that lets the app run in the background so reminders are not blocked. Google Play limits which apps may ask this; UltimateTasks is distributed outside Play, and reminders are exactly the use case it exists for. You decide. |
| Foreground service (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`) | Only in **robust mode**, which is off unless you turn it on (Settings → Reminders or the reminders wizard). Some phones (ColorOS, MIUI, OriginOS…) kill apps in the background and their alarms are lost; this service only keeps UltimateTasks running so the reminders scheduled on your phone arrive. It does **nothing on the network** and reads nothing. Its fixed notification says what it is for and has a **Turn off** button; turning the mode off in Settings stops it too. |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Alarms are lost when the phone restarts or the time zone changes; this lets the app schedule your reminders again and, if robust mode is on, start its service. It does nothing else. |

No location, contacts, camera, microphone or storage permission is requested.

## Contact

Questions or concerns: open an issue in the project repository.

---

# Política de privacidad

UltimateTasks es un cliente para las tareas de tu propio Nextcloud. No tiene servidores propios, ni cuentas propias, ni anuncios, ni analíticas, ni telemetría. Nadie más que tú y tu servidor Nextcloud ve tus datos.

## Qué datos van adónde

- **Tus datos solo viajan entre tu dispositivo y el servidor Nextcloud en el que inicias sesión**, siempre por HTTPS (CalDAV). Nunca se desactiva la validación de certificados; se aceptan los de tu propia CA instalados en el dispositivo.
- **En el dispositivo** la app guarda una copia de tus listas y tareas para funcionar sin conexión. Se borra al cerrar sesión o desinstalar la app.
- **Tu contraseña de aplicación** (la crea el Login Flow v2 de Nextcloud; nunca es tu contraseña real) se cifra con una clave guardada en el Android Keystore. Nunca aparece en registros ni copias de seguridad.
- **La copia en la nube de Android está desactivada**: no copia nada de esto.
- **Los ajustes** (tema, idioma, listas visibles, avisos) se quedan en el dispositivo.
- **Las copias que exportas** son archivos que guardas donde quieras. Solo incluyen tu sesión iniciada si lo pides; entonces la contraseña de aplicación va cifrada con una contraseña que eliges (AES-256-GCM, clave derivada con PBKDF2). Quien tenga el archivo *y* esa contraseña podría entrar como tú: guarda bien ambos.
- **El texto que compartes a la app** desde otra app se convierte en una tarea nueva; no se lee nada más.

## Permisos y por qué

| Permiso | Por qué |
|---|---|
| Internet (`INTERNET`) | Para hablar con tu servidor Nextcloud. |
| Notificaciones (`POST_NOTIFICATIONS`) | Avisos de tareas. Se piden desde el asistente de avisos o en Ajustes → Avisos (Android 13+). |
| Alarmas exactas (`USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` en Android 12) | Para que los avisos lleguen a la hora que pusiste y no minutos después. En Android 13+ se concede al instalar porque los avisos son el propósito de la app; en Android 12 lo permites en los ajustes del sistema. |
| Modo alarma (ajuste opcional) | Algunos móviles retrasan incluso las alarmas exactas para ahorrar batería. En este modo los avisos se programan como un despertador, que ningún ahorro de batería retrasa; Android muestra entonces el icono de alarma mientras haya uno pendiente. |
| Ignorar la optimización de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | Solo si lo pides, desde el asistente de avisos o Ajustes → Avisos: abre la ventana del sistema para dejar que la app funcione en segundo plano y no se bloqueen los avisos. Google Play limita qué apps pueden pedirlo; UltimateTasks se distribuye fuera de Play y los avisos son justo el caso para el que existe. Decides tú. |
| Servicio en primer plano (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`) | Solo en el **modo robusto**, desactivado salvo que lo actives (Ajustes → Avisos o el asistente de avisos). Algunos móviles (ColorOS, MIUI, OriginOS…) cierran las apps en segundo plano y sus alarmas se pierden; este servicio solo mantiene UltimateTasks activa para que lleguen los avisos programados en tu móvil. **No usa la red** ni lee nada. Su notificación fija explica para qué sirve y tiene un botón **Desactivar**; desactivar el modo en Ajustes también lo para. |
| Inicio con el sistema (`RECEIVE_BOOT_COMPLETED`) | Las alarmas se pierden al reiniciar el móvil o cambiar de zona horaria; esto permite volver a programar tus avisos y, si el modo robusto está activo, arrancar su servicio. No hace nada más. |

No se pide ubicación, contactos, cámara, micrófono ni almacenamiento.

## Contacto

Dudas o problemas: abre una incidencia en el repositorio del proyecto.
