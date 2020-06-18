package at.robbert.upkeep

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import java.io.File

data class Config(
    val botToken: String,
    val database: String,
    val databaseUser: String,
    val databasePassword: String
)

object ConfigLoader {
    val config: Config = Gson().fromJson(File("config.json").readText())
}
