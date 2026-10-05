/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.dragndrop

import android.net.Uri
import android.view.DragAndDropPermissions
import com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_SYSTEM_DRAG
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo

/**
 * Item info placeholder for system-level drag and drop payloads.
 */
class SystemDragItemInfo : WorkspaceItemInfo() {

    var payload: Payload
        get() = payloadBackingField.value
        set(value) {
            payloadBackingField.value = value
        }

    private var payloadBackingField = Reference<Payload>(EmptyPayload)

    init {
        itemType = ITEM_TYPE_SYSTEM_DRAG
    }

    override fun clone(): WorkspaceItemInfo =
        SystemDragItemInfo().apply { copyFrom(this@SystemDragItemInfo) }

    override fun copyFrom(itemInfo: ItemInfo): Unit =
        super.copyFrom(itemInfo).apply {
            payloadBackingField =
                (itemInfo as? SystemDragItemInfo)?.payloadBackingField?.copy()
                    ?: Reference(EmptyPayload)
        }

    override fun makeShallowCopy(): ItemInfo =
        SystemDragItemInfo().apply {
            copyFrom(this@SystemDragItemInfo)
            payloadBackingField = this@SystemDragItemInfo.payloadBackingField
        }

    sealed class Payload {
        abstract fun isAcceptable(): Boolean
    }

    data object EmptyPayload : Payload() {
        override fun isAcceptable(): Boolean = false
    }

    data class UriListPayload(val permissions: DragAndDropPermissions?, val uriList: List<Uri>?) :
        Payload() {
        override fun isAcceptable(): Boolean = permissions != null && uriList?.isNotEmpty() == true
    }

    private data class Reference<T>(var value: T)
}
