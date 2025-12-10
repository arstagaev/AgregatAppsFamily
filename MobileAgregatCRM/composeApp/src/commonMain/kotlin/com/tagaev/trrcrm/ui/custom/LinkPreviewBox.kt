package com.tagaev.trrcrm.ui.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import org.koin.compose.koinInject
import kotlin.toString


//@Composable
//fun TextC(
//    text: String,
//    modifier: Modifier = Modifier
//) {
//    // Find first URL in the incoming text. If there is no URL, the preview stays collapsed.
//    Column {
//        TextC(text)
//
//        LinkPreviewBox(
//            text = text,
//            modifier = modifier,
//        )
//    }
//}

@Composable
fun LinkPreviewBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    val firstUrl = remember(text) { extractFirstUrlFromText(text) }
    if (firstUrl == null) {
        // No link in text – nothing to show.
        return
    }

    val httpClient: HttpClient = koinInject()

    var preview by remember { mutableStateOf<LinkPreview?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(firstUrl) {
        isLoading = true
        error = null
        try {
            preview = fetchLinkPreview(httpClient, firstUrl)
        } catch (t: Throwable) {
            error = t.message ?: "Ошибка загрузки превью"
        } finally {
            isLoading = false
        }
    }

    // If we have finished loading, there is no error, and the preview contains
    // no useful content (no image, no title, no description), do not show anything.
    if (!isLoading &&
        error == null &&
        preview?.let { it.imageUrl.isNullOrBlank() && it.title.isNullOrBlank() && it.description.isNullOrBlank() } == true
    ) {
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when {
            isLoading -> {
                println(">>> isLoading")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }

            error != null || preview == null -> {
                println(">>> error ${error.toString()}")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = firstUrl,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                    Text(
                        text = error ?: "Нет превью",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                println(">>> preview ${preview.toString()}")
                val p = preview!!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!p.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = p.imageUrl,
                            contentDescription = p.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = p.title ?: firstUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                        if (!p.description.isNullOrBlank()) {
                            Text(
                                text = p.description!!,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                        }
                        Text(
                            text = p.url,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

data class LinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?
)

private data class MetaTag(val attrs: Map<String, String>)

private fun parseMetaTags(html: String): List<MetaTag> {
    val tagRegex = Regex("""<meta\s+([^>]+)>""", RegexOption.IGNORE_CASE)
    val attrRegex = Regex("""(\w+)\s*=\s*["']([^"']*)["']""")
    return tagRegex.findAll(html).map { match ->
        val attrString = match.groupValues[1]
        val attrs = attrRegex.findAll(attrString).associate {
            it.groupValues[1].lowercase() to it.groupValues[2]
        }
        MetaTag(attrs)
    }.toList()
}

private fun findMetaContent(
    metaTags: List<MetaTag>,
    attrName: String,      // "property" or "name"
    attrValue: String      // e.g. "og:title"
): String? {
    val key = attrName.lowercase()
    return metaTags
        .firstOrNull { tag ->
            tag.attrs[key]?.equals(attrValue, ignoreCase = true) == true
        }
        ?.attrs["content"]
}

// Try to extract image URL from JSON-LD scripts (schema.org Product, etc.)
private fun extractImageFromJsonLd(html: String): String? {
    // Match &amp; capture the contents of &lt;script type="application/ld+json"&gt; ... &lt;/script&gt;
    val scriptRegex = Regex(
        pattern = """<script[^>]+type=['"]application/ld\+json['"][^>]*>(.*?)</script>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    for (match in scriptRegex.findAll(html)) {
        val json = match.groupValues[1]
        // First, look for a simple scalar: "image": "https://..."
        val imageRegex = Regex(""""image"\s*:\s*"(https?://[^"]+)"""")
        imageRegex.find(json)?.let { return it.groupValues[1] }
        // Then, look for an array: "image": ["https://...", ...]
        val imageArrayRegex = Regex(""""image"\s*:\s*\[\s*"(https?://[^"]+)"""")
        imageArrayRegex.find(json)?.let { return it.groupValues[1] }
    }
    return null
}

// Amazon product page: try to pull the main product image from <img id="landingImage" ...>
private fun extractAmazonMainImage(html: String): String? {
    val imgTagRegex = Regex(
        pattern = """<img[^>]+id=['"]landingImage['"][^>]*>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val tagMatch = imgTagRegex.find(html) ?: return null
    val tag = tagMatch.value

    fun attr(name: String): String? {
        val attrRegex = Regex("""$name\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return attrRegex.find(tag)?.groupValues?.getOrNull(1)
    }

    // 1) Full‑res URL is often in data-old-hires
    attr("data-old-hires")?.takeIf { it.startsWith("http") }?.let { return it }

    // 2) Or inside data-a-dynamic-image JSON: "https://...":[w,h], pick the first URL
    attr("data-a-dynamic-image")?.let { jsonAttr ->
        val urlRegex = Regex(""""(https?://[^"]+)"""")
        urlRegex.find(jsonAttr)?.groupValues?.getOrNull(1)?.let { return it }
    }

    // 3) Fallback to src
    attr("src")?.takeIf { it.startsWith("http") }?.let { return it }

    return null
}

private fun looksLikeImageUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val lower = url.lowercase()
    return lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".png") ||
            lower.endsWith(".webp") ||
            lower.endsWith(".gif")
}

// Build absolute URL from page URL and raw src/href value.
private fun buildAbsoluteUrl(pageUrl: String, raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    // Extract scheme and host from pageUrl
    val regex = Regex("""^(https?)://([^/]+)""")
    val match = regex.find(pageUrl) ?: return null
    val scheme = match.groupValues[1]
    val host = match.groupValues[2]
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("//") -> "$scheme:$trimmed"
        trimmed.startsWith("/") -> "$scheme://$host$trimmed"
        else -> "$scheme://$host/$trimmed"
    }
}

// Generic meta fallback: pick first meta content that looks like an image URL
private fun extractImageFromMetaCandidates(metaTags: List<MetaTag>, pageUrl: String): String? {
    for (tag in metaTags) {
        val raw = tag.attrs["content"] ?: continue
        val absolute = buildAbsoluteUrl(pageUrl, raw) ?: continue
        if (looksLikeImageUrl(absolute.substringBefore("?"))) {
            return absolute
        }
    }
    return null
}

// Generic HTML fallback: pick first <img> src that looks like an image URL
private fun extractFirstImageFromImgTag(html: String, pageUrl: String): String? {
    val imgRegex = Regex(
        pattern = """<img[^>]+src=['"]([^'"]+)['"][^>]*>""",
        options = setOf(RegexOption.IGNORE_CASE)
    )
    for (match in imgRegex.findAll(html)) {
        val raw = match.groupValues[1]
        val absolute = buildAbsoluteUrl(pageUrl, raw) ?: continue
        if (looksLikeImageUrl(absolute.substringBefore("?"))) {
            return absolute
        }
    }
    return null
}

suspend fun fetchLinkPreview(
    client: HttpClient,
    url: String
): LinkPreview {
    val html = downloadHtml(client, url)
    val metaTags = parseMetaTags(html)

    fun prop(name: String) = findMetaContent(metaTags, "property", name)
    fun meta(name: String) = findMetaContent(metaTags, "name", name)

    val title =
        prop("og:title")
            ?: meta("twitter:title")
    val description =
        prop("og:description")
            ?: meta("twitter:description")
    val imageUrl =
        prop("og:image")
            ?: meta("twitter:image")
            ?: prop("og:image:url")
            ?: meta("image") // some sites use name="image"
            ?: extractImageFromJsonLd(html)
            // Domain-specific: Amazon / a.co short links
            ?: if (url.contains("amazon.", ignoreCase = true) || url.contains("a.co/", ignoreCase = true)) {
                extractAmazonMainImage(html)
            } else {
                null
            }
            // Generic fallbacks for marketplaces like Ozon, Wildberries, Avito, etc.
            ?: extractImageFromMetaCandidates(metaTags, url)
            ?: extractFirstImageFromImgTag(html, url)

    return LinkPreview(
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl
    )
}

suspend fun downloadHtml(client: HttpClient, url: String): String {
    val resp: HttpResponse = client.get(url)
    return resp.bodyAsText()
}

private val urlRegex =
    "(https?://[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex()

private fun extractFirstUrlFromText(text: String): String? =
    urlRegex.find(text)
        ?.value
        ?.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '"', '\'')
