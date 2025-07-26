package com.dinostreaming

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class DinoStreaming : MainAPI() {
    override var mainUrl = "https://www.dinostreaming.it"
    override var name = "DinoStreaming"
    override val hasMainPage = true
    override val lang = "it"

    override val mainPage = listOf(
        "Film" to "\$mainUrl/film-streaming/",
        "Serie TV" to "\$mainUrl/serie-tv-streaming/"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(request.data).document
        val items = doc.select("div.result-item").mapNotNull {
            val title = it.selectFirst("h3.result-title")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("src")
            val type = if (request.name.contains("Serie")) TvType.TvSeries else TvType.Movie

            newMovieSearchResponse(title, link, type) {
                this.posterUrl = poster
            }
        }

        return newHomePageResponse(request.name, items)
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text() ?: return null
        val poster = doc.select("img").firstOrNull()?.attr("src")
        val description = doc.select("div.entry-content p").firstOrNull()?.text()

        val isSerie = url.contains("/serie-tv/")

        return if (isSerie) {
            val episodes = doc.select("ul.episodios li").map {
                val name = it.selectFirst("a")?.text() ?: ""
                val link = it.selectFirst("a")?.attr("href") ?: ""
                Episode(link, name)
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(data).document
        val iframe = doc.select("iframe").firstOrNull()?.attr("src") ?: return

        loadExtractor(iframe, data, subtitleCallback, callback)
    }
}
