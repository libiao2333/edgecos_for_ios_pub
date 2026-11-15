package com.edgecos.mobileapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.MyPlan
import com.edgecos.mobileapp.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val billingRepo = ServiceLocator.billingRepository

    val user: StateFlow<User?> = ServiceLocator.authRepository.currentUser

    private val _myPlan = MutableStateFlow<MyPlan?>(null)
    val myPlan: StateFlow<MyPlan?> = _myPlan.asStateFlow()

    private val _planLoading = MutableStateFlow(false)
    val planLoading: StateFlow<Boolean> = _planLoading.asStateFlow()

    init {
        loadMyPlan()
    }

    /** 加载当前套餐（含存储与流量用量），用于个人页顶部卡片展示 */
    fun loadMyPlan() {
        viewModelScope.launch {
            _planLoading.value = true
            when (val r = billingRepo.getMyPlan()) {
                is EdgeResult.Success -> _myPlan.value = r.data
                is EdgeResult.Failure -> Unit
            }
            _planLoading.value = false
        }
    }
}
