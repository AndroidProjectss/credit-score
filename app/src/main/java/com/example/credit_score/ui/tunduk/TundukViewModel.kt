package com.example.credit_score.ui.tunduk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.credit_score.data.model.TundukData
import com.example.credit_score.data.repository.TundukRepository
import kotlinx.coroutines.launch

/**
 * ViewModel для работы с Тундук API
 */
class TundukViewModel : ViewModel() {
    
    private val repository = TundukRepository()
    
    // LiveData для состояния загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // LiveData для данных клиента
    private val _clientData = MutableLiveData<TundukData?>()
    val clientData: LiveData<TundukData?> = _clientData
    
    // LiveData для ошибки
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    /**
     * Получает данные клиента по ИНН
     * @param inn ИНН клиента
     */
    fun getClientDataByInn(inn: String) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            repository.getClientDataByInn(inn)
                .onSuccess { data ->
                    _clientData.postValue(data)
                    _error.postValue(null)
                }
                .onFailure { exception ->
                    _clientData.postValue(null)
                    _error.postValue(exception.message ?: "Неизвестная ошибка")
                }
            
            _isLoading.postValue(false)
        }
    }
    
    /**
     * Сбрасывает состояние
     */
    fun reset() {
        _clientData.value = null
        _error.value = null
        _isLoading.value = false
    }
}
