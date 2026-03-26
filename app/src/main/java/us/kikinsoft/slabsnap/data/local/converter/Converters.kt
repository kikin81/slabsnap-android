package us.kikinsoft.slabsnap.data.local.converter

import androidx.room.TypeConverter
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromMap(value: Map<String, String>): String = JSONObject(value).toString()

    @TypeConverter
    fun toMap(value: String): Map<String, String> {
        val json = JSONObject(value)
        return json.keys().asSequence().associateWith { json.getString(it) }
    }
}
