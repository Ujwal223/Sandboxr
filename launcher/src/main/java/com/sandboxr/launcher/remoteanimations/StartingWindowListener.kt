/*
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher.remoteanimations

import android.graphics.Color

/**
 * Listener that tracks starting window theme attributes and splash screen states
 * for remote app launch animations.
 */
class StartingWindowListener {

    var backgroundColor: Int = Color.TRANSPARENT
    var hasSplashScreen: Boolean = false
    var hasDifferentAppIcon: Boolean = false

    fun onStartingWindowDrawn(bgColor: Int, splashScreen: Boolean, differentAppIcon: Boolean = false) {
        this.backgroundColor = bgColor
        this.hasSplashScreen = splashScreen
        this.hasDifferentAppIcon = differentAppIcon
    }

    fun reset() {
        backgroundColor = Color.TRANSPARENT
        hasSplashScreen = false
        hasDifferentAppIcon = false
    }
}
