package com.sekusarisu.yanami.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.soundClick

class AboutScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current
        val context = LocalContext.current
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val viewModel = koinScreenModel<AboutViewModel>()
        val state by viewModel.state.collectAsState()
        val adaptiveInfo = rememberAdaptiveLayoutInfo()

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is AboutEffect.ShowToast -> {
                        val message = when (effect.message) {
                            "update_check_failed" -> context.getString(R.string.update_check_failed)
                            "update_already_latest" -> context.getString(R.string.update_already_latest)
                            else -> effect.message
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                            title = { Text(stringResource(R.string.about_title)) },
                            scrollBehavior = scrollBehavior,
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
                    maxWidth = if (adaptiveInfo.isTabletLandscape) 860.dp else 760.dp
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        AboutItem(
                                icon = Icons.Default.Code,
                                title = stringResource(R.string.about_github),
                                subtitle = stringResource(R.string.about_github_desc),
                                onClick = soundClick {
                                    uriHandler.openUri("https://github.com/ciallothu/YanamiNext")
                                }
                        )
                    }
                    item {
                        AboutItem(
                                icon = Icons.Default.Info,
                                title = stringResource(R.string.about_version),
                                subtitle = state.currentVersionName
                        )
                    }
                    item {
                        AboutItemWithLoading(
                                icon = Icons.Default.SystemUpdateAlt,
                                title = stringResource(R.string.update_check),
                                subtitle = stringResource(R.string.update_check_desc),
                                isLoading = state.isCheckingUpdate,
                                onClick = soundClick { viewModel.onEvent(AboutEvent.CheckForUpdate) }
                        )
                    }
                }
            }
        }

        if (state.showUpdateDialog && state.updateInfo != null) {
            val info = state.updateInfo!!
            AlertDialog(
                    onDismissRequest = { viewModel.onEvent(AboutEvent.DismissUpdateDialog) },
                    title = { Text(stringResource(R.string.update_available_title)) },
                    text = {
                        Column {
                            Text(
                                    text = stringResource(R.string.update_new_version, info.versionName),
                                    style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                    text = stringResource(R.string.update_changelog),
                                    style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                    text = info.changelog,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.onEvent(AboutEvent.DismissUpdateDialog)
                            uriHandler.openUri(info.downloadUrl)
                        }) {
                            Text(stringResource(R.string.update_download))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onEvent(AboutEvent.DismissUpdateDialog) }) {
                            Text(stringResource(R.string.update_later))
                        }
                    }
            )
        }
    }
}

@Composable
private fun AboutItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        onClick: (() -> Unit)? = null
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .then(
                                    if (onClick != null) Modifier.clickable(onClick = onClick)
                                    else Modifier
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutItemWithLoading(
        icon: ImageVector,
        title: String,
        subtitle: String,
        isLoading: Boolean,
        onClick: () -> Unit
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(enabled = !isLoading, onClick = onClick)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                )
            }
        } else {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                    text = if (isLoading) stringResource(R.string.update_checking) else subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
