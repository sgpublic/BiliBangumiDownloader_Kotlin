package io.github.sgpublic.bilidownload.module

import android.content.Context
import android.graphics.Color
import io.github.sgpublic.bilidownload.R
import io.github.sgpublic.bilidownload.data.SeasonData
import io.github.sgpublic.bilidownload.data.SeriesData
import io.github.sgpublic.bilidownload.data.parcelable.EpisodeData
import okhttp3.Call
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*

class SeasonModule(private val context: Context, protected val sid: Long, accessKey: String) {
    private val helper: BaseAPI = BaseAPI(accessKey)
    private val episodeData: LinkedList<EpisodeData> = LinkedList()
    private val seasonData: SeasonData = SeasonData()
    private lateinit var callback: Callback
    
    fun getInfoBySid(callback: Callback) {
        this.callback = callback
        val call = helper.getSeasonInfoAppRequest(sid)
        call.enqueue(object : okhttp3.Callback {
            private var biliplus = false

            override fun onFailure(call: Call, e: IOException) {
                if (e is UnknownHostException) {
                    this@SeasonModule.callback.onFailure(-401, context.getString(R.string.error_network), e)
                } else {
                    this@SeasonModule.callback.onFailure(-402, e.message, e)
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string().toString()
                try {
                    val json = JSONObject(result)
                    if (json.getInt("code") == 0) {
                        val resultObj = json.getJSONObject("result")
                        if (resultObj.isNull("limit")) {
                            doParseAppResult(resultObj)
                            return
                        }
                        seasonData.area = AREA_LIMITED
                    }
                    if (biliplus && json.getInt("code") != -404) {
                        this@SeasonModule.callback.onFailure(-404, json.getString("message"), null)
                        return
                    }
                    helper.getSeasonInfoBiliplusRequest(sid)
                        .enqueue(this)
                    biliplus = true
                } catch (e: JSONException) {
                    this@SeasonModule.callback.onFailure(-403, e.message, e)
                }
            }
        })
    }

    private fun getInfoBySidWeb(){
        val call = helper.getSeasonInfoWebRequest(sid)
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e is UnknownHostException) {
                    callback.onFailure(-411, context.getString(R.string.error_network), e)
                } else {
                    callback.onFailure(-412, e.message, e)
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string().toString()
                try {
                    val json = JSONObject(result)
                    if (json.getInt("code") == 0) {
                        doParseWebResult(json.getJSONObject("result"))
                        return
                    }
                    if (json.getInt("code") != -404) {
                        callback.onFailure(-414, json.getString("message"), null)
                        return
                    }
                } catch (e: JSONException) {
                    callback.onFailure(-413, e.message, e)
                }
            }
        })
    }

    private fun doParseAppResult(json: JSONObject) {
        seasonData.actors = try {
            json.getJSONObject("actor").getString("info")
        } catch (_: JSONException) {
            val actors = json.getJSONArray("actor")
            StringBuilder().apply {
                for (i in 0 until actors.length()) {
                    val actor = actors.getJSONObject(i)
                    append(actor.getString("role"))
                    append("：")
                    append(actor.getString("actor"))
                    append("\n")
                }
            }.toString()
        }
        seasonData.actorsLines = seasonData.actors.split("\n").toTypedArray().size
        seasonData.alias = json.getString("alias")
        seasonData.seasonType = if (!json.isNull("type")) json.getInt("type") else
            json.getJSONObject("media").getInt("type_id")
        val array: JSONArray = json.getJSONArray("seasons")
        val list: ArrayList<SeriesData> = ArrayList<SeriesData>()
        for (arrayIndex in 0 until array.length()) {
            val index: JSONObject = array.getJSONObject(arrayIndex)
            val seriesData = SeriesData()
            if (!index.isNull("badge")) {
                seriesData.badge = index.getString("badge")
                val objectIndexBadgeInfo: JSONObject = index.getJSONObject("badge_info")
                seriesData.badgeColor = Color.parseColor(
                    objectIndexBadgeInfo.getString("bg_color")
                )
                seriesData.badgeColorNight = Color.parseColor(
                    objectIndexBadgeInfo.getString("bg_color_night")
                )
            }
            seriesData.cover = index.getString("cover")
            seriesData.title = index.getString("title")
            seriesData.seasonId = index.getLong("season_id")
            if (seriesData.seasonId != sid) {
                list.add(seriesData)
            } else {
                seriesData.seasonTypeName = if (!json.isNull("type_name"))
                    json.getString("type_name") else
                    json.getJSONObject("media").getString("type_name")
                seasonData.baseInfo = seriesData
            }
        }
        seasonData.series = list
        if (!json.isNull("limit") && seasonData.area == AREA_LOCAL) {
            seasonData.area = AREA_LIMITED
        }
        val description = StringBuilder()
        val arrayAreas: JSONArray = if (!json.isNull("areas"))
            json.getJSONArray("areas") else
            json.getJSONObject("media").getJSONArray("area")
        description.append("番剧 | ")
        for (areas_index in 0 until arrayAreas.length()) {
            if (areas_index != 0) {
                description.append("、")
            }
            description.append(arrayAreas.getJSONObject(areas_index).getString("name"))
        }
        if (json.isNull("publish")) {
            val pubTimeDate = SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.CHINA)
                .parse(json.getString("pub_time"))
                ?: throw NullPointerException()
            val pubTime = SimpleDateFormat("yyyy年MM月dd日开播", Locale.CHINA)
                .format(pubTimeDate)
            description.append("\n").append(pubTime)
                .append("\n").append(json.getJSONObject("media")
                    .getJSONObject("episode_index")
                    .getString("index_show"))
        } else {
            description.append("\n")
                .append(json.getJSONObject("publish")
                    .getString("release_date_show"))
                .append("\n")
                .append(json.getJSONObject("publish")
                    .getString("time_length_show"))
        }
        seasonData.description = description.toString()
        seasonData.evaluate = json.getString("evaluate")
        seasonData.staff = try {
            json.getJSONObject("staff").getString("info")
        } catch (_: JSONException) {
            json.getString("staff")
        }
        seasonData.staffLines = seasonData.staff.split("\n").toTypedArray().size
        description.append("番剧 | ")
        if (!json.isNull("styles")) {
            val styles = StringBuilder()
            val arrayStyles: JSONArray = json.getJSONArray("styles")
            for (styles_index in 0 until arrayStyles.length()) {
                if (styles_index != 0) {
                    styles.append("、")
                }
                styles.append(arrayStyles.getJSONObject(styles_index).getString("name"))
            }
            seasonData.styles = styles.toString()
        }
        seasonData.rating = if (json.isNull("rating")) 0.0 else
            json.getJSONObject("rating").getDouble("score")
        try {
            doParseWebResult(json)
        } catch (_: JSONException) {
            this.getInfoBySidWeb()
        }
    }

    @Throws(JSONException::class)
    private fun doParseWebResult(json: JSONObject) {
        val array: JSONArray = json.getJSONArray("episodes")
        episodeData.clear()
        for (episodesIndex in 0 until array.length()) {
            val episodeDataIndex = EpisodeData()
            val index: JSONObject = array.getJSONObject(episodesIndex)
            episodeDataIndex.aid = if (!index.isNull("aid"))
                index.getLong("aid") else
                index.getString("av_id").toLong()
            episodeDataIndex.cid = index.getLong("cid")
            episodeDataIndex.aid = if (!index.isNull("id"))
                index.getLong("id") else if (!index.isNull("ep_id"))
                index.getString("ep_id").toLong() else
                index.getString("episode_id").toLong()
            episodeDataIndex.cover = index.getString("cover")
            episodeDataIndex.payment = if (!index.isNull("status"))
                index.getInt("status") else
                index.getInt("episode_status")
            if (!index.isNull("bvid")) {
                episodeDataIndex.bvid = index.getString("bvid")
            }
            if (!index.isNull("badge_info")) {
                val badge: JSONObject =
                    index.getJSONObject("badge_info")
                episodeDataIndex.badge = badge.getString("text")
                episodeDataIndex.badgeColor = Color.parseColor(
                    badge.getString("bg_color")
                )
                episodeDataIndex.badgeColorNight = Color.parseColor(
                    badge.getString("bg_color_night")
                )
            }
            val uploadTime = if (!index.isNull("pub_time")) {
                Date(index.getLong("pub_time") * 1000L)
            } else if (!index.isNull("update_time")) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:SS.s", Locale.CHINA)
                    .parse(index.getString("update_time"))
            } else {
                SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.CHINA)
                    .parse(index.getString("pub_real_time"))
            }
            episodeDataIndex.pubRealTime = SimpleDateFormat.getDateInstance().format(uploadTime)
            episodeDataIndex.title = if (index.isNull("long_title"))
                index.getString("index_title") else index.getString("long_title")
            episodeDataIndex.index = if (index.isNull("title"))
                index.getString("index") else index.getString("title")
            episodeData.add(episodeDataIndex)
        }
        callback.onResult(episodeData, seasonData)
    }

    interface Callback {
        fun onFailure(code: Int, message: String?, e: Throwable?)
        fun onResult(episodeData: List<EpisodeData>, seasonData: SeasonData)
    }

    companion object {
        const val AREA_LOCAL = 0
        const val AREA_LIMITED = 1
    }
}