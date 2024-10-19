package com.example.fawna.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fawna.R
import java.util.Date


class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    private val _trinkets = MutableLiveData<List<Trinket>>()
    val trinkets: LiveData<List<Trinket>> = _trinkets

    private val _selectedTrinket = MutableLiveData<TrinketDetails>()
    val selectedTrinket: LiveData<TrinketDetails> = _selectedTrinket

    init {
        loadSampleTrinkets()
    }

    private fun loadSampleTrinkets() {
        val context = getApplication<Application>().applicationContext
        
        val sampleTrinkets = (0..62).map { index ->
            val resourceName = "ic_trinket_$index"
            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            Trinket(
                id = index.toString(),
                name = "Trinket $index",
                imageResId = resourceId
            )
        }
        _trinkets.value = sampleTrinkets
    }

    fun addTrinket(trinket: Trinket) {
        val currentList = _trinkets.value.orEmpty().toMutableList()
        currentList.add(trinket)
        _trinkets.value = currentList
    }

    fun selectTrinket(trinketId: String) {
        // In a real app, you would fetch this data from a database
        _selectedTrinket.value = TrinketDetails(
            id = trinketId,
            dateCreated = Date(),
            question = "Sample question for trinket $trinketId",
            answer = "Sample answer for trinket $trinketId",
            timestamp = Date(),
            notes = "Sample notes for trinket $trinketId"
        )
    }
}

data class TrinketDetails(
    val id: String,
    val dateCreated: Date,
    val question: String,
    val answer: String,
    val timestamp: Date,
    val notes: String
)
