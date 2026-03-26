package com.skothr.ephemeris

import android.app.Application
import com.skothr.ephemeris.ephemeris.SwissEphemeris
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class EphemerisApp : Application() {

    val swissEphemeris = SwissEphemeris()
    val ephemerisReady = CompletableDeferred<Unit>()

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            val ephePath = extractEphemerisData()
            swissEphemeris.init(ephePath)
            ephemerisReady.complete(Unit)
        }
    }

    private fun extractEphemerisData(): String {
        val epheDir = File(filesDir, "ephe")
        if (!epheDir.exists()) {
            epheDir.mkdirs()
            assets.list("ephe")?.forEach { filename ->
                assets.open("ephe/$filename").use { input ->
                    File(epheDir, filename).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        return epheDir.absolutePath
    }
}
