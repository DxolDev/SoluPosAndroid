package com.solupos.contenedor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solupos.contenedor.data.db.StoreEntity
import com.solupos.contenedor.data.repository.StoreRepository
import com.solupos.contenedor.security.KioskPinStore
import com.solupos.contenedor.security.PinCrypto
import com.solupos.contenedor.security.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class StoreFormState(
    val name: String = "",
    val url: String = "",
    val nameError: Boolean = false,
    val urlError: Boolean = false,
    val isSaved: Boolean = false,
    /** Hay un token guardado; el campo vacío significa "conservarlo". */
    val hasToken: Boolean = false,
    val token: String = "",
    val kioskMode: Boolean = false,
    val kioskFrontCamera: Boolean = false,
    val deviceHasPin: Boolean = false,
    val pin: String = "",
    val pinError: Boolean = false
)

class StoreFormViewModel(
    private val repository: StoreRepository,
    private val editStoreId: String?,
    private val tokenStore: TokenStore,
    private val pinStore: KioskPinStore
) : ViewModel() {

    val isEditing: Boolean = editStoreId != null

    private val _state = MutableStateFlow(StoreFormState(deviceHasPin = pinStore.hasPin()))
    val state: StateFlow<StoreFormState> = _state.asStateFlow()

    private var existing: StoreEntity? = null

    init {
        editStoreId?.let { id ->
            viewModelScope.launch {
                repository.getById(id)?.let { store ->
                    existing = store
                    _state.value = _state.value.copy(
                        name = store.name,
                        url = store.url,
                        hasToken = tokenStore.get(id) != null,
                        kioskMode = store.kioskMode,
                        kioskFrontCamera = store.kioskFrontCamera
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value, nameError = false)
    }

    fun onUrlChange(value: String) {
        _state.value = _state.value.copy(url = value, urlError = false)
    }

    fun onTokenChange(value: String) {
        _state.value = _state.value.copy(token = value)
    }

    fun onKioskModeChange(value: Boolean) {
        _state.value = _state.value.copy(kioskMode = value, pinError = false)
    }

    fun onKioskFrontCameraChange(value: Boolean) {
        _state.value = _state.value.copy(kioskFrontCamera = value)
    }

    fun onPinChange(value: String) {
        _state.value = _state.value.copy(pin = value.filter { it in '0'..'9' }.take(PinCrypto.MAX_LENGTH), pinError = false)
    }

    fun save() {
        val current = _state.value
        val name = current.name.trim()
        val url = current.url.trim()
        // Con el quiosco activo hace falta un PIN: el existente del equipo o uno nuevo válido.
        val pinNeeded = current.kioskMode && (!current.deviceHasPin || current.pin.isNotEmpty())
        val pinError = pinNeeded && !PinCrypto.isValidFormat(current.pin)
        if (name.isEmpty() || url.isEmpty() || pinError) {
            _state.value = current.copy(nameError = name.isEmpty(), urlError = url.isEmpty(), pinError = pinError)
            return
        }
        viewModelScope.launch {
            val id = editStoreId ?: UUID.randomUUID().toString()
            val base = existing ?: StoreEntity(id = id, name = name, url = url)
            val store = base.copy(
                name = name,
                url = url,
                kioskMode = current.kioskMode,
                kioskFrontCamera = current.kioskFrontCamera
            )
            if (editStoreId != null) repository.update(store) else repository.insert(store)
            if (store.kioskMode) repository.clearKioskExcept(store.id)
            if (current.token.isNotBlank()) tokenStore.set(id, current.token)
            if (current.kioskMode && current.pin.isNotEmpty()) pinStore.setPin(current.pin)
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}

class StoreFormViewModelFactory(
    private val repository: StoreRepository,
    private val editStoreId: String?,
    private val tokenStore: TokenStore,
    private val pinStore: KioskPinStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        StoreFormViewModel(repository, editStoreId, tokenStore, pinStore) as T
}
