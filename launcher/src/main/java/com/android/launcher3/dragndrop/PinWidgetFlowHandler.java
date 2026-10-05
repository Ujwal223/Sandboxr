/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.dragndrop;

import android.content.Context;
import android.content.pm.LauncherApps.PinItemRequest;
import com.sandboxr.launcher.widget.LauncherAppWidgetProviderInfo;

/**
 * Compatibility shim for PinWidgetFlowHandler.
 */
public class PinWidgetFlowHandler extends com.sandboxr.launcher.dragndrop.PinWidgetFlowHandler {
    public PinWidgetFlowHandler(PinItemRequest request, Context context) {
        super(request, context);
    }

    public PinWidgetFlowHandler(LauncherAppWidgetProviderInfo providerInfo) {
        super(providerInfo);
    }
}
