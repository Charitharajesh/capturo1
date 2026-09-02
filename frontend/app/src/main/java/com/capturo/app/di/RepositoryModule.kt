package com.capturo.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Note: Since this codebase directly injects concrete Repository classes 
    // (which are annotated with @Singleton and @Inject constructor), Hilt provides
    // them out-of-the-box. This module is prepared for future @Binds declarations 
    // when repository interfaces are introduced in the domain layer.
}
