package com.osprey74.michinavi

import android.app.Application
import com.osprey74.michinavi.service.ServiceLocator

class MichiNaviApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
