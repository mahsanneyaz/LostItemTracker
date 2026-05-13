package com.srm.lostitemtracker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {

    private val repository = ItemRepository()

    private val _submitResult = MutableLiveData<Result<Unit>>()
    val submitResult: LiveData<Result<Unit>> = _submitResult

    fun submitLost(
        context     : Context,
        itemName    : String,
        category    : String,
        description : String,
        location    : String,
        date        : String,
        photoUri    : Uri?
    ) {
        viewModelScope.launch {
            _submitResult.value = repository.submitLostReport(
                context, itemName, category, description, location, date, photoUri
            )
        }
    }

    fun submitFound(
        context     : Context,
        itemName    : String,
        category    : String,
        description : String,
        location    : String,
        date        : String,
        photoUri    : Uri?
    ) {
        viewModelScope.launch {
            _submitResult.value = repository.submitFoundReport(
                context, itemName, category, description, location, date, photoUri
            )
        }
    }
}