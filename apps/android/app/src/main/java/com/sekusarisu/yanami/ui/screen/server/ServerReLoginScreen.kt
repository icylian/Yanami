package com.sekusarisu.yanami.ui.screen.server

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.nodelist.NodeListScreen
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.soundClick
import org.koin.core.parameter.parametersOf

/** 会话失效后的重登录页面 */
class ServerReLoginScreen(private val serverId: Long, private val forceTwoFa: Boolean = false) :
        Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ServerReLoginViewModel> { parametersOf(serverId, forceTwoFa) }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ServerReLoginContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is ServerReLoginContract.Effect.NavigateToNodeList -> {
                        navigator.pop()
                        navigator.push(NodeListScreen())
                    }
                }
            }
        }

        ServerReLoginContent(
            state = state,
            onEvent = viewModel::onEvent,
            onBack = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerReLoginContent(
    state: ServerReLoginContract.State,
    onEvent: (ServerReLoginContract.Event) -> Unit,
    onBack: () -> Unit
) {
    val adaptiveInfo = rememberAdaptiveLayoutInfo()
    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(stringResource(R.string.server_relogin_title)) },
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
        val lockCredentials = state.requires2fa && state.username.isNotBlank() && state.password.isNotBlank()
        AdaptiveContentPane(
                modifier = Modifier.padding(innerPadding),
                maxWidth = if (adaptiveInfo.isTabletLandscape) 760.dp else 680.dp
        ) {
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text =
                            if (state.serverName.isBlank()) ""
                            else stringResource(
                                    R.string.server_relogin_target,
                                    state.serverName
                            ),
                    style = MaterialTheme.typography.titleMedium
            )
            if (state.baseUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = state.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                    text = stringResource(R.string.server_relogin_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                    value = state.username,
                    onValueChange = {
                        onEvent(ServerReLoginContract.Event.UsernameChanged(it))
                    },
                    label = { Text(stringResource(R.string.add_server_username_label)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    readOnly = lockCredentials,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                    value = state.password,
                    onValueChange = {
                        onEvent(ServerReLoginContract.Event.PasswordChanged(it))
                    },
                    label = { Text(stringResource(R.string.add_server_password_label)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    readOnly = lockCredentials,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions =
                            KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction =
                                            if (state.requires2fa) ImeAction.Next
                                            else ImeAction.Done
                            )
            )

            if (state.requires2fa) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                        value = state.twoFaCode,
                        onValueChange = {
                            onEvent(ServerReLoginContract.Event.TwoFaCodeChanged(it))
                        },
                        label = { Text(stringResource(R.string.add_server_2fa_label)) },
                        placeholder = { Text(stringResource(R.string.add_server_2fa_hint)) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions =
                                KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                )
                )
            }

            state.error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

                Button(
                        onClick = soundClick { onEvent(ServerReLoginContract.Event.Submit) },
                        enabled = !state.isLoading && !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.server_relogin_action))
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ServerReLoginScreenPreview() {
    MaterialTheme {
        ServerReLoginContent(
            state = ServerReLoginContract.State(
                serverName = "My Server",
                baseUrl = "https://example.com/api",
                requires2fa = true,
                username = "admin",
                password = "password"
            ),
            onEvent = {},
            onBack = {}
        )
    }
}
