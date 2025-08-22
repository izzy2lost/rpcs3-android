package net.rpcs3

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.rpcs3.ui.navigation.AppNavHost
import kotlin.concurrent.thread

private const val ACTION_USB_PERMISSION = "net.rpcs3.USB_PERMISSION"

class MainActivity : ComponentActivity() {
    private lateinit var unregisterUsbEventListener: () -> Unit
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            RPCS3Theme {
                AppNavHost()
            }
        }

        RPCS3.rootDirectory = applicationContext.getExternalFilesDir(null).toString()
        if (!RPCS3.rootDirectory.endsWith("/")) {
            RPCS3.rootDirectory += "/"
        }

        if (!RPCS3.initialized) {
            lifecycleScope.launch { GameRepository.load() }
            FirmwareRepository.load()
        }

        Permission.PostNotifications.requestPermission(this)

        with(getSystemService(NOTIFICATION_SERVICE) as NotificationManager) {
            val channel = NotificationChannel(
                "rpcs3-progress",
                "Installation progress",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            createNotificationChannel(channel)
        }

        if (!RPCS3.initialized) {
            RPCS3.instance.initialize(RPCS3.rootDirectory)
            val nativeLibraryDir = packageManager.getApplicationInfo(packageName, 0).nativeLibraryDir
            RPCS3.instance.settingsSet("Video@@Vulkan@@Custom Driver@@Hook Directory", "\"" + nativeLibraryDir + "\"")
            RPCS3.initialized = true

            thread {
                RPCS3.instance.startMainThreadProcessor()
            }

            thread {
                RPCS3.instance.processCompilationQueue()
            }
        }

        unregisterUsbEventListener = listenUsbEvents(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterUsbEventListener()
    }
}
