package com.osprey74.michinavi.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class MichiNaviCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = MichiNaviCarSession()
}
