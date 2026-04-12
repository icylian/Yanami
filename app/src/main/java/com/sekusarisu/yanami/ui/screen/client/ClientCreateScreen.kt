package com.sekusarisu.yanami.ui.screen.client

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.soundClick

class ClientCreateScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ClientCreateViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ClientCreateContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is ClientCreateContract.Effect.NavigateBack -> navigator.pop()
                    is ClientCreateContract.Effect.NavigateToServerRelogin -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(
                                ServerReLoginScreen(
                                        serverId = effect.serverId,
                                        forceTwoFa = effect.forceTwoFa
                                )
                        )
                    }
                    is ClientCreateContract.Effect.NavigateToServerEdit -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(AddServerScreen(editServerId = effect.serverId))
                    }
                }
            }
        }

        Scaffold(
                topBar = {
                    TopAppBar(
                            title = { Text(stringResource(R.string.client_management_create)) },
                            navigationIcon = {
                                IconButton(onClick = soundClick { navigator.pop() }) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription =
                                                    stringResource(R.string.action_back)
                                    )
                                }
                            }
                    )
                }
        ) { innerPadding ->
            AdaptiveContentPane(
                    modifier = Modifier.padding(innerPadding),
                    maxWidth = 840.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(stringResource(R.string.client_create_hint))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                            value = state.name,
                            onValueChange = {
                                viewModel.onEvent(ClientCreateContract.Event.NameChanged(it))
                            },
                            label = { Text(stringResource(R.string.client_edit_name)) },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                            onClick = soundClick { viewModel.onEvent(ClientCreateContract.Event.Save) },
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }

        state.createdResult?.let { result ->
            AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.client_create_success_title)) },
                    text = {
                        Column {
                            Text("UUID")
                            Text(result.uuid)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Token")
                            Text(result.token)
                        }
                    },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(result.token))
                                    Toast.makeText(
                                                    context,
                                                    context.getString(
                                                            R.string.client_management_token_copied
                                                    ),
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                }
                        ) { Text(stringResource(R.string.action_copy)) }
                    },
                    dismissButton = {
                        TextButton(
                                onClick = {
                                    viewModel.onEvent(
                                            ClientCreateContract.Event.DismissCreatedResult
                                    )
                                }
                        ) { Text(stringResource(R.string.action_close)) }
                    }
            )
        }
    }
}
