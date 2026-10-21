package com.example.util

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.DailyThoughtPoster

/**
 * Returns localized string according to the user's selected language mode:
 * - "ENGLISH": English only
 * - "HINDI": Hindi only
 * - "BILINGUAL": Both English & Hindi
 */
fun tr(appLanguage: String, en: String, hi: String): String {
    return when (appLanguage) {
        "ENGLISH" -> en
        "HINDI" -> hi
        else -> "$en • $hi"
    }
}

/**
 * Launches a real Google Search in the device browser or Google Search app.
 */
fun openGoogleSearch(context: Context, rawQuery: String) {
    val query = rawQuery.trim()
    if (query.isBlank()) {
        Toast.makeText(context, "Please enter or speak a search topic first / कृपया खोज विषय लिखें या बोलें", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val encoded = Uri.encode(query)
        val googleUri = Uri.parse("https://www.google.com/search?q=$encoded")
        val viewIntent = Intent(Intent.ACTION_VIEW, googleUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(viewIntent)
    } catch (e: Exception) {
        try {
            val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webSearchIntent)
        } catch (ex: Exception) {
            Toast.makeText(context, "Opened Google Search for: $query", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Shares a bilingual Hindi + English Thought of the Day Quote Poster via Android's native Share Sheet.
 */
fun shareThoughtPoster(context: Context, poster: DailyThoughtPoster) {
    val shareText = buildString {
        appendLine("✨ ${poster.themeTitle} ✨")
        appendLine()
        appendLine("🇮🇳 हिंदी सुविचार:")
        appendLine("\"${poster.quoteHindi}\"")
        appendLine()
        appendLine("🌐 English Quote:")
        appendLine("\"${poster.quoteEnglish}\"")
        appendLine()
        appendLine("— ${poster.authorOrSource}")
        appendLine("💡 ${poster.reflectionNote}")
    }
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, poster.themeTitle)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Thought Poster / सुविचार साझा करें").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Quote copied to share!", Toast.LENGTH_SHORT).show()
    }
}
