package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.parentBackdropImages

class GenreBaseRowItem(
    val genreName: String,
    library: BaseItemDto,
    representativeMovie: BaseItemDto?,
) : BaseRowItem(
    baseRowType = BaseRowType.Genre,
    baseItem = library,
) {
    private val movieItem = representativeMovie?.let {
        BaseItemDtoBaseRowItem(it)
    }
    val representativeMovieId = representativeMovie?.id

    override val showCardInfoOverlay: Boolean = true

    override val itemId: UUID =
        UUID.nameUUIDFromBytes(genreName.toByteArray(Charsets.UTF_8))

    override fun getCardName(context: Context): String = genreName

    override fun getFullName(context: Context): String = genreName

    override fun getName(context: Context): String = genreName

  override fun getImage(imageType: ImageType): JellyfinImage? {
    val movie = movieItem?.baseItem ?: return null

    return (movie.itemBackdropImages + movie.parentBackdropImages)
        .firstOrNull()
}
}