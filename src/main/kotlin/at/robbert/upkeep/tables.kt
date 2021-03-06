package at.robbert.upkeep

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.CurrentDateTime
import org.jetbrains.exposed.sql.`java-time`.datetime

data class Members(val members: List<Member>)
data class Member(val memberName: String)

object Chats : IdTable<Long>() {
    override val id = long("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val name = text("chatName")
    val members = jsonb<Members>("members")
    val lastMessage = datetime("lastMessage").defaultExpression(CurrentDateTime())
    val added = datetime("added").defaultExpression(CurrentDateTime())
    val enabled = bool("enabled").default(false)
}

enum class RedirectMethod {
    JAVASCRIPT, HTTP301, HTTP302
}

data class LinkToMonitor(val link:String, val redirectTo:String, val redirectMethod: RedirectMethod)

object LinksToMonitor : IntIdTable() {
    val link = text("link")
    val redirectTo = text("redirectTo")
    val redirectMethod = enumeration("redirectMethod", RedirectMethod::class)
}
