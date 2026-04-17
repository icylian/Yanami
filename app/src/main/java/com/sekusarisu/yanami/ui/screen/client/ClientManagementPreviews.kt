package com.sekusarisu.yanami.ui.screen.client

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.theme.ThemeColor
import com.sekusarisu.yanami.ui.theme.YanamiTheme

@Preview(showBackground = true)
@Composable
private fun ClientCardPreviewUnmasked() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        ClientCard(
                client = previewManagedClient(),
                maskIpAddress = false,
                canMoveUp = true,
                canMoveDown = true,
                onShowInstallCommand = {},
                onOpenTerminal = {},
                onEdit = {},
                onDelete = {},
                onMoveUp = {},
                onMoveDown = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClientCardPreviewMasked() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        ClientCard(
                client = previewManagedClient(hidden = true),
                maskIpAddress = true,
                canMoveUp = false,
                canMoveDown = true,
                onShowInstallCommand = {},
                onOpenTerminal = {},
                onEdit = {},
                onDelete = {},
                onMoveUp = {},
                onMoveDown = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SortModeClientCardPreview() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        SortModeClientCard(client = previewManagedClient(hidden = true), isDragging = false)
    }
}

@Preview(name = "Install Command Dialog", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun InstallCommandDialogPreview() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        InstallCommandDialog(
                client = previewManagedClient(),
                serverBaseUrl = "https://komari.example.com/admin",
                onDismiss = {}
        )
    }
}

@Preview(name = "Client List", showBackground = true, widthDp = 1280, heightDp = 900)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientManagementContentPreview() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        Scaffold(
                topBar = {
                    TopAppBar(
                            title = { Text("Komari Admin") },
                            navigationIcon = {
                                IconButton(onClick = {}) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {}) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                }
                            }
                    )
                }
        ) { innerPadding ->
            AdaptiveContentPane(modifier = Modifier.padding(innerPadding), maxWidth = 920.dp) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            label = { Text("搜索客户端…") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已显示 3 / 3")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("IP 打码")
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(checked = true, onCheckedChange = {})
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(selected = true, onClick = {}, label = { Text("全部") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = false, onClick = {}, label = { Text("ISIF") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = false, onClick = {}, label = { Text("PROD") })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(previewManagedClients()) { index, client ->
                            ClientCard(
                                    client = client,
                                    maskIpAddress = true,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < previewManagedClients().lastIndex,
                                    onShowInstallCommand = {},
                                    onOpenTerminal = {},
                                    onEdit = {},
                                    onDelete = {},
                                    onMoveUp = {},
                                    onMoveDown = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Client List Sort Mode", showBackground = true, widthDp = 1280, heightDp = 900)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientManagementSortModePreview() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        Scaffold(
                topBar = {
                    TopAppBar(
                            title = { Text("Komari Admin") },
                            navigationIcon = {
                                IconButton(onClick = {}) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {}) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                }
                            }
                    )
                }
        ) { innerPadding ->
            AdaptiveContentPane(modifier = Modifier.padding(innerPadding), maxWidth = 920.dp) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            enabled = false,
                            label = { Text("搜索客户端…") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已显示 3 / 3")
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("IP 打码")
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(checked = false, onCheckedChange = {}, enabled = false)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("排序模式")
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(checked = true, onCheckedChange = {}, enabled = false)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(selected = true, onClick = {}, enabled = false, label = { Text("全部") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("ISIF") })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                            text = "长按卡片可拖拽排序",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(previewManagedClients()) { _, client ->
                            SortModeClientCard(client = client, isDragging = false)
                        }
                    }
                }
            }
        }
    }
}

private fun previewManagedClients(): List<ManagedClient> =
        listOf(
                previewManagedClient(),
                previewManagedClient(
                        uuid = "11111111-2222-4333-8444-555555555555",
                        name = "DEMO_SG_EDGE",
                        ipv4 = "198.51.100.24",
                        ipv6 = "",
                        version = "1.2.0-preview",
                        price = 19.9,
                        billingCycle = 30,
                        expiredAt = "2026-09-15T00:00:00Z",
                        hidden = false,
                        updatedAt = "2026-04-01T08:12:00Z"
                ),
                previewManagedClient(
                        uuid = "66666666-7777-4888-9999-aaaaaaaaaaaa",
                        name = "DEMO_JP_CORE",
                        ipv4 = "203.0.113.88",
                        ipv6 = "2001:db8:abcd:1200::42",
                        version = "1.2.1",
                        price = 29.0,
                        billingCycle = 90,
                        expiredAt = "2026-12-01T00:00:00Z",
                        hidden = true,
                        updatedAt = "2026-04-03T21:45:00Z"
                )
        )

private fun previewManagedClient(
        uuid: String = "d3b07384-d9a2-4f1a-a6ab-7c8d9e0f1122",
        name: String = "DEMO_HK_NODE_A",
        ipv4: String = "192.168.242.15",
        ipv6: String = "2001:db8:10:20::15",
        version: String = "1.2.0",
        price: Double = 12.5,
        billingCycle: Int = 30,
        expiredAt: String? = "2026-08-05T00:00:00Z",
        hidden: Boolean = false,
        updatedAt: String = "2026-04-05T09:21:50Z"
): ManagedClient =
        ManagedClient(
                uuid = uuid,
                token = "demo_token_preview_01",
                name = name,
                cpuName = "Virtual CPU",
                virtualization = "kvm",
                arch = "amd64",
                cpuCores = 2,
                os = "Debian GNU/Linux 13",
                kernelVersion = "6.12.0-demo-cloud-amd64",
                gpuName = "None",
                ipv4 = ipv4,
                ipv6 = ipv6,
                region = "Demo",
                remark = "",
                publicRemark = "",
                memTotal = 2147483648,
                swapTotal = 0,
                diskTotal = 53687091200,
                version = version,
                weight = 5,
                price = price,
                billingCycle = billingCycle,
                autoRenewal = false,
                currency = "$",
                expiredAt = expiredAt,
                group = "DEMO",
                tags = "",
                hidden = hidden,
                trafficLimit = 0,
                trafficLimitType = "max",
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = updatedAt
        )
