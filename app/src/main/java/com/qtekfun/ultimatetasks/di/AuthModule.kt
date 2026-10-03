// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.di

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.auth.AndroidKeystoreCipher
import com.qtekfun.ultimatetasks.data.auth.SecretCipher
import com.qtekfun.ultimatetasks.data.remote.CredentialsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    /** Credentials are encrypted with a key that lives in Android Keystore. */
    @Binds
    abstract fun secretCipher(cipher: AndroidKeystoreCipher): SecretCipher

    /** Network clients read the signed-in account's app password from the session. */
    @Binds
    abstract fun credentials(session: AccountSession): CredentialsProvider
}
