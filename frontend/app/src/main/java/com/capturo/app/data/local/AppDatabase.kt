package com.capturo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.capturo.app.data.local.dao.*
import com.capturo.app.data.local.entity.*

@Database(entities = [UserEntity::class, BookingEntity::class, MessageEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bookingDao(): BookingDao
    abstract fun messageDao(): MessageDao
}
