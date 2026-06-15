package com.example.data.local

import androidx.room.*
import com.example.model.CraigslistGig
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarked_gigs")
data class BookmarkedGig(
    @PrimaryKey val id: String,
    val title: String,
    val link: String,
    val description: String,
    val date: String,
    val city: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Extension methods for easy database conversions
fun BookmarkedGig.toGig(): CraigslistGig = CraigslistGig(
    id = id,
    title = title,
    link = link,
    description = description,
    date = date,
    city = city
)

fun CraigslistGig.toBookmark(): BookmarkedGig = BookmarkedGig(
    id = id,
    title = title,
    link = link,
    description = description,
    date = date,
    city = city
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarked_gigs ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedGig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(gig: BookmarkedGig)

    @Query("DELETE FROM bookmarked_gigs WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_gigs WHERE id = :id)")
    fun observeIsBookmarked(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_gigs WHERE id = :id)")
    suspend fun isBookmarkedSync(id: String): Boolean
}

@Database(entities = [BookmarkedGig::class], version = 1, exportSchema = false)
abstract class GigDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}
