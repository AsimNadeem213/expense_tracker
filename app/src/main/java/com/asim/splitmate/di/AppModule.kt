package com.asim.splitmate.di

import androidx.room.Room
import com.asim.splitmate.core.common.Constants
import com.asim.splitmate.core.common.DefaultDispatchersProvider
import com.asim.splitmate.core.common.DispatchersProvider
import com.asim.splitmate.core.database.ExpenseMateDatabase
import com.asim.splitmate.core.firebase.RealtimeDatabaseDataSource
import com.asim.splitmate.data.repository.AuthRepositoryImpl
import com.asim.splitmate.data.repository.ExpenseRepositoryImpl
import com.asim.splitmate.data.repository.GroupRepositoryImpl
import com.asim.splitmate.data.repository.SettlementRepositoryImpl
import com.asim.splitmate.data.repository.UserRepositoryImpl
import com.asim.splitmate.domain.repository.AuthRepository
import com.asim.splitmate.domain.repository.ExpenseRepository
import com.asim.splitmate.domain.repository.GroupRepository
import com.asim.splitmate.domain.repository.SettlementRepository
import com.asim.splitmate.domain.repository.UserRepository
import com.asim.splitmate.domain.usecase.AddExpenseUseCase
import com.asim.splitmate.domain.usecase.CalculateGroupBalancesUseCase
import com.asim.splitmate.domain.usecase.GetDashboardDataUseCase
import com.asim.splitmate.feature.auth.AuthViewModel
import com.asim.splitmate.feature.balances.BalancesViewModel
import com.asim.splitmate.feature.dashboard.DashboardViewModel
import com.asim.splitmate.feature.expenses.ExpenseViewModel
import com.asim.splitmate.feature.groups.GroupViewModel
import com.asim.splitmate.feature.profile.ProfileViewModel
import com.asim.splitmate.feature.settlements.SettlementViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Dispatchers
    single<DispatchersProvider> { DefaultDispatchersProvider() }

    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            ExpenseMateDatabase::class.java,
            Constants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    // DAOs
    single { get<ExpenseMateDatabase>().userDao() }
    single { get<ExpenseMateDatabase>().groupDao() }
    single { get<ExpenseMateDatabase>().expenseDao() }
    single { get<ExpenseMateDatabase>().settlementDao() }

    // Remote (Firebase Realtime Database)
    single { RealtimeDatabaseDataSource() }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<GroupRepository> { GroupRepositoryImpl(get(), get(), get(), get(), get()) }
    single<ExpenseRepository> { ExpenseRepositoryImpl(get(), get()) }
    single<SettlementRepository> { SettlementRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }

    // Use Cases
    factory { AddExpenseUseCase(get()) }
    factory { CalculateGroupBalancesUseCase(get(), get(), get()) }
    factory { GetDashboardDataUseCase(get(), get(), get()) }

    // ViewModels
    viewModel { AuthViewModel(get()) }
    viewModel { DashboardViewModel(get(), get(), get()) }
    viewModel { GroupViewModel(get(), get(), get(), get()) }
    viewModel { ExpenseViewModel(get(), get(), get(), get()) }
    viewModel { BalancesViewModel(get(), get(), get()) }
    viewModel { SettlementViewModel(get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
}
