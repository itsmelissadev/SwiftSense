package io.github.itsmelissadev.swiftsense

import android.app.Application
import io.github.itsmelissadev.swiftsense.service.adb.AdbManager
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge

class SwiftSenseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShellBridge.init(this)
        AdbManager.init(this)
    }
}
