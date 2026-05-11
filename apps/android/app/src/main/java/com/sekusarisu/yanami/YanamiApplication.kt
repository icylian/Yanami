package com.sekusarisu.yanami

import android.app.Application
import com.sekusarisu.yanami.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Yanami Application
 *
 * 负责初始化 Koin 依赖注入框架。 在 AndroidManifest.xml 中通过 android:name 引用。
 */
class YanamiApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@YanamiApplication)
            modules(appModule)
        }
    }
}
