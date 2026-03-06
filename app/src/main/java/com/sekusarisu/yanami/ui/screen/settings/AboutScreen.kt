package com.sekusarisu.yanami.ui.screen.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.soundClick

class AboutScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current
        val context = LocalContext.current
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
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
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                item {
                    AboutItem(
                            icon = Icons.Default.Code,
                            title = stringResource(R.string.about_github),
                            subtitle = stringResource(R.string.about_github_desc),
                            onClick = soundClick {
                                uriHandler.openUri("https://github.com/icylian/Yanami")
                            }
                    )
                }
                item {
                    AboutItem(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.about_version),
                            subtitle = versionName
                    )
                }
            }
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
