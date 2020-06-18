package at.robbert.upkeep

import at.robbert.upkeep.LinkChecker.checkLink
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

interface UpkeepBotCommand {
    val command: String
    val description: String
    val parameters: List<Pair<String, String>>

    suspend fun execute(bot: UpkeepBot, originalCommand: Update, params: Map<String, String>, reply: (String) -> Unit)
}

object MonitorLinkCommand : UpkeepBotCommand {
    override val command get() = "monitor_link"
    override val description get() = "Add a link to be monitored"
    override val parameters get() = listOf("link" to "Please enter link to be monitored")

    override suspend fun execute(
        bot: UpkeepBot,
        originalCommand: Update,
        params: Map<String, String>,
        reply: (String) -> Unit
    ) {
        val link = params["link"] ?: error("No link param? $params")
        val checked = checkLink(link)
        log.debug("Checked=$checked")
        if (checked != null)
            transaction {
                LinksToMonitor.insert {
                    it[LinksToMonitor.link] = link
                    it[redirectTo] = checked.redirectsTo
                    it[redirectMethod] = checked.redirectMethod
                }
                reply("Successfully added $link linking to ${checked.redirectsTo} using ${checked.redirectMethod}")
            }
        else
            reply("Link couldn't be accessed!")
    }
}

object UnmonitorLinkCommand : UpkeepBotCommand {
    override val command get() = "unmonitor_link"
    override val description get() = "Stop a link from being monitored"
    override val parameters: List<Pair<String, String>>
        get() = emptyList()

    override suspend fun execute(
        bot: UpkeepBot,
        originalCommand: Update,
        params: Map<String, String>,
        reply: (String) -> Unit
    ) {
        val linksList = transaction {
            LinksToMonitor.selectAll().map {
                it[LinksToMonitor.link]
            }
        }
        val linksListText = linksList.mapIndexed { index, s -> index to s }.joinToString("\n") { (index, link) ->
            "\t(${index + 1})\t$link"
        }
        val linkSelected = bot.askUser(
            originalCommand.message().chat().id(),
            originalCommand.message().messageId(),
            "Which link do you want to unmonitor?\n$linksListText"
        )
        val linkIndex = linkSelected.toIntOrNull()?.minus(1)
        if (linkIndex != null && linkIndex in linksList.indices) {
            val deleted = transaction {
                LinksToMonitor.deleteWhere {
                    LinksToMonitor.link.eq(linksList[linkIndex])
                }
            }
            if (deleted == 1)
                reply("Successfully deleted ${linksList[linkIndex]}")
            else
                reply("Deletion went wrong, deleted $deleted links instead!")
        } else {
            reply("Invalid selection: \"$linkSelected\"")
        }
    }
}

object ListLinksCommand : UpkeepBotCommand {
    override val command get() = "list_links"
    override val description get() = "List links currently being monitored"
    override val parameters: List<Pair<String, String>>
        get() = emptyList()

    override suspend fun execute(
        bot: UpkeepBot,
        originalCommand: Update,
        params: Map<String, String>,
        reply: (String) -> Unit
    ) {
        val links = transaction {
            LinksToMonitor.selectAll().map {
                LinkToMonitor(
                    link = it[LinksToMonitor.link],
                    redirectMethod = it[LinksToMonitor.redirectMethod],
                    redirectTo = it[LinksToMonitor.redirectTo]
                )
            }
        }
        bot.bot.execute(
            SendMessage(
                originalCommand.message().chat().id(),
                if (links.isEmpty())
                    "No links being monitored right now"
                else
                    "Links being monitored:\n${links.joinToString("\n") {
                        "\t${it.link} should redirect to ${it.redirectTo} using ${it.redirectMethod}"
                    }}"
            )
        )
    }
}

object DemoCommand : UpkeepBotCommand {
    override val command get() = "demo_test"
    override val description get() = "Do parameters test"
    override val parameters
        get() = listOf(
            "param1" to "Please enter first parameter",
            "param2" to "Please enter second parameter"
        )

    override suspend fun execute(
        bot: UpkeepBot,
        originalCommand: Update,
        params: Map<String, String>,
        reply: (String) -> Unit
    ) {
        reply("Got parameters: $params")
    }
}
