package com.github.keeganwitt.applist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AppListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppInfoRepository = AppInfoRepository(application)
    val appList = MutableLiveData<List<AppInfo>>()
    val isLoading = MutableLiveData<Boolean>()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun loadApps(field: AppInfoField, showSystem: Boolean, descending: Boolean, reload: Boolean) {
        isLoading.value = true
        executor.execute {
            val result: List<AppInfo> = repository.getAppList(field, showSystem, descending, reload)
            appList.postValue(result)
            isLoading.postValue(false)
        }
    }
}
