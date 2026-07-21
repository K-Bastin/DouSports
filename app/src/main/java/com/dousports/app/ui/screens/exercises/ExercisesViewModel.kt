package com.dousports.app.ui.screens.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.ExerciseEntity
import com.dousports.app.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExercisesUiState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val equipmentList: List<String> = emptyList(),
    val selectedBodyPart: String? = null,
    val selectedEquipment: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedBodyPart = MutableStateFlow<String?>(null)
    private val selectedEquipment = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(ExercisesUiState())
    val uiState: StateFlow<ExercisesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val bodyParts = repository.getAllBodyParts()
            val equipment = repository.getAllEquipment()
            _uiState.update { it.copy(bodyParts = bodyParts, equipmentList = equipment) }
        }

        viewModelScope.launch {
            combine(
                searchQuery.debounce(300),
                selectedBodyPart,
                selectedEquipment
            ) { query, bodyPart, equipment -> Triple(query, bodyPart, equipment) }
                .flatMapLatest { (query, bodyPart, equipment) ->
                    repository.filterExercises(query, bodyPart, equipment)
                }
                .collect { exercises ->
                    _uiState.update { it.copy(exercises = exercises, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isNotBlank()) {
            selectedBodyPart.value = null
            selectedEquipment.value = null
            _uiState.update { it.copy(selectedBodyPart = null, selectedEquipment = null) }
        }
    }

    fun onBodyPartSelected(bodyPart: String?) {
        selectedBodyPart.value = bodyPart
        _uiState.update { it.copy(selectedBodyPart = bodyPart, searchQuery = "") }
        searchQuery.value = ""
    }

    fun onEquipmentSelected(equipment: String?) {
        selectedEquipment.value = equipment
        _uiState.update { it.copy(selectedEquipment = equipment, searchQuery = "") }
        searchQuery.value = ""
    }
}
