package net.rpcs3

import android.content.res.Resources.NotFoundException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File


enum class FirmwareStatus {
    None,
    Installed,
    Compiled
}

@Serializable
private data class FirmwareInfo(val version: String?, val status: FirmwareStatus);

class FirmwareRepository {
    companion object {
        val progressChannel: MutableState<Long?> = mutableStateOf(null)
        val version: MutableState<String?> = mutableStateOf(null)
        val status: MutableState<FirmwareStatus> = mutableStateOf(FirmwareStatus.None)

        fun save() {
                try {
                    File(RPCS3.rootDirectory + "fw.json").writeText(
                        Json.encodeToString(
                            FirmwareInfo(version.value, status.value)
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()

            }
        }

        fun load() {
                try {
                    val info =
                        Json.decodeFromString<FirmwareInfo>(File(RPCS3.rootDirectory + "fw.json").readText())
                    status.value = info.status
                    version.value = info.version
                } catch (_: NotFoundException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
        }

        @Keep
        @JvmStatic
        fun onFirmwareInstalled(version: String?) {
            updateStatus(version, FirmwareStatus.Installed)
        }

        @Keep
        @JvmStatic
        fun onFirmwareCompiled(version: String?) {
            updateStatus(version, FirmwareStatus.Compiled)
        }

        fun updateStatus(version: String?, status: FirmwareStatus) {
            synchronized(Companion.version) {
                Companion.version.value = version
                Companion.status.value = status

                save()
            }
        }
    }
}
