package com.zelgius.wear.greenhousesensor

import android.app.Application
import android.system.Os.bind
import com.zelgius.greenhousesensor.common.repository.DataStoreRepository
import com.zelgius.greenhousesensor.common.repository.DataStoreRepositoryImpl
import com.zelgius.greenhousesensor.common.repository.MockDataStoreRepository
import com.zelgius.greenhousesensor.common.repository.RecordRepository
import com.zelgius.greenhousesensor.common.repository.RecordRepositoryImpl
import com.zelgius.greenhousesensor.common.repository.MockRecordRepository
import com.zelgius.greenhousesensor.common.service.BleService
import com.zelgius.greenhousesensor.common.service.BleServiceImpl
import com.zelgius.greenhousesensor.common.service.MockBleService
import com.zelgius.greenhousesensor.common.ui.current_record.CurrentRecordViewModel
import com.zelgius.greenhousesensor.common.ui.home.HomeViewModel
import com.zelgius.greenhousesensor.common.ui.record_history.RecordHistoryViewModel
import com.zelgius.greenhousesensor.common.usecases.ConnectDeviceUseCase
import com.zelgius.greenhousesensor.common.usecases.GetCurrentRecordUseCase
import com.zelgius.greenhousesensor.common.usecases.GetRecordHistoryUseCase
import com.zelgius.wear.greenhousesensor.ui.FindDeviceViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.bind
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
    singleOf(::BleServiceImpl) { bind<BleService>() }
    singleOf(::RecordRepositoryImpl) { bind<RecordRepository>() }
    singleOf(::DataStoreRepositoryImpl) { bind<DataStoreRepository>() }
    //singleOf(::MockBleService) { bind<BleService>() }
    //singleOf(::MockRecordRepository) { bind<RecordRepository>() }
    //singleOf(::MockDataStoreRepository) { bind<DataStoreRepository>() }


    // Use cases
    factoryOf(::ConnectDeviceUseCase)
    factoryOf(::GetCurrentRecordUseCase)
    factoryOf(::GetRecordHistoryUseCase)

    // ViewModels
    viewModelOf(::RecordHistoryViewModel)
    viewModelOf(::CurrentRecordViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::FindDeviceViewModel)
}