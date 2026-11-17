package com.javonpeart.ideacompanion

import android.app.Application
import androidx.room.Room
import com.javonpeart.ideacompanion.data.IdeaDatabase
import com.javonpeart.ideacompanion.repository.IdeaRepository
import com.javonpeart.ideacompanion.settings.SettingsRepository

class IdeaApp : Application() {
    val database by lazy { Room.databaseBuilder(this, IdeaDatabase::class.java, "ideas.db").build() }
    val repository by lazy { IdeaRepository(database.ideas()) }
    val settings by lazy { SettingsRepository(this) }
}
