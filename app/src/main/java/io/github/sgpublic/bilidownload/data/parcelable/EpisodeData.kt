package io.github.sgpublic.bilidownload.data.parcelable

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class EpisodeData(
    var index: String = "",
    var aid: Long = 0L,
    var cid: Long = 0L,
    var epId: Long = 0L,
    var cover: String = "",
    var pubRealTime: String = "",
    var title: String = "",
    var payment: Int = 0,
    var bvid: String = "",
    var badge: String = "",
    var badgeColor: Int = 0,
    var badgeColorNight: Int = 0
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val infoData = other as EpisodeData
        return epId == infoData.epId
    }

    override fun hashCode(): Int {
        return Objects.hash(epId)
    }

    fun toEntryJson(): EntryJson {
        return EntryJson().also {
            it.title = title
            it.cover = cover
            it.source.av_id = aid
            it.source.cid = cid
            it.ep.index = index
            it.ep.av_id = aid
            it.ep.cover = cover
            it.ep.danmaku = cid
            it.ep.episode_id = epId
            it.ep.bvid = bvid
            it.ep.index_title = title
            it.ep.sort_index = index.toIntOrNull() ?: 0
        }
    }

    override fun toString(): String = "$index $title"

    companion object {
        const val PAYMENT_NORMAL = 2
        const val PAYMENT_VIP = 13
    }
}