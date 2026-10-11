package com.xiaomanjun.sleepdownschedule.feature.experimental.colorosui

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import com.xiaomanjun.sleepdownschedule.BuildConfig
import java.lang.reflect.Proxy

/** Loads code and resources from the same installed system APK; no vendor code is bundled. */
internal object ColorOsNativeUi {
    const val Button = "com.coui.appcompat.button.COUIButton"
    const val FloatingNavigation =
        "com.coui.appcompat.bottomfloatingnavigationbar.COUIBottomFloatingNavigationBar"
    const val FloatingButton = "com.coui.appcompat.floatingactionbutton.COUIFloatingButton"
    private const val Tag = "SleepDownCOUI"
    private val sources = listOf("com.android.settings", "com.android.contacts", "com.coloros.filemanager")
    private var searched = false
    private var cachedSource: Source? = null

    @Synchronized
    fun source(context: Context): Source? {
        if (!BuildConfig.SLEEPDOWN_EXPERIMENTAL_FEATURES || Build.VERSION.SDK_INT < 31 ||
            Build.MANUFACTURER.lowercase() !in setOf("oppo", "oneplus", "realme")) return null
        if (searched) return cachedSource
        searched = true
        for (packageName in sources) {
            val candidate = prepare("load $packageName") {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                check(info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                val native = context.applicationContext.createPackageContext(
                    packageName, Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
                )
                val source = Source(packageName, native)
                // This first wave needs the ColorOS 17 floating bar and button material APIs.
                source.load(FloatingNavigation)
                source.load("$Button\$MaterialEffectState")
                check(source.resource("Theme.COUI.Main", "style") != 0)
                check(source.resource("Theme.COUI.Main.Dark", "style") != 0)
                source
            }
            if (candidate != null) {
                cachedSource = candidate
                Log.i(Tag, "Native home components loaded from $packageName")
                return candidate
            }
        }
        return null
    }

    fun <T> prepare(operation: String, block: () -> T): T? = try {
        block()
    } catch (error: Exception) {
        Log.w(Tag, "Cannot $operation; keeping the existing home component", error)
        null
    } catch (error: LinkageError) {
        Log.w(Tag, "Cannot $operation; incompatible installed COUI classes", error)
        null
    }

    class Source(val packageName: String, private val native: Context) {
        fun load(name: String): Class<*> = native.classLoader.loadClass(name)
        fun resource(name: String, type: String): Int = native.resources.getIdentifier(name, type, packageName)

        fun themedContext(host: Context, dark: Boolean): Context {
            val configuration = Configuration(host.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val configured = native.createConfigurationContext(configuration)
            val application = object : ContextWrapper(configured) {
                override fun getApplicationContext(): Context = this
                override fun getClassLoader(): ClassLoader = native.classLoader
            }
            val resources = object : ContextWrapper(host) {
                override fun getResources() = configured.resources
                override fun getAssets() = configured.assets
                override fun getClassLoader(): ClassLoader = native.classLoader
                override fun getApplicationContext(): Context = application
                override fun getSystemService(name: String): Any? =
                    if (name == Context.LAYOUT_INFLATER_SERVICE) LayoutInflater.from(host).cloneInContext(this)
                    else super.getSystemService(name)
            }
            val theme = resource(if (dark) "Theme.COUI.Main.Dark" else "Theme.COUI.Main", "style")
            return ContextThemeWrapper(resources, theme)
        }

        fun material(view: View, enumName: String) {
            val state = load(enumName)
            view.javaClass.getMethod("setUseMaterialEffectAttr", state)
                .invoke(view, state.getField("FOLLOW_SYSTEM").get(null))
        }

        fun listener(typeName: String, callback: (String, Array<out Any?>) -> Any?): Any {
            val type = load(typeName)
            return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, arguments ->
                when (method.name) {
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === arguments?.firstOrNull()
                    "toString" -> "SleepDown ${type.simpleName}"
                    else -> callback(method.name, arguments ?: emptyArray())
                }
            }
        }
    }
}
