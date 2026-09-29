/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.sandboxr.launcher.util

import java.text.Collator
import java.util.Comparator

/**
 * Extension of [java.text.Collator] with special handling for digits.
 * Used for comparing user-visible labels in alphabetical order.
 */
class LabelComparator : Comparator<String> {

    private val collator: Collator = Collator.getInstance().apply {
        strength = Collator.PRIMARY
    }

    override fun compare(titleA: String?, titleB: String?): Int {
        val a = titleA ?: ""
        val b = titleB ?: ""

        // Ensure that we prioritize titles starting with letter or digit over symbols
        val aStartsWithLetter = a.isNotEmpty() && Character.isLetterOrDigit(a.codePointAt(0))
        val bStartsWithLetter = b.isNotEmpty() && Character.isLetterOrDigit(b.codePointAt(0))

        if (aStartsWithLetter && !bStartsWithLetter) {
            return -1
        } else if (!aStartsWithLetter && bStartsWithLetter) {
            return 1
        }

        // Order by the title in the current locale
        return collator.compare(a, b)
    }
}
