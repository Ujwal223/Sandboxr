/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.sandboxr.launcher.search

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.sandboxr.launcher.util.IntArray
import java.text.Collator
import java.util.Locale

/**
 * High-performance string matching utility supporting prefix matching, word boundary break detection,
 * fuzzy subsequence searching, scoring, and Spannable character match highlighting.
 */
object StringMatcherUtility {

    private const val SPACE = ' '

    /**
     * Returns true if [query] is a prefix of [target] or matches across word boundaries.
     */
    @JvmStatic
    fun matches(query: String, target: String, matcher: StringMatcher = StringMatcher.getInstance()): Boolean {
        val queryLength = query.length
        val targetLength = target.length

        if (targetLength < queryLength || queryLength <= 0) {
            return false
        }

        if (requestSimpleFuzzySearch(query)) {
            return target.contains(query, ignoreCase = true)
        }

        var lastType: Int
        var thisType: Int = Character.UNASSIGNED.toInt()
        var nextType: Int = Character.getType(target.codePointAt(0))

        val end = targetLength - queryLength
        for (i in 0..end) {
            lastType = thisType
            thisType = nextType
            nextType = if (i < targetLength - 1) {
                Character.getType(target.codePointAt(i + 1))
            } else {
                Character.UNASSIGNED.toInt()
            }

            if (matcher.isBreak(thisType, lastType, nextType) &&
                matcher.matches(query, target.substring(i, i + queryLength))
            ) {
                return true
            }
        }

        // Fallback to fuzzy subsequence match
        return fuzzyScore(query, target) > 0
    }

    /**
     * Calculates a matching relevance score between 0 and 2000+.
     * Returns -1 if no match.
     */
    @JvmStatic
    fun fuzzyScore(query: String, target: String): Int {
        if (query.isEmpty() || target.isEmpty()) return -1
        val q = query.lowercase(Locale.ROOT)
        val t = target.lowercase(Locale.ROOT)

        if (t == q) return 2000
        if (t.startsWith(q)) return 1600 - (t.length - q.length)

        // Check word prefix matches (e.g. "ca" in "Google Calendar" or "gm" in "Gmail")
        val words = t.split(Regex("[\\s_\\-\\.]+"))
        for ((idx, word) in words.withIndex()) {
            if (word.startsWith(q)) {
                return 1200 - (idx * 50) - (word.length - q.length)
            }
        }

        // Check acronym / camelCase matching (e.g. "ps" matches "Play Store")
        val acronym = StringBuilder()
        for (word in words) {
            if (word.isNotEmpty()) acronym.append(word[0])
        }
        if (acronym.toString().startsWith(q)) {
            return 900
        }

        // Substring match
        val subIndex = t.indexOf(q)
        if (subIndex >= 0) {
            return 800 - subIndex
        }

        // Subsequence match
        var qIdx = 0
        var tIdx = 0
        var consecutive = 0
        var totalScore = 0

        while (qIdx < q.length && tIdx < t.length) {
            if (q[qIdx] == t[tIdx]) {
                totalScore += 20 + (consecutive * 15)
                consecutive++
                qIdx++
            } else {
                consecutive = 0
            }
            tIdx++
        }

        return if (qIdx == q.length) 200 + totalScore else -1
    }

    /**
     * Finds character index ranges in [target] that match [query].
     */
    @JvmStatic
    fun getMatchedIndices(query: String, target: String): List<Int> {
        if (query.isEmpty() || target.isEmpty()) return emptyList()
        val q = query.lowercase(Locale.ROOT)
        val t = target.lowercase(Locale.ROOT)

        // 1. Direct substring match
        val subIndex = t.indexOf(q)
        if (subIndex >= 0) {
            return (subIndex until subIndex + q.length).toList()
        }

        // 2. Word prefix match
        var searchPos = 0
        for (word in target.split(Regex("[\\s_\\-\\.]+"))) {
            val wordStart = t.indexOf(word.lowercase(Locale.ROOT), searchPos)
            if (wordStart >= 0) {
                if (word.lowercase(Locale.ROOT).startsWith(q)) {
                    return (wordStart until wordStart + q.length).toList()
                }
                searchPos = wordStart + word.length
            }
        }

        // 3. Subsequence match
        val indices = ArrayList<Int>()
        var qIdx = 0
        var tIdx = 0
        while (qIdx < q.length && tIdx < t.length) {
            if (q[qIdx] == t[tIdx]) {
                indices.add(tIdx)
                qIdx++
            }
            tIdx++
        }
        return if (qIdx == q.length) indices else emptyList()
    }

    /**
     * Highlights matching characters in [title] using Liquid Glass accent color and bold font style.
     */
    @JvmStatic
    fun highlightMatches(
        title: CharSequence,
        query: String,
        highlightColor: Int = 0xFF4E80EE.toInt()
    ): CharSequence {
        if (query.trim().isEmpty() || title.isEmpty()) return title
        val matchedIndices = getMatchedIndices(query.trim(), title.toString())
        if (matchedIndices.isEmpty()) return title

        val spannable = SpannableString(title)

        // Group consecutive indices into spans to reduce span objects
        var start = -1
        var prev = -1

        for (idx in matchedIndices) {
            if (start == -1) {
                start = idx
                prev = idx
            } else if (idx == prev + 1) {
                prev = idx
            } else {
                applySpan(spannable, start, prev + 1, highlightColor)
                start = idx
                prev = idx
            }
        }
        if (start != -1) {
            applySpan(spannable, start, prev + 1, highlightColor)
        }

        return spannable
    }

    private fun applySpan(spannable: SpannableString, start: Int, end: Int, color: Int) {
        if (start in 0 until end && end <= spannable.length) {
            spannable.setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * Returns a list of breakpoints wherever the string contains a break.
     */
    @JvmStatic
    fun getListOfBreakpoints(input: CharSequence, matcher: StringMatcher): IntArray {
        val inputLength = input.length
        if (inputLength <= 2 || TextUtils.indexOf(input, SPACE) != -1) {
            val list = IntArray()
            for (i in 0 until inputLength) {
                if (input[i] == SPACE) {
                    list.add(i - 1)
                }
            }
            return list
        }

        val listOfBreakPoints = IntArray()
        var prevType: Int
        var thisType: Int = Character.getType(Character.codePointAt(input, 0))
        var nextType: Int = Character.getType(Character.codePointAt(input, 1))

        for (i in 1 until inputLength) {
            prevType = thisType
            thisType = nextType
            nextType = if (i < inputLength - 1) {
                Character.getType(Character.codePointAt(input, i + 1))
            } else {
                Character.UNASSIGNED.toInt()
            }
            if (matcher.isBreak(thisType, prevType, nextType)) {
                listOfBreakPoints.add(i - 1)
            }
        }
        return listOfBreakPoints
    }

    private fun requestSimpleFuzzySearch(s: String): Boolean {
        var i = 0
        while (i < s.length) {
            val codepoint = s.codePointAt(i)
            i += Character.charCount(codepoint)
            if (Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN) {
                return true
            }
        }
        return false
    }

    /**
     * Performs locale-sensitive string comparison using [Collator].
     */
    open class StringMatcher {
        private val collator: Collator = Collator.getInstance().apply {
            strength = Collator.PRIMARY
            decomposition = Collator.CANONICAL_DECOMPOSITION
        }

        open fun matches(query: String?, target: String?): Boolean {
            if (query == null || target == null) return false
            val compare = collator.compare(query, target)
            return if (compare == 0) {
                true
            } else if (compare < 0) {
                collator.compare(query + '\uFFFF', target) >= 0
            } else {
                false
            }
        }

        open fun isBreak(thisType: Int, prevType: Int, nextType: Int): Boolean {
            when (prevType) {
                Character.UNASSIGNED.toInt(),
                Character.SPACE_SEPARATOR.toInt(),
                Character.LINE_SEPARATOR.toInt(),
                Character.PARAGRAPH_SEPARATOR.toInt() -> return true
            }
            when (thisType) {
                Character.UPPERCASE_LETTER.toInt() -> {
                    if (nextType != Character.UPPERCASE_LETTER.toInt() &&
                        nextType != Character.OTHER_SYMBOL.toInt() &&
                        nextType != Character.DECIMAL_DIGIT_NUMBER.toInt() &&
                        nextType != Character.UNASSIGNED.toInt()
                    ) {
                        return true
                    }
                    return prevType != Character.UPPERCASE_LETTER.toInt()
                }
                Character.TITLECASE_LETTER.toInt() -> {
                    return prevType != Character.UPPERCASE_LETTER.toInt()
                }
                Character.LOWERCASE_LETTER.toInt() -> {
                    return prevType > Character.OTHER_LETTER.toInt() || prevType <= Character.UNASSIGNED.toInt()
                }
                Character.DECIMAL_DIGIT_NUMBER.toInt(),
                Character.LETTER_NUMBER.toInt(),
                Character.OTHER_NUMBER.toInt() -> {
                    return !(prevType == Character.DECIMAL_DIGIT_NUMBER.toInt() ||
                            prevType == Character.LETTER_NUMBER.toInt() ||
                            prevType == Character.OTHER_NUMBER.toInt())
                }
                Character.MATH_SYMBOL.toInt(),
                Character.CURRENCY_SYMBOL.toInt(),
                Character.OTHER_PUNCTUATION.toInt(),
                Character.DASH_PUNCTUATION.toInt() -> return true
                else -> return false
            }
        }

        companion object {
            @JvmStatic
            fun getInstance(): StringMatcher = StringMatcher()
        }
    }

    open class StringMatcherSpace : StringMatcher() {
        override fun isBreak(thisType: Int, prevType: Int, nextType: Int): Boolean {
            return prevType == Character.UNASSIGNED.toInt() || prevType == Character.SPACE_SEPARATOR.toInt()
        }

        companion object {
            @JvmStatic
            fun getInstance(): StringMatcherSpace = StringMatcherSpace()
        }
    }
}
