package com.flashlearn.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. @HiltAndroidApp triggers Hilt's code generation
 * and creates the top-level application-scoped dependency container.
 *
 * No other logic belongs here — all business logic lives in domain/data.
 */
@HiltAndroidApp
class FlashLearnApplication : Application()
