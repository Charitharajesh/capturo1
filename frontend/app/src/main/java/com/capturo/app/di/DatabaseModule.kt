package com.capturo.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.capturo.app.data.local.AppDatabase
import com.capturo.app.data.local.dao.BookingDao
import com.capturo.app.data.local.dao.MessageDao
import com.capturo.app.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "capturo_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao {
        return db.userDao()
    }

    @Provides
    @Singleton
    fun provideBookingDao(db: AppDatabase): BookingDao {
        return db.bookingDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao {
        return db.messageDao()
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val prefsName = "capturo_secured_prefs"
        return try {
            createEncryptedPrefs(context, prefsName)
        } catch (e: Exception) {
            // Common after reinstalling the APK or an auto-backup restore: the encrypted
            // prefs file survives but its Keystore master key no longer matches, so
            // decryption throws and the app would crash on launch. Wipe the corrupt
            // prefs + master key and retry once; fall back to plain prefs as a last resort.
            Timber.e(e, "EncryptedSharedPreferences unavailable — resetting corrupt store")
            deleteCorruptPrefs(context, prefsName)
            try {
                createEncryptedPrefs(context, prefsName)
            } catch (e2: Exception) {
                Timber.e(e2, "Encrypted prefs still failing — falling back to plain prefs")
                context.getSharedPreferences("${prefsName}_plain", Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(context: Context, name: String): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            name,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun deleteCorruptPrefs(context: Context, name: String) {
        try {
            context.deleteSharedPreferences(name)
        } catch (_: Exception) {
        }
        try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry("_androidx_security_master_key_")
        } catch (_: Exception) {
        }
    }
}
