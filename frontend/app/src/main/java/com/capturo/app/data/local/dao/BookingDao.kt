package com.capturo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.capturo.app.data.local.entity.BookingEntity

@Dao
interface BookingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingEntity>)

    @Query("SELECT * FROM bookings")
    suspend fun getBookings(): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE status = :status")
    suspend fun getBookingsByStatus(status: String): List<BookingEntity>

    @Query("DELETE FROM bookings")
    suspend fun deleteBookings()
}
