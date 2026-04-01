package com.osprey74.michinavi.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.R as CarAppR
import androidx.car.app.validation.HostValidator

class MichiNaviCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator =
        HostValidator.Builder(applicationContext)
            .addAllowedHosts(CarAppR.array.hosts_allowlist_sample)
            .build()

    override fun onCreateSession(): Session = MichiNaviCarSession()
}
