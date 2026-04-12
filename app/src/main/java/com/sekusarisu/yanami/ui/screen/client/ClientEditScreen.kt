package com.sekusarisu.yanami.ui.screen.client

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import org.koin.core.parameter.parametersOf

class ClientEditScreen(private val uuid: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ClientEditViewModel> { parametersOf(uuid) }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ClientEditContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is ClientEditContract.Effect.NavigateBack -> navigator.pop()
                    is ClientEditContract.Effect.NavigateToServerRelogin -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(
                                ServerReLoginScreen(
                                        serverId = effect.serverId,
                                        forceTwoFa = effect.forceTwoFa
                                )
                        )
                    }
                    is ClientEditContract.Effect.NavigateToServerEdit -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(AddServerScreen(editServerId = effect.serverId))
                    }
                }
            }
        }

        Scaffold(
                topBar = {
                    TopAppBar(
                            title = {
                                Text(
                                        state.clientName.ifBlank {
                                            stringResource(R.string.client_edit_title)
                                        }
                                )
                            },
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
            when {
                state.isLoading -> {
                    Column(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    AdaptiveContentPane(
                            modifier = Modifier.padding(innerPadding),
                            maxWidth = 840.dp
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text(state.error.orEmpty())
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                    onClick =
                                            soundClick {
                                                viewModel.onEvent(ClientEditContract.Event.Retry)
                                            }
                            ) { Text(stringResource(R.string.action_retry)) }
                        }
                    }
                }
                else -> {
                    AdaptiveContentPane(
                            modifier = Modifier.padding(innerPadding),
                            maxWidth = 840.dp
                    ) {
                        Column(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .padding(16.dp)
                                                .verticalScroll(rememberScrollState())
                        ) {
                            EditTextField(
                                    label = stringResource(R.string.client_edit_name),
                                    value = state.draft.name
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.NAME,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_group),
                                    value = state.draft.group
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.GROUP,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_tags),
                                    value = state.draft.tags
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.TAGS,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_remark),
                                    value = state.draft.remark,
                                    singleLine = false
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.REMARK,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_public_remark),
                                    value = state.draft.publicRemark,
                                    singleLine = false
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.PUBLIC_REMARK,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_weight),
                                    value = state.draft.weight
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.WEIGHT,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_price),
                                    value = state.draft.price
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.PRICE,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_billing_cycle),
                                    value = state.draft.billingCycle
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.BILLING_CYCLE,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_currency),
                                    value = state.draft.currency
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.CURRENCY,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_expired_at),
                                    value = state.draft.expiredAt
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.EXPIRED_AT,
                                                it
                                        )
                                )
                            }
                            EditTextField(
                                    label = stringResource(R.string.client_edit_traffic_limit),
                                    value = state.draft.trafficLimit
                            ) {
                                viewModel.onEvent(
                                        ClientEditContract.Event.TextChanged(
                                                ClientEditContract.TextField.TRAFFIC_LIMIT,
                                                it
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.client_edit_auto_renewal))
                                Switch(
                                        checked = state.draft.autoRenewal,
                                        onCheckedChange = {
                                            viewModel.onEvent(
                                                    ClientEditContract.Event.AutoRenewalChanged(it)
                                            )
                                        }
                                )
                            }

                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.client_edit_hidden))
                                Switch(
                                        checked = state.draft.hidden,
                                        onCheckedChange = {
                                            viewModel.onEvent(
                                                    ClientEditContract.Event.HiddenChanged(it)
                                            )
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.client_edit_traffic_limit_type))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                listOf("max", "sum", "min", "up", "down").forEach { type ->
                                    FilterChip(
                                            selected = state.draft.trafficLimitType == type,
                                            onClick = {
                                                viewModel.onEvent(
                                                        ClientEditContract.Event
                                                                .TrafficLimitTypeChanged(type)
                                                )
                                            },
                                            label = { Text(type) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                    onClick =
                                            soundClick {
                                                viewModel.onEvent(ClientEditContract.Event.Save)
                                            },
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
            }
        }
    }
}

@Composable
private fun EditTextField(
        label: String,
        value: String,
        singleLine: Boolean = true,
        onValueChange: (String) -> Unit
) {
    OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
}
