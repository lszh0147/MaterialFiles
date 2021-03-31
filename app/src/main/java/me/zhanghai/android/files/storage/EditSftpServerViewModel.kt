/*
 * Copyright (c) 2021 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java8.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import me.zhanghai.android.files.provider.common.newDirectoryStream
import me.zhanghai.android.files.provider.common.readAllBytes
import me.zhanghai.android.files.provider.common.size
import me.zhanghai.android.files.util.ActionState
import java.io.IOException

class EditSftpServerViewModel : ViewModel() {
    private val _readPrivateKeyFileState =
        MutableStateFlow<ActionState<Path, String>>(ActionState.Ready())
    val readPrivateKeyFileState = _readPrivateKeyFileState.asStateFlow()

    fun readPrivateKeyFile(file: Path) {
        viewModelScope.launch {
            if (_readPrivateKeyFileState.value !is ActionState.Ready) {
                return@launch
            }
            _readPrivateKeyFileState.value = ActionState.Running(file)
            try {
                val text = runInterruptible(Dispatchers.IO) {
                    val size = file.size()
                    if (size > MAX_PRIVATE_KEY_FILE_SIZE) {
                        throw IOException("Private key file size $size is too large")
                    }
                    val bytes = file.readAllBytes()
                    String(bytes)
                }
                _readPrivateKeyFileState.value = ActionState.Success(file, text)
            } catch (e: Exception) {
                _readPrivateKeyFileState.value = ActionState.Error(file, e)
            }
        }
    }

    fun finishReadingPrivateKeyFile() {
        viewModelScope.launch {
            _readPrivateKeyFileState.value = ActionState.Ready()
        }
    }

    private val _connectState = MutableStateFlow<ActionState<SftpServer, Unit>>(ActionState.Ready())
    val connectState = _connectState.asStateFlow()

    fun connect(server: SftpServer) {
        viewModelScope.launch {
            if (_connectState.value !is ActionState.Ready) {
                return@launch
            }
            _connectState.value = ActionState.Running(server)
            try {
                runInterruptible(Dispatchers.IO) {
                    SftpServerAuthenticator.addTransientServer(server)
                    try {
                        val path = server.path
                        path.fileSystem.use {
                            path.newDirectoryStream().toList()
                        }
                    } finally {
                        SftpServerAuthenticator.removeTransientServer(server)
                    }
                }
                _connectState.value = ActionState.Success(server, Unit)
            } catch (e: Exception) {
                _connectState.value = ActionState.Error(server, e)
            }
        }
    }

    fun finishConnecting() {
        viewModelScope.launch {
            _connectState.value = ActionState.Ready()
        }
    }

    companion object {
        private const val MAX_PRIVATE_KEY_FILE_SIZE = 1024 * 1024.toLong()
    }
}
