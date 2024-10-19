package com.example.fawna.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    private val _trinkets = MutableLiveData<List<Trinket>>()
    val trinkets: LiveData<List<Trinket>> = _trinkets

    init {
        loadSampleTrinkets()
    }

    private fun loadSampleTrinkets() {
        val sampleTrinkets = listOf(
            Trinket("1", "Trinket 1", "https://example.com/image1.jpg"),
            Trinket("2", "Trinket 2", "https://example.com/image2.jpg"),
            Trinket("3", "Trinket 3", "https://example.com/image3.jpg"),
            // Add more sample trinkets as needed
        )
        _trinkets.value = sampleTrinkets
    }

    fun addTrinket(trinket: Trinket) {
        val currentList = _trinkets.value.orEmpty().toMutableList()
        currentList.add(trinket)
        _trinkets.value = currentList
    }
}