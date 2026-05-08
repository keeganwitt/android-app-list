package com.github.keeganwitt.applist.db

import androidx.room.TypeConverter
import com.github.keeganwitt.applist.AppInfoField

class Converters {
    @TypeConverter
    fun fromAppInfoFieldSet(value: Set<AppInfoField>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun toAppInfoFieldSet(value: String): Set<AppInfoField> {
        if (value.isEmpty()) return emptySet()
        return value
            .split(",")
            .mapNotNull {
                try {
                    AppInfoField.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()
    }
}
