package com.xiaomanjun.sleepdownschedule.feature.experimental.colorosui

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import com.xiaomanjun.sleepdownschedule.BuildConfig
import java.lang.reflect.Proxy

/** Loads code and resources from the same installed system APK; no vendor code is bundled. */
internal object ColorOsNativeUi {
    const val ToolbarMaterial = "com.coui.appcompat.toolbar.ToolbarMaterialEffectDelegate"
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
                source.load(ToolbarMaterial).getMethod("forMenuItem", View::class.java)
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
        private val materialManagerType by lazy { load("com.coui.appcompat.COUIMaterialStateManager") }
        private val materialManager by lazy { materialManagerType.getField("INSTANCE").get(null) }
        private var materialObserverRegistered = false

        fun load(name: String): Class<*> = native.classLoader.loadClass(name)
        fun resource(name: String, type: String): Int = native.resources.getIdentifier(name, type, packageName)

        fun color(context: Context, attribute: String): ColorStateList {
            val value = TypedValue()
            check(context.theme.resolveAttribute(resource(attribute, "attr"), value, true))
            return if (value.resourceId != 0) context.getColorStateList(value.resourceId)
                else ColorStateList.valueOf(value.data)
        }

        fun tintSpotLight(owner: Any, declaringClass: String, fieldName: String, accent: Int) {
            // COUI exposes the light's color setter, but no getter on these two hosts.
            // Read the existing drawable; do not replace its touch handling or animation.
            val field = load(declaringClass).getDeclaredField(fieldName).apply { isAccessible = true }
            val light = field.get(owner) ?: return
            light.javaClass.getMethod("setLightColor", Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType, Float::class.javaPrimitiveType).invoke(light,
                // A saturated accent absorbs most of the native white light. Keep its hue,
                // but lift the light towards white without changing the system's touch springs.
                0.55f + 0.45f * Color.red(accent) / 255f,
                0.55f + 0.45f * Color.green(accent) / 255f,
                0.55f + 0.45f * Color.blue(accent) / 255f)
        }

        fun themedContext(host: Context, dark: Boolean): Context {
            return NativeThemeContext(host, dark).also { configureMaterial(it) }
        }

        /** Update resources and theme in place: bound native views and their springs stay alive. */
        fun updateTheme(context: Context, dark: Boolean): Boolean =
            (context as NativeThemeContext).update(dark)

        private inner class NativeThemeContext(host: Context, private var dark: Boolean,
            private val configurationOverride: Configuration? = null) : ContextWrapper(host) {
            private fun configuredContext(dark: Boolean): Context {
                val configuration = Configuration(configurationOverride ?: baseContext.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                return native.createConfigurationContext(configuration)
            }

            private fun styledContext(configured: Context, dark: Boolean): Context = ContextThemeWrapper(
                configured, resource(if (dark) "Theme.COUI.Main.Dark" else "Theme.COUI.Main", "style"))

            private var configured = configuredContext(dark)
            private var styled = styledContext(configured, dark)
            // Keep the host's UID/package attribution for Settings and feature-provider access.
            // This separate wrapper never captures this Activity-backed theme context.
            private val application = nativeApplicationContext(host.applicationContext, configured)

            fun update(wantedDark: Boolean): Boolean {
                if (dark == wantedDark) return false
                dark = wantedDark
                configured = configuredContext(dark)
                styled = styledContext(configured, dark)
                return true
            }

            override fun getResources() = configured.resources
            override fun getAssets() = configured.assets
            override fun getTheme() = styled.theme
            override fun setTheme(resId: Int) { styled.setTheme(resId) }
            override fun getClassLoader(): ClassLoader = native.classLoader
            override fun getApplicationContext(): Context = application
            // COUI's FAB asks a configuration context for vendor dimension resources when refreshed.
            override fun createConfigurationContext(overrideConfiguration: Configuration): Context =
                NativeThemeContext(baseContext,
                    overrideConfiguration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
                    Configuration(overrideConfiguration))
            override fun getSystemService(name: String): Any? =
                if (name == Context.LAYOUT_INFLATER_SERVICE) LayoutInflater.from(baseContext).cloneInContext(this)
                else super.getSystemService(name)
        }

        private fun nativeApplicationContext(host: Context, configured: Context): Context =
            object : ContextWrapper(host) {
                override fun getResources() = configured.resources
                override fun getAssets() = configured.assets
                override fun getApplicationContext(): Context = this
                override fun getClassLoader(): ClassLoader = native.classLoader
            }

        private fun configureMaterial(context: Context) {
            if (!materialObserverRegistered) {
                val flags = listOf("FLAG_MATERIAL_STROKE", "FLAG_MATERIAL_BLUR",
                    "FLAG_MATERIAL_SPOTLIGHT", "FLAG_MATERIAL_CAUSTIC_SHADOW")
                    .fold(0) { value, name -> value or materialManagerType.getField(name).getInt(null) }
                // FOLLOW_SYSTEM reads these initially-false states; views only register listeners.
                // The observer lives with this process-wide source and retains an application context.
                materialManagerType.getMethod("registerMaterialStateObserver", Context::class.java,
                    Int::class.javaPrimitiveType).invoke(materialManager, context, flags)
                materialObserverRegistered = true
            } else {
                materialManagerType.getMethod("onConfigurationChange", Context::class.java)
                    .invoke(materialManager, context)
            }
            val states = listOf("materialVersionCheck", "isFeatureEnabled", "isBlurEffectEnable",
                "isStrokeEffectEnable", "isSpotLightEffectEnable", "isCausticShadowEffectEnable")
                .joinToString { name -> "$name=${materialManagerType.getMethod(name).invoke(materialManager)}" }
            Log.i(Tag, "Native material state: $states")
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
