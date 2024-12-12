// di/AppModule.kt
package com.ibkpoc.amn.di

import com.ibkpoc.amn.network.ApiService
import com.ibkpoc.amn.repository.MeetingRepository
import com.ibkpoc.amn.repository.MeetingRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideMeetingRepository(apiService: ApiService): MeetingRepository {
        return MeetingRepositoryImpl(apiService)
    }
}