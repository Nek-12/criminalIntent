package com.example.criminalIntent

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*
import kotlin.random.Random


@Entity
data class Crime(
    @PrimaryKey var id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isSolved: Boolean = false,
    var requiresPolice: Boolean = false,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Crime

        if (id != other.id) return false
        if (title != other.title) return false
        if (date != other.date) return false
        if (isSolved != other.isSolved) return false
        if (requiresPolice != other.requiresPolice) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + isSolved.hashCode()
        result = 31 * result + requiresPolice.hashCode()
        return result
    }

}

