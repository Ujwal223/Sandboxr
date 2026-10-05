/*
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.android.launcher3.util

import android.view.MotionEvent

object TouchUtil {
    @JvmStatic
    fun isMouseRightClickDownOrMove(event: MotionEvent): Boolean =
        com.sandboxr.launcher.util.TouchUtil.isMouseRightClickDownOrMove(event)
}
