
package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.itemhandling.GenreBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.model.api.ItemSortBy
import org.koin.java.KoinJavaComponent.inject

class HomeFragmentGenresRow : HomeFragmentRow {
private val apiClient: ApiClient by inject(ApiClient::class.java)
    override fun addToRowsAdapter(
        context: Context,
        cardPresenter: CardPresenter,
        rowsAdapter: MutableObjectAdapter<Row>,
    ) {
        val rowAdapter = ItemRowAdapter(
            context,
            emptyList(),
            cardPresenter,
            rowsAdapter,
            true,
        )

        val row = ListRow(
            HeaderItem(context.getString(R.string.home_section_genres)),
            rowAdapter,
        )
        rowAdapter.setRow(row)
        rowsAdapter.add(row)

        CoroutineScope(Dispatchers.Main).launch {
            val genres = withContext(Dispatchers.IO) {
                apiClient.genresApi.getGenres(
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                ).content.items
            }

            rowAdapter.clear()
            genres.mapNotNull { it.name }
                .distinct()
                .sorted()
                .forEach { name ->
                    rowAdapter.add(GenreBaseRowItem(name))
                }
        }
    }
}
