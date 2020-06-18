package at.robbert.upkeep

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

open class JsonBColumnType<T : Any>(private val clazz: Class<T>) : ColumnType() {
    companion object {
        private val gson = Gson()
    }

    override fun sqlType(): String = "jsonb"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val obj = PGobject()
        obj.type = "jsonb"
        obj.value = value as String
        stmt[index] = obj
    }

    override fun valueFromDB(value: Any): T {
        value as PGobject
        return gson.fromJson(value.value, clazz)
    }

    override fun notNullValueToDB(value: Any): Any = gson.toJson(value)
    override fun nonNullValueToString(value: Any): String = "'${notNullValueToDB(value)}'"
}

inline fun <reified T : Any> Table.jsonb(name: String): Column<T> = registerColumn(name, JsonBColumnType(T::class.java))
