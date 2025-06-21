package com.zelgius.greenhousesensor

import android.app.Application
import com.zelgius.greenhousesensor.common.repository.DataStoreRepository
import com.zelgius.greenhousesensor.common.repository.RecordRepository
import com.zelgius.greenhousesensor.common.service.BleService
import com.zelgius.greenhousesensor.common.ui.current_record.CurrentRecordViewModel
import com.zelgius.greenhousesensor.ui.FindDeviceViewModel
import com.zelgius.greenhousesensor.common.ui.home.HomeViewModel
import com.zelgius.greenhousesensor.common.ui.record_history.RecordHistoryViewModel
import com.zelgius.greenhousesensor.common.usecases.ConnectDeviceUseCase
import com.zelgius.greenhousesensor.common.usecases.GetCurrentRecordUseCase
import com.zelgius.greenhousesensor.common.usecases.GetRecordHistoryUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }
}

val appModule = module {
    // Service / Repository
    singleOf(::BleService)
    singleOf(::DataStoreRepository)
    singleOf(::RecordRepository)

    // Use cases
    factoryOf(::ConnectDeviceUseCase)
    factoryOf(::GetCurrentRecordUseCase)
    factoryOf(::GetRecordHistoryUseCase)

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::FindDeviceViewModel)
    viewModelOf(::CurrentRecordViewModel)
    viewModelOf(::RecordHistoryViewModel)
}