package com.sekusarisu.yanami.ui.screen.server

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.CustomHeader
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.nodelist.NodeListScreen
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.soundClick
import org.koin.core.parameter.parametersOf

/** 添加服务器实例页面 */
class AddServerScreen(private val editServerId: Long? = null) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AddServerViewModel> { parametersOf(editServerId) }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val isEditMode = editServerId != null

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ServerContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is ServerContract.Effect.ServerSaved -> {
                        Toast.makeText(
                                        context,
                                        context.getString(R.string.add_server_saved),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                        navigator.push(NodeListScreen())
                    }
                    is ServerContract.Effect.NavigateBack -> {
                        navigator.pop()
                    }
                    is ServerContract.Effect.ServerUpdated -> {
                        Toast.makeText(
                                        context,
                                        context.getString(R.string.server_updated),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                        navigator.pop()
                    }
                    else -> {}
                }
            }
        }

        AddServerContent(
            isEditMode = isEditMode,
            state = state,
            onEvent = viewModel::onEvent,
            onBack = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerContent(
    isEditMode: Boolean,
    state: ServerContract.AddState,
    onEvent: (ServerContract.Event) -> Unit,
    onBack: () -> Unit
) {
    val adaptiveInfo = rememberAdaptiveLayoutInfo()
    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Text(
                                    stringResource(
                                            if (isEditMode) R.string.server_edit
                                            else R.string.server_add
                                    )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = soundClick { onBack() }) {
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
                maxWidth = if (adaptiveInfo.isTabletLandscape) 900.dp else 840.dp
        ) {
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                    value = state.name,
                    onValueChange = { onEvent(ServerContract.Event.UpdateName(it)) },
                    label = { Text(stringResource(R.string.add_server_name_label)) },
                    placeholder = { Text(stringResource(R.string.add_server_name_hint)) },
                    leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = {
                        onEvent(ServerContract.Event.UpdateBaseUrl(it))
                    },
                    label = { Text(stringResource(R.string.add_server_url_label)) },
                    placeholder = { Text(stringResource(R.string.add_server_url_hint)) },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions =
                            KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Next
                            )
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomHeadersSection(
                    customHeaders = state.customHeaders,
                    onEvent = onEvent
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 认证模式切换
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                        selected = state.authType == AuthType.PASSWORD,
                        onClick =
                                soundClick {
                                    onEvent(
                                            ServerContract.Event.UpdateAuthType(AuthType.PASSWORD)
                                    )
                                },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text(stringResource(R.string.auth_type_password))
                }
                SegmentedButton(
                        selected = state.authType == AuthType.API_KEY,
                        onClick =
                                soundClick {
                                    onEvent(
                                            ServerContract.Event.UpdateAuthType(AuthType.API_KEY)
                                    )
                                },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text(stringResource(R.string.auth_type_api_key))
                }
                SegmentedButton(
                        selected = state.authType == AuthType.GUEST,
                        onClick =
                                soundClick {
                                    onEvent(
                                            ServerContract.Event.UpdateAuthType(AuthType.GUEST)
                                    )
                                },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text(stringResource(R.string.auth_type_guest))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 根据认证模式显示不同的输入字段
            AnimatedVisibility(visible = state.authType == AuthType.PASSWORD) {
                Column {
                    OutlinedTextField(
                            value = state.username,
                            onValueChange = {
                                onEvent(ServerContract.Event.UpdateUsername(it))
                            },
                            label = {
                                Text(stringResource(R.string.add_server_username_label))
                            },
                            placeholder = {
                                Text(stringResource(R.string.add_server_username_hint))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                            value = state.password,
                            onValueChange = {
                                onEvent(ServerContract.Event.UpdatePassword(it))
                            },
                            label = {
                                Text(stringResource(R.string.add_server_password_label))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions =
                                    KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction =
                                                    if (state.show2faField) ImeAction.Next
                                                    else ImeAction.Done
                                    )
                    )

                    AnimatedVisibility(visible = state.show2faField) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                    value = state.twoFaCode,
                                    onValueChange = {
                                        onEvent(ServerContract.Event.UpdateTwoFaCode(it))
                                    },
                                    label = {
                                        Text(stringResource(R.string.add_server_2fa_label))
                                    },
                                    placeholder = {
                                        Text(stringResource(R.string.add_server_2fa_hint))
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Key, contentDescription = null)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions =
                                            KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Done
                                            )
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.authType == AuthType.API_KEY) {
                OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = {
                            onEvent(ServerContract.Event.UpdateApiKey(it))
                        },
                        label = { Text(stringResource(R.string.add_server_api_key_label)) },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions =
                                KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val testEnabled =
                    !state.isTesting &&
                            state.baseUrl.isNotBlank() &&
                            when (state.authType) {
                                AuthType.PASSWORD ->
                                        state.username.isNotBlank() && state.password.isNotBlank()
                                AuthType.API_KEY -> state.apiKey.isNotBlank()
                                AuthType.GUEST -> true
                            }

            FilledTonalButton(
                    onClick = soundClick { onEvent(ServerContract.Event.TestConnection) },
                    enabled = testEnabled,
                    modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Wifi, contentDescription = null)
                }
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(stringResource(R.string.add_server_test_connection))
            }

            state.testResult?.let { result ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Text(
                            text = result,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            state.testError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.errorContainer
                                )
                ) {
                    Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val saveEnabled =
                    !state.isSaving &&
                            state.name.isNotBlank() &&
                            state.baseUrl.isNotBlank() &&
                            when (state.authType) {
                                AuthType.PASSWORD ->
                                        state.username.isNotBlank() && state.password.isNotBlank()
                                AuthType.API_KEY -> state.apiKey.isNotBlank()
                                AuthType.GUEST -> true
                            }

            Button(
                    onClick = soundClick { onEvent(ServerContract.Event.SaveServer) },
                    enabled = saveEnabled,
                    modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                }
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(stringResource(R.string.action_save))
            }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CustomHeadersSection(
        customHeaders: List<CustomHeader>,
        onEvent: (ServerContract.Event) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
                text = stringResource(R.string.add_server_custom_headers_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        customHeaders.forEachIndexed { index, header ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                            value = header.name,
                            onValueChange = {
                                onEvent(ServerContract.Event.UpdateCustomHeaderName(index, it))
                            },
                            label = {
                                Text(stringResource(R.string.add_server_header_name_label))
                            },
                            placeholder = {
                                Text(stringResource(R.string.add_server_header_name_hint))
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    IconButton(
                            onClick =
                                    soundClick {
                                        onEvent(ServerContract.Event.RemoveCustomHeader(index))
                                    }
                    ) {
                        Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription =
                                        stringResource(R.string.add_server_remove_header)
                        )
                    }
                }

                OutlinedTextField(
                        value = header.value,
                        onValueChange = {
                            onEvent(ServerContract.Event.UpdateCustomHeaderValue(index, it))
                        },
                        label = { Text(stringResource(R.string.add_server_header_value_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions =
                                KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                )
                )
            }
        }

        FilledTonalButton(
                onClick = soundClick { onEvent(ServerContract.Event.AddCustomHeader) },
                modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text(stringResource(R.string.add_server_add_header))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AddServerScreenPreview() {
    MaterialTheme {
        AddServerContent(
            isEditMode = false,
            state = ServerContract.AddState(),
            onEvent = {},
            onBack = {}
        )
    }
}
