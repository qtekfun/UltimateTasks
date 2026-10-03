// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.auth

import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.AccountCredentialsEntity
import com.qtekfun.ultimatetasks.data.remote.Credentials
import javax.inject.Inject

/** Stores app passwords encrypted at rest; plaintext only ever exists in memory. */
class CredentialStore @Inject constructor(
    database: UltimateTasksDatabase,
    private val cipher: SecretCipher
) {
    private val dao = database.credentialsDao()

    suspend fun save(accountId: Long, credentials: Credentials) {
        val secret = cipher.encrypt(credentials.appPassword.toByteArray(Charsets.UTF_8))
        dao.put(
            AccountCredentialsEntity(accountId, credentials.loginName, secret.ciphertext, secret.iv)
        )
    }

    suspend fun load(accountId: Long): Credentials? = dao.get(accountId)?.let { stored ->
        val password = cipher.decrypt(EncryptedSecret(stored.ciphertext, stored.iv))
        Credentials(stored.loginName, password.toString(Charsets.UTF_8))
    }
}
