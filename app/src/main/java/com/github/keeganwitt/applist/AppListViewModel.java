package com.github.keeganwitt.applist;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppListViewModel extends AndroidViewModel {
    private final AppInfoRepository repository;
    private final MutableLiveData<List<AppInfo>> appList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AppListViewModel(Application application) {
        super(application);
        repository = new AppInfoRepository(application);
    }

    public LiveData<List<AppInfo>> getAppList() {
        return appList;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void loadApps(AppInfoField field, boolean showSystem, boolean descending, boolean reload) {
        isLoading.setValue(true);
        executor.execute(() -> {
            List<AppInfo> result = repository.getAppList(field, showSystem, descending, reload);
            appList.postValue(result);
            isLoading.postValue(false);
        });
    }
}
