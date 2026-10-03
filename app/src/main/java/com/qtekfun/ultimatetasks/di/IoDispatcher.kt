// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.di

import javax.inject.Qualifier

/** The dispatcher for database and network work, injected so tests can replace it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
