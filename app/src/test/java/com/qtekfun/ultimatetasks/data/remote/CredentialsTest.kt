// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CredentialsTest {
    @Test
    fun `never shows the app password when printed`() {
        assertEquals(
            "Credentials(loginName=ana, appPassword=***)",
            Credentials("ana", "secret").toString()
        )
    }
}
