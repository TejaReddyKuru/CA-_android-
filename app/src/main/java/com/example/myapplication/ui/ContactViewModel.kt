package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Contact
import com.example.myapplication.data.ContactDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val contactDao = ContactDatabase.getDatabase(application).contactDao()

    companion object {
        private const val SEARCH_QUERY_KEY = "search_query"
    }

    // SavedStateHandle preserves the search query across process death
    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(SEARCH_QUERY_KEY, "")

    @OptIn(ExperimentalCoroutinesApi::class)
    val contacts: StateFlow<List<Contact>> = searchQuery
        .flatMapLatest { query ->
            contactDao.searchContacts(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate sample contacts if the database is empty
        viewModelScope.launch(Dispatchers.IO) {
            if (contactDao.getCount() == 0) {
                contactDao.insertAll(
                    listOf(
                        Contact(name = "Alice Smith", phoneNumber = "555-0101"),
                        Contact(name = "Bob Johnson", phoneNumber = "555-0202"),
                        Contact(name = "Charlie Brown", phoneNumber = "555-0303"),
                        Contact(name = "Diana Prince", phoneNumber = "555-0404"),
                        Contact(name = "Evan Wright", phoneNumber = "555-0505"),
                        Contact(name = "Fiona Gallagher", phoneNumber = "555-0606")
                    )
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        savedStateHandle[SEARCH_QUERY_KEY] = query
    }
}
