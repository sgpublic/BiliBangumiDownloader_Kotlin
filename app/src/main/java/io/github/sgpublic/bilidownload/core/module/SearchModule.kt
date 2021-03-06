package io.github.sgpublic.bilidownload.core.module

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import io.github.sgpublic.bilidownload.Application
import io.github.sgpublic.bilidownload.R
import io.github.sgpublic.bilidownload.core.data.SearchData
import io.github.sgpublic.bilidownload.core.util.LogCat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SearchModule {
    private val helper: ApiModule = ApiModule()

    fun getHotWord(callback: HotWordCallback) {
        val call = helper.getHotWordRequest()
        call.enqueue(object : ApiModule.BaseOkHttpCallback(callback) {
            override fun onParseData(data: JSONObject) {
                val hotWords = ArrayList<String>()
                val array = data.getJSONArray("list")
                for (array_index in 0 until array.length()) {
                    val dataIndex = array.getJSONObject(array_index)
                    hotWords.add(dataIndex.getString("keyword"))
                }
                callback.onResult(hotWords)
            }
        })
    }

    fun suggest(keyword: String, callback: SuggestCallback) {
        val call = helper.getSearchSuggestRequest(keyword)
        call.enqueue(object : ApiModule.BaseOkHttpCallback(callback) {
            override fun onParseData(data: JSONObject) {
                val suggestions = ArrayList<Spannable>()
                val array = data.getJSONObject("result").getJSONArray("tag")
                var arrayIndex = 0
                while (arrayIndex < 7 && arrayIndex < array.length()) {
                    val dataIndex = array.getJSONObject(arrayIndex)
                    val valueString = dataIndex.getString("value")
                    val valueSpannable: Spannable = SpannableString(valueString)
                    for (value_index in keyword.indices) {
                        val keywordIndex =
                            keyword.substring(value_index).substring(0, 1)
                        val valueStringSub = valueString.indexOf(keywordIndex)
                        if (valueStringSub >= 0) {
                            valueSpannable.setSpan(
                                ForegroundColorSpan(Application.getColor(R.color.colorPrimary)),
                                valueStringSub, valueStringSub + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                    suggestions.add(valueSpannable)
                    arrayIndex++
                }
                callback.onResult(suggestions)
            }
        })
    }

    fun search(keyword: String, callback: SearchCallback) {
        val call = helper.getSearchResultRequest(keyword)
        call.enqueue(object : ApiModule.BaseOkHttpCallback(callback) {
            override fun onParseData(data: JSONObject) {
                val dataObj = data.getJSONObject("data")
                if (dataObj.isNull("result")) {
                    LogCat.d("empty result")
                    callback.onResult(listOf())
                } else {
                    val array = dataObj.getJSONArray("result")
                    callback.onResult(parse(array))
                }
            }
        })
    }

    private fun parse(array: JSONArray): LinkedList<SearchData> {
        val searchDataList = LinkedList<SearchData>()
        for (i in 0 until array.length()) {
            val index = array.getJSONObject(i)
            val searchData = SearchData()
            if (!index.isNull("badges")) {
                val seasonBadge = index.getJSONArray("badges").getJSONObject(0)
                searchData.seasonBadge = seasonBadge.getString("text")
                searchData.seasonBadgeColor = Color.parseColor(
                    seasonBadge.getString("bg_color")
                )
                searchData.seasonBadgeColorNight = Color.parseColor(
                    seasonBadge.getString("bg_color_night")
                )
            }
            searchData.seasonCover = index.getString("cover")
            if (index.isNull("media_score")) {
                searchData.mediaScore = 0.0
            } else {
                searchData.mediaScore = index.getJSONObject("media_score")
                    .getDouble("score")
            }
            searchData.seasonId = index.getLong("season_id")
            //searchData.season_title = object.getString("season_title");
            val seasonTitleString = index.getString("title")
            val seasonTitleSpannable: Spannable = SpannableString(
                seasonTitleString
                    .replace("<em class=\"keyword\">", "")
                    .replace("</em>", "")
            )
            val seasonTitleSubStart =
                seasonTitleString.indexOf("<em class=\"keyword\">")
            val seasonTitleSubEnd = seasonTitleString
                .replace("<em class=\"keyword\">", "")
                .indexOf("</em>")
            if (seasonTitleSubStart >= 0 && seasonTitleSubEnd >= 0) {
                seasonTitleSpannable.setSpan(
                    ForegroundColorSpan(Application.getColor(R.color.colorPrimary)),
                    seasonTitleSubStart, seasonTitleSubEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            searchData.seasonTitle = seasonTitleSpannable
            if (index.getLong("pubtime") * 1000 > System.currentTimeMillis()) {
                searchData.selectionStyle = SearchData.SelectionStyle.GRID
            } else {
                searchData.selectionStyle = SearchData.SelectionStyle.of(index.getString("selection_style"))
            }
            val date = Date(index.getLong("pubtime") * 1000)
            val format = SimpleDateFormat("yyyy", Locale.CHINA)
            searchData.seasonContent = format.format(date) + "???"
            searchData.seasonContent = index.getString("season_type_name") + "???" +
                    index.getString("areas") + "\n" +
                    index.getString("styles")
            val epsArray = index.getJSONArray("eps")
            if (epsArray.length() > 0) {
                val episode = epsArray.getJSONObject(0)
                searchData.episodeCover = episode.getString("cover")
                val episodeTitleString = episode.getString("long_title")
                val episodeTitleSpannable: Spannable = SpannableString(
                    episodeTitleString
                        .replace("<em class=\"keyword\">", "")
                        .replace("</em>", "")
                )
                val episodeTitleSubStart =
                    episodeTitleString.indexOf("<em class=\"keyword\">")
                val episodeTitleSubEnd = episodeTitleString
                    .replace("<em class=\"keyword\">", "")
                    .indexOf("</em>")
                if (episodeTitleSubStart >= 0 && episodeTitleSubEnd >= 0) {
                    episodeTitleSpannable.setSpan(
                        ForegroundColorSpan(Application.getColor(R.color.colorPrimary)),
                        episodeTitleSubStart, episodeTitleSubEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                searchData.episodeTitle = episodeTitleSpannable
                if (!episode.isNull("badges")) {
                    val episodeBadge = episode.getJSONArray("badges").getJSONObject(0)
                    searchData.episodeBadge = episodeBadge.getString("text")
                    searchData.episodeBadgeColor = Color.parseColor(
                        episodeBadge.getString("bg_color")
                    )
                    searchData.episodeBadgeColorNight = Color.parseColor(
                        episodeBadge.getString("bg_color_night")
                    )
                }
            }
            searchDataList.add(searchData)
        }
        return searchDataList
    }

    interface SearchCallback : ApiModule.Callback {
        fun onResult(searchData: List<SearchData>)
    }

    interface SuggestCallback : ApiModule.Callback {
        fun onResult(suggestions: List<Spannable>)
    }

    interface HotWordCallback : ApiModule.Callback {
        override fun onFailure(code: Int, message: String?, e: Throwable?) {}
        fun onResult(hotWords: List<String>)
    }
}