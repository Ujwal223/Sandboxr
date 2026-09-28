/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.sandboxr.launcher.compat

import android.content.Context
import android.icu.text.AlphabeticIndex
import android.os.LocaleList
import java.util.Locale

/**
 * Compatibility wrapper around ICU AlphabeticIndex to generate section headers
 * for the All Apps alphabetical drawer.
 */
class AlphabeticIndexCompat(locales: LocaleList) {

    private val baseIndex: AlphabeticIndex.ImmutableIndex<String>
    private val defaultMiscLabel: String

    constructor(context: Context) : this(context.resources.configuration.locales)

    init {
        val localeCount = locales.size()
        val primaryLocale = if (localeCount == 0) Locale.ENGLISH else locales.get(0)
        val indexBuilder = AlphabeticIndex<String>(primaryLocale)
        for (i in 1 until localeCount) {
            indexBuilder.addLabels(locales.get(i))
        }
        indexBuilder.addLabels(Locale.ENGLISH)
        baseIndex = indexBuilder.buildImmutableIndex()

        defaultMiscLabel = if (primaryLocale.language == Locale.JAPANESE.language) {
            "\u4ed6" // Japanese "misc"
        } else {
            MID_DOT
        }
    }

    /**
     * Computes the section label for an input title string (e.g. "A", "B", "#", "∙").
     */
    fun computeSectionName(cs: CharSequence): String {
        val s = cs.trim().toString()
        if (s.isEmpty()) return defaultMiscLabel

        val c = s.codePointAt(0)
        if (Character.isDigit(c)) {
            return "#"
        }

        val bucketIndex = baseIndex.getBucketIndex(s)
        val sectionName = baseIndex.getBucket(bucketIndex).label

        if (sectionName.trim().isEmpty()) {
            return if (Character.isLetter(c)) {
                defaultMiscLabel
            } else {
                MID_DOT
            }
        }
        return sectionName
    }

    companion object {
        const val MID_DOT = "\u2219"
    }
}
