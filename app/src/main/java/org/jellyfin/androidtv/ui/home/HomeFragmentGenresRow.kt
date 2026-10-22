
package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.itemhandling.GenreBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.getActivity
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.random.Random
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.isActive

class HomeFragmentGenresRow(
    private val libraries: List<BaseItemDto>,
) : HomeFragmentRow {
    private val apiClient: ApiClient by inject(ApiClient::class.java)

    override fun addToRowsAdapter(
        context: Context,
        cardPresenter: CardPresenter,
        rowsAdapter: MutableObjectAdapter<Row>,
    ) {
       val genreCardPresenter = CardPresenter(true, 126)
val rowAdapter = ItemRowAdapter(
    context,
    emptyList(),
    genreCardPresenter,
    rowsAdapter,
)

        val row = ListRow(
            HeaderItem(context.getString(R.string.home_section_genres)),
            rowAdapter,
        )
        rowAdapter.setRow(row)
        rowsAdapter.add(row)

        val lifecycleOwner = context.getActivity() as? LifecycleOwner ?: return

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val library = libraries.firstOrNull {
                    it.collectionType == CollectionType.MOVIES
                } ?: libraries.firstOrNull()

                if (library == null) {
                    Timber.w("Genres row: no library available")
                    return@repeatOnLifecycle
                }

                try {
                    val genres = withContext(Dispatchers.IO) {
                        apiClient.genresApi.getGenres(
                            sortBy = setOf(ItemSortBy.SORT_NAME),
                        ).content.items.mapNotNull { it.name }.distinct()
                    }

                    val genreData = withContext(Dispatchers.IO) {
                        kotlinx.coroutines.coroutineScope {
                            genres.map { genre ->
                                async {
                                    runCatching {
                                        val result = apiClient.itemsApi.getItems(
                                            parentId = library.id,
                                            includeItemTypes = setOf(BaseItemKind.MOVIE),
                                            genres = setOf(genre),
                                            recursive = true,
                                            limit = 30,
                                            fields = ItemRepository.itemFields,
                                        ).content

                                        val movies = result.items
                                        if (movies.isEmpty()) {
                                            null
                                        } else {
                                            Triple(
                                                genre,
                                                result.totalRecordCount ?: movies.size,
                                                movies,
                                            )
                                        }
                                    }.onFailure {
                                        Timber.w(it, "Failed to load genre: %s", genre)
                                    }.getOrNull()
                                }
                            }.awaitAll()
                                .filterNotNull()
                                .sortedByDescending { it.second }
                        }
                    }

                    genreData.forEach { (name, _, movies) ->
                        rowAdapter.add(
                            GenreBaseRowItem(
                                genreName = name,
                                library = library,
                                representativeMovie = movies.random(),
                            )
                        )
                    }

                   while (kotlinx.coroutines.currentCoroutineContext().isActive) {
    delay(24 * 60 * 60 * 1000L)

    genreData.forEach { (name, _, _) ->
        val index = (0 until rowAdapter.size()).firstOrNull { i ->
            (rowAdapter.get(i) as? GenreBaseRowItem)?.genreName == name
        } ?: return@forEach

        val currentMovieId =
            (rowAdapter.get(index) as? GenreBaseRowItem)?.representativeMovieId

        val newMovie = runCatching {
            apiClient.itemsApi.getItems(
                parentId = library.id,
                includeItemTypes = setOf(BaseItemKind.MOVIE),
                genres = setOf(name),
                recursive = true,
                limit = 1,
                sortBy = setOf(ItemSortBy.RANDOM),
                fields = ItemRepository.itemFields,
            ).content.items.firstOrNull()
        }.onFailure {
            Timber.w(it, "Failed to refresh genre artwork: %s", name)
        }.getOrNull()

        if (newMovie != null && newMovie.id != currentMovieId) {
            rowAdapter.set(
                index,
                GenreBaseRowItem(
                    genreName = name,
                    library = library,
                    representativeMovie = newMovie,
                ),
            )
        }
    }
}
                   
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load Genres home row")
                }
            }
        }
    }
}
