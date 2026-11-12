<!--
SPDX-FileCopyrightText: 2026 UltimateTasks contributors
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

## [1.0.0-rc.7] - 2026-11-12

Foldable and tablet fix.

### Fixed
- On foldables and tablets, the "New task" and "Add list" buttons at the bottom were hidden behind the dock or taskbar. They now stay clear of the navigation bar, the taskbar and the display cutout.

## [1.0.0-rc.6] - 2026-11-12

Add a task and fill in its details at once, and times that follow you when you travel.

### Added
- Pressing Enter on a new task now opens its details, so you can set the date, time, reminder and the rest right away.
- A Time zone row for tasks with a time: floating (the same time wherever you are), your device's zone, or any other zone. When the zone differs from your device's, the equivalent local time is shown too.

### Changed
- A time you set on a task is now floating by default: "9:00" stays 9:00 local when you travel to another time zone, instead of being tied to the zone you were in when you set it.
- Today, the home counts and the list sections update when the day, the clock or the time zone changes, instead of staying out of date until you reopen the screen.
- Time zone names written by Windows and Outlook calendars are understood.

## [1.0.0-rc.5] - 2026-10-20

Sync right away once the local network is allowed.

### Fixed
- After you allowed local network access, or after a run of failed syncs, opening the app or pulling to refresh could do nothing for a long while (up to hours), because the app waited for the retry delay of the failed syncs. Now it syncs at once.

## [1.0.0-rc.4] - 2026-10-19

Reach a server on your local network on Android 17.

### Fixed
- On Android 17 the app could not reach a server on your local network (for example a home server reached through split DNS), and every sync timed out. Now, only when your server is on the local network, the app explains why it needs the "Nearby devices" permission and asks for it.

## [1.0.0-rc.3] - 2026-10-15

### Changed

- A new task or subtask is kept when you tap elsewhere or leave the screen, as Enter does: no need to press Enter.
- Gradle 9.8.1 and Kotlin 2.4.21.

## [1.0.0-rc.2] - 2026-10-05

Second release candidate: reminders that survive phones which stop apps in the background.

### Added

- Missed reminders come back: when the app runs again (start, sync or any alarm), reminders the phone kept from showing in the last 24 hours (6, 24, 48 hours or never) are shown, marked "Missed at 10:30".
- Heartbeat: while reminders are pending, a silent alarm every 30 minutes brings back missed ones and sets every alarm again.
- Robust mode (optional, recommended on OPPO, Xiaomi, vivo, Huawei/Honor…): a foreground service with a minimal ongoing notification keeps the app alive so reminders arrive; it never uses the network.
- The test reminder now travels the real way, an alarm a minute ahead, and says whether it arrived on time, late or not at all.
- Reminders guide: warns when notifications are blocked in the system settings, asks not to pause the app when unused, and gives exact steps for each phone maker's settings.

### Fixed

- Notifications counted as allowed when the phone blocked them in its settings.
 - 2026-10-04

First release candidate of 1.0.0.

### Added

- Sign in with Nextcloud's Login Flow v2; the app password is encrypted with the Android Keystore.
- Offline-first CalDAV sync of Nextcloud tasks (VTODO): local database, operation queue with retries and backoff, ETags and sync tokens, periodic sync and pull to refresh. Unknown iCalendar properties are kept untouched.
- Conflicts never lost: a title or notes changed on both sides waits for you to choose; other fields are merged.
- Home as in Apple Reminders: Today, Scheduled, All and Completed tiles, then your lists; search.
- Lists: create, rename, recolor, choose an icon, reorder; delete only once allowed in Settings. Choose which lists are visible, so Deck's task lists stay out of the way.
- Tasks: round checkboxes with undo, notes, due date and time, priority, tags, URL, subtasks, moving between lists, manual or sorted order.
- Repeating tasks, including "the 3rd Friday of every month" and custom rules.
- Reminders that arrive: exact alarms, early reminders, snooze from the notification, an alarm-clock mode and a wizard for phones that stop apps in the background.
- Share text from any app to create a task.
- Settings: theme, pure black, dynamic colors, language, default list; export and restore them, optionally with the session sealed by a password.
- Tablets and wide windows: two or three panes.
- Accessibility: TalkBack reads each task as one element with its actions; layouts hold at 200% font size.
- English and Spanish.

[Unreleased]: https://github.com/qtekfun/UltimateTasks/compare/v1.0.0-rc.5...HEAD
[1.0.0-rc.5]: https://github.com/qtekfun/UltimateTasks/compare/v1.0.0-rc.4...v1.0.0-rc.5
[1.0.0-rc.4]: https://github.com/qtekfun/UltimateTasks/compare/v1.0.0-rc.3...v1.0.0-rc.4
[1.0.0-rc.3]: https://github.com/qtekfun/UltimateTasks/compare/v1.0.0-rc.2...v1.0.0-rc.3
[1.0.0-rc.2]: https://github.com/qtekfun/UltimateTasks/compare/v1.0.0-rc.1...v1.0.0-rc.2
[1.0.0-rc.1]: https://github.com/qtekfun/UltimateTasks/releases/tag/v1.0.0-rc.1
