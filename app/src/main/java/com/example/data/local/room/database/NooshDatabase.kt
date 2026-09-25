package com.example.data.local.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.room.dao.DailyWaterSummaryDao
import com.example.data.local.room.dao.GamificationDao
import com.example.data.local.room.dao.HealthAlertEventDao
import com.example.data.local.room.dao.HealthCompanionDao
import com.example.data.local.room.dao.ReminderDao
import com.example.data.local.room.dao.SyncOutboxDao
import com.example.data.local.room.dao.UserProfileDao
import com.example.data.local.room.dao.WaterIntakeDao
import com.example.data.local.room.entity.DailyWaterSummaryEntity
import com.example.data.local.room.entity.GamificationBadgeEntity
import com.example.data.local.room.entity.HealthAlertEventEntity
import com.example.data.local.room.entity.HealthCompanionEntity
import com.example.data.local.room.entity.ReminderEntity
import com.example.data.local.room.entity.SyncOutboxEntity
import com.example.data.local.room.entity.UserProfileEntity
import com.example.data.local.room.entity.WaterIntakeEntity
import com.example.domain.gamification.GamificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        WaterIntakeEntity::class,
        ReminderEntity::class,
        DailyWaterSummaryEntity::class,
        HealthAlertEventEntity::class,
        HealthCompanionEntity::class,
        SyncOutboxEntity::class,
        GamificationBadgeEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class NooshDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun waterIntakeDao(): WaterIntakeDao
    abstract fun reminderDao(): ReminderDao
    abstract fun dailyWaterSummaryDao(): DailyWaterSummaryDao
    abstract fun healthAlertEventDao(): HealthAlertEventDao
    abstract fun healthCompanionDao(): HealthCompanionDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun gamificationDao(): GamificationDao

    companion object {
        @Volatile
        private var INSTANCE: NooshDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope? = null): NooshDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NooshDatabase::class.java,
                    "noosh_water.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            scope?.launch(Dispatchers.IO) {
                                INSTANCE?.let { database ->
                                    val defaultProfile = UserProfileEntity(
                                        id = "default_user",
                                        name = "کاربر گرامی",
                                        email = "user@noosh.app",
                                        dailyWaterGoalMl = 2000,
                                        reminderIntervalMinutes = 60,
                                        reminderEnabled = true,
                                        wakeUpTime = "08:00",
                                        sleepTime = "23:00"
                                    )
                                    database.userProfileDao().insertOrUpdateProfile(defaultProfile)

                                    val badgeEntities = GamificationManager.defaultBadges.map {
                                        GamificationBadgeEntity.fromDomain(it)
                                    }
                                    database.gamificationDao().insertBadgesIfAbsent(badgeEntities)
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
