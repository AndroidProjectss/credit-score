package com.example.credit_score.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Отсканируйте паспорт для распознавания"
    }
    val text: LiveData<String> = _text
}