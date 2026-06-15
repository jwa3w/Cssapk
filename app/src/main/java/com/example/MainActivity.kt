package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.local.GigDatabase
import com.example.data.repository.GigRepository
import com.example.ui.screens.MainScreen
import com.example.ui.viewmodel.GigViewModel

class MainActivity : ComponentActivity() {
    private lateinit var database: GigDatabase
    private lateinit var repository: GigRepository
    private lateinit var viewModel: GigViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Room local database persistence securely 
        database = Room.databaseBuilder(
            applicationContext,
            GigDatabase::class.java,
            "craigslist_gigs_database"
        ).fallbackToDestructiveMigration().build()
        
        repository = GigRepository(database.bookmarkDao())
        
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(GigViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return GigViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        })[GigViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MainScreen(viewModel = viewModel)
        }
    }
}
