package com.sekusarisu.yanami.di

import androidx.room.Room
import com.sekusarisu.yanami.BuildConfig
import com.sekusarisu.yanami.data.backup.ConfigBackupManager
import com.sekusarisu.yanami.data.local.MIGRATION_2_3
import com.sekusarisu.yanami.data.local.MIGRATION_3_4
import com.sekusarisu.yanami.data.local.YanamiDatabase
import com.sekusarisu.yanami.data.local.crypto.CryptoManager
import com.sekusarisu.yanami.data.local.preferences.UserPreferencesRepository
import com.sekusarisu.yanami.data.remote.KomariAdminClientService
import com.sekusarisu.yanami.data.remote.KomariAdminPingService
import com.sekusarisu.yanami.data.remote.KomariAuthService
import com.sekusarisu.yanami.data.remote.KomariRpcService
import com.sekusarisu.yanami.data.remote.SessionCookieInterceptor
import com.sekusarisu.yanami.data.remote.SessionManager
import com.sekusarisu.yanami.data.remote.UpdateCheckService
import com.sekusarisu.yanami.data.repository.ClientRepositoryImpl
import com.sekusarisu.yanami.data.repository.NodeRepositoryImpl
import com.sekusarisu.yanami.data.repository.PingTaskRepositoryImpl
import com.sekusarisu.yanami.data.repository.ServerRepositoryImpl
import com.sekusarisu.yanami.domain.repository.ClientRepository
import com.sekusarisu.yanami.domain.repository.NodeRepository
import com.sekusarisu.yanami.domain.repository.PingTaskRepository
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.ui.screen.client.ClientCreateViewModel
import com.sekusarisu.yanami.ui.screen.client.ClientEditViewModel
import com.sekusarisu.yanami.ui.screen.client.ClientManagementViewModel
import com.sekusarisu.yanami.ui.screen.client.PingTaskManagementViewModel
import com.sekusarisu.yanami.ui.screen.nodedetail.NodeDetailViewModel
import com.sekusarisu.yanami.ui.screen.nodelist.NodeListViewModel
import com.sekusarisu.yanami.ui.screen.server.AddServerViewModel
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginViewModel
import com.sekusarisu.yanami.ui.screen.server.ServerListViewModel
import com.sekusarisu.yanami.ui.screen.settings.AboutViewModel
import com.sekusarisu.yanami.ui.screen.settings.SettingsViewModel
import com.sekusarisu.yanami.ui.screen.terminal.SshTerminalViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

    // ─── Session ───
    single { SessionManager() }

    // ─── Network ───
    single {
        val sessionManager = get<SessionManager>()
        HttpClient(OkHttp) {
            engine { addInterceptor(SessionCookieInterceptor(sessionManager)) }
            install(ContentNegotiation) {
                json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            prettyPrint = false
                        }
                )
            }
            install(WebSockets)
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            install(Logging) {
                level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
            }
        }
    }

    // ─── Database ───
    single {
        Room.databaseBuilder(androidContext(), YanamiDatabase::class.java, "yanami_database")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }

    single { get<YanamiDatabase>().serverInstanceDao() }

    // ─── Crypto ───
    single { CryptoManager() }

    // ─── Preferences ───
    single { UserPreferencesRepository(androidContext()) }
    single { ConfigBackupManager(androidContext(), get(), get()) }

    // ─── Remote Service ───
    single { KomariAuthService(get()) }
    single { KomariRpcService(get(), get()) }
    single { KomariAdminClientService(get()) }
    single { KomariAdminPingService(get()) }
    single { UpdateCheckService() }

    // ─── Repository ───
    single<ServerRepository> {
        ServerRepositoryImpl(
                dao = get(),
                cryptoManager = get(),
                authService = get(),
                rpcService = get(),
                sessionManager = get()
        )
    }
    single<NodeRepository> { NodeRepositoryImpl(rpcService = get()) }
    single<ClientRepository> { ClientRepositoryImpl(service = get()) }
    single<PingTaskRepository> { PingTaskRepositoryImpl(service = get()) }

    // ─── ScreenModels (Voyager) ───
    factory { ServerListViewModel(get(), androidContext()) }
    factory { (editServerId: Long?) -> AddServerViewModel(editServerId, get(), androidContext()) }
    factory { (serverId: Long, forceTwoFa: Boolean) ->
        ServerReLoginViewModel(serverId, forceTwoFa, get(), androidContext())
    }
    factory { NodeListViewModel(get(), get(), androidContext()) }
    factory { ClientManagementViewModel(get(), get(), androidContext()) }
    factory { PingTaskManagementViewModel(get(), get(), get(), androidContext()) }
    factory { ClientCreateViewModel(get(), get(), androidContext()) }
    factory { (uuid: String) -> ClientEditViewModel(uuid, get(), get(), androidContext()) }
    factory { SettingsViewModel(get(), get(), androidContext()) }
    factory { AboutViewModel(get(), androidContext()) }
    factory { (uuid: String) -> NodeDetailViewModel(uuid, get(), get(), get(), androidContext()) }
    factory { (uuid: String) -> SshTerminalViewModel(uuid, get(), get(), get(), get()) }
}
