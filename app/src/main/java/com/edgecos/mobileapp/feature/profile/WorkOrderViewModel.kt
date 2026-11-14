package com.edgecos.mobileapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.WorkOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkOrderUiState(
    val workOrders: List<WorkOrder> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val selectedWorkOrder: WorkOrder? = null,
    val isLoadingDetail: Boolean = false,
    val isSubmittingReply: Boolean = false,
    val isClosing: Boolean = false,
    val error: String? = null,
)

class WorkOrderViewModel : ViewModel() {
    private val repo = ServiceLocator.workOrderRepository

    private val _state = MutableStateFlow(WorkOrderUiState())
    val state: StateFlow<WorkOrderUiState> = _state.asStateFlow()

    val currentUserId: Long? get() = ServiceLocator.authRepository.currentUser.value?.id

    init {
        loadWorkOrders()
    }

    fun loadWorkOrders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val r = repo.list()) {
                is EdgeResult.Success -> _state.value = _state.value.copy(
                    isLoading = false,
                    workOrders = r.data,
                )
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = r.error.message,
                )
            }
        }
    }

    fun submitWorkOrder(title: String, content: String, category: String = "file_download_cdn") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, error = null)
            when (val r = repo.submit(title, content, category)) {
                is EdgeResult.Success -> {
                    _state.value = _state.value.copy(isSubmitting = false)
                    loadWorkOrders()
                }
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isSubmitting = false,
                    error = r.error.message,
                )
            }
        }
    }

    fun loadDetail(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingDetail = true, error = null)
            when (val r = repo.detail(id)) {
                is EdgeResult.Success -> _state.value = _state.value.copy(
                    isLoadingDetail = false,
                    selectedWorkOrder = r.data,
                )
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isLoadingDetail = false,
                    error = r.error.message,
                )
            }
        }
    }

    fun replyToWorkOrder(id: Long, content: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmittingReply = true, error = null)
            when (val r = repo.reply(id, content)) {
                is EdgeResult.Success -> {
                    _state.value = _state.value.copy(isSubmittingReply = false)
                    loadDetail(id)
                }
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isSubmittingReply = false,
                    error = r.error.message,
                )
            }
        }
    }

    fun closeWorkOrder(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClosing = true, error = null)
            when (val r = repo.updateStatus(id, "closed")) {
                is EdgeResult.Success -> {
                    _state.value = _state.value.copy(isClosing = false)
                    loadWorkOrders()
                    // Also refresh detail if it's the same work order
                    if (_state.value.selectedWorkOrder?.id == id) {
                        loadDetail(id)
                    }
                }
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isClosing = false,
                    error = r.error.message,
                )
            }
        }
    }

    fun clearSelectedWorkOrder() {
        _state.value = _state.value.copy(selectedWorkOrder = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
