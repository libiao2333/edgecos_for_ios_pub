package com.edgecos.mobileapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.MyPlan
import com.edgecos.mobileapp.domain.model.PaymentResult
import com.edgecos.mobileapp.domain.model.Plan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlanViewModel : ViewModel() {
    private val billingRepo = ServiceLocator.billingRepository

    private val _state = MutableStateFlow(PlanUiState())
    val state: StateFlow<PlanUiState> = _state.asStateFlow()

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult: StateFlow<PaymentResult?> = _paymentResult.asStateFlow()

    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError: StateFlow<String?> = _paymentError.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        loadMyPlan()
        loadPlans()
    }

    fun loadMyPlan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(myPlanLoading = true)
            when (val r = billingRepo.getMyPlan()) {
                is EdgeResult.Success -> _state.value = _state.value.copy(
                    myPlanLoading = false,
                    myPlan = r.data,
                )
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    myPlanLoading = false,
                    myPlanError = r.error.message,
                )
            }
        }
    }

    fun loadPlans() {
        viewModelScope.launch {
            _state.value = _state.value.copy(plansLoading = true)
            when (val r = billingRepo.getPlans()) {
                is EdgeResult.Success -> _state.value = _state.value.copy(
                    plansLoading = false,
                    plans = r.data,
                )
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    plansLoading = false,
                    plansError = r.error.message,
                )
            }
        }
    }

    /**
     * 创建支付订单。
     *
     * @param planId        套餐 ID
     * @param paymentMethod 支付方式："alipay" / "wxpay" / "balance"
     * @param billingMonths 计费月数：1 / 3 / 12
     */
    fun createPayment(planId: Long, paymentMethod: String, billingMonths: Long = 1) {
        viewModelScope.launch {
            _paymentError.value = null
            _paymentResult.value = null
            _state.value = _state.value.copy(paying = true)
            when (val r = billingRepo.createPayment(planId, paymentMethod, billingMonths)) {
                is EdgeResult.Success -> {
                    _state.value = _state.value.copy(paying = false)
                    _paymentResult.value = r.data
                    // 支付成功后刷新套餐状态
                    if (r.data.isPaid) loadMyPlan()
                }
                is EdgeResult.Failure -> {
                    _state.value = _state.value.copy(paying = false)
                    _paymentError.value = r.error.message
                }
            }
        }
    }

    fun consumePaymentResult() {
        _paymentResult.value = null
        _paymentError.value = null
    }
}

data class PlanUiState(
    val myPlanLoading: Boolean = false,
    val myPlan: MyPlan? = null,
    val myPlanError: String? = null,
    val plansLoading: Boolean = false,
    val plans: List<Plan> = emptyList(),
    val plansError: String? = null,
    val paying: Boolean = false,
)
