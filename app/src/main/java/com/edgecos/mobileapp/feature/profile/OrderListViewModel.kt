package com.edgecos.mobileapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.Order
import com.edgecos.mobileapp.domain.model.Orders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderListViewModel : ViewModel() {
    private val billingRepo = ServiceLocator.billingRepository

    private val _state = MutableStateFlow(OrderListUiState())
    val state: StateFlow<OrderListUiState> = _state.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val r = billingRepo.getOrders()) {
                is EdgeResult.Success -> _state.value = _state.value.copy(
                    isLoading = false,
                    orders = r.data,
                )
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = r.error.message,
                )
            }
        }
    }

    /** 取消订单 */
    fun cancelOrder(tradeNo: String) {
        viewModelScope.launch {
            when (val r = billingRepo.cancelOrder(tradeNo)) {
                is EdgeResult.Success -> refresh()
                is EdgeResult.Failure -> _actionError.value = r.error.message
            }
        }
    }

    /**
     * 重新支付订单。
     *
     * @return 支付结果（成功时），调用方据此决定是否调起浏览器
     */
    suspend fun repayOrder(
        tradeNo: String,
        paymentMethod: String,
    ): com.edgecos.mobileapp.domain.model.PaymentResult? {
        return when (val r = billingRepo.repayOrder(tradeNo, paymentMethod)) {
            is EdgeResult.Success -> {
                refresh()
                r.data
            }
            is EdgeResult.Failure -> {
                _actionError.value = r.error.message
                null
            }
        }
    }

    /** 查询支付状态 */
    suspend fun queryPayment(tradeNo: String): com.edgecos.mobileapp.domain.model.PaymentStatus? {
        return when (val r = billingRepo.queryPayment(tradeNo)) {
            is EdgeResult.Success -> r.data
            is EdgeResult.Failure -> {
                _actionError.value = r.error.message
                null
            }
        }
    }

    fun consumeActionError() {
        _actionError.value = null
    }
}

data class OrderListUiState(
    val isLoading: Boolean = false,
    val orders: Orders = Orders(emptyList(), com.edgecos.mobileapp.domain.model.OrdersSummary(0, 0, 0, 0, 0)),
    val error: String? = null,
)
