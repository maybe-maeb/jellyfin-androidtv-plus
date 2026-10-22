package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.util.apiclient.JellyfinImage

class GenreBaseRowItem(
    val genreName: String,
) : BaseRowItem(
    baseRowType = BaseRowType.Genre,
) {
    override fun getCardName(context: Context) = genreName

    override fun getName(context: Context) = genreName

    override fun getFullName(context: Context) = genreName

    override fun getImage(imageType: ImageType): JellyfinImage? = null

    override fun equals(other: Any?): Boolean =
        other is GenreBaseRowItem && other.genreName == genreName

    override fun hashCode(): Int = genreName.hashCode()
}
