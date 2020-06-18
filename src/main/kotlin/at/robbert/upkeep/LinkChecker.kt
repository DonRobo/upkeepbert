package at.robbert.upkeep

import at.robbert.upkeep.RedirectMethod.*
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import org.apache.commons.text.StringEscapeUtils

data class LinkStatus(val redirectsTo: String, val redirectMethod: RedirectMethod)

object LinkChecker {
    suspend fun checkLink(link: String): LinkStatus? {
        val response = link.httpGet().allowRedirects(false).awaitStringResponseResult()
        fun headerLocation() = response.second.header("Location").single()
        fun javascriptLocation() = response.third.get().let {
            val redirectToRegex = Regex(".*const redirectTo\\s*=\\s*'([^']*)'\\s*;.*", RegexOption.DOT_MATCHES_ALL)
            val match = redirectToRegex.matchEntire(it)
            log.trace("Match $match")
            if (match == null) {
                log.warn("Couldn't read javascript redirect!")
                log.trace(it)
            }
            match?.groupValues?.get(1)?.let { link ->
                StringEscapeUtils.unescapeEcmaScript(link)
            }
        }

        return when (response.second.statusCode) {
            200 -> javascriptLocation()?.let { LinkStatus(it, JAVASCRIPT) }
            301 -> LinkStatus(headerLocation(), HTTP301)
            302 -> LinkStatus(headerLocation(), HTTP302)
            else -> {
                log.warn("Link $link couldn't be accessed: ${response.second}")
                null
            }
        }
    }
}
