<!--
SPDX-FileCopyrightText: 2026 UltimateTasks contributors
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

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

[Unreleased]: https://github.com/qtekfun/UltimateTasks/commits/HEAD
