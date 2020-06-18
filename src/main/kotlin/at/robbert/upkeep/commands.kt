package at.robbert.upkeep

import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage

interface UpkeepBotCommand {
    val command: String
    val description: String
    val parameters: List<Pair<String, String>>

    fun execute(bot: UpkeepBot, originalCommand: Update, params: Map<String, String>)
}

object MonitorLinkCommand : UpkeepBotCommand {
    override val command get() = "monitor_link"
    override val description get() = "Add a link to be monitored"
    override val parameters: List<Pair<String, String>>
        get() = emptyList()

    override fun execute(
        bot: UpkeepBot,
        originalCommand: Update,
        params: Map<String, String>
    ) {
        log.info("Executing monitor_link")
    }
}

object UnmonitorLinkCommand : UpkeepBotCommand {
    override val command get() = "unmonitor_link"
    override val description get() = "Stop a link from being monitored"
    override val parameters: List<Pair<String, String>>
        get() = emptyList()

    override fun execute(
        bot: UpkeepBot,
        originalCommand: Update,
        params: Map<String, String>
    ) {
        log.info("Executing unmonitor_link")
    }
}

object ListLinksCommand : UpkeepBotCommand {
    override val command get() = "list_links"
    override val description get() = "List links currently being monitored"
    override val parameters: List<Pair<String, String>>
        get() = emptyList()

    override fun execute(
        bot: UpkeepBot,
        originalCommand: Update,
        params: Map<String, String>
    ) {
        log.info("Executing list_links")
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

    override fun execute(
        bot: UpkeepBot,
        originalCommand: Update,
        params: Map<String, String>
    ) {
        bot.bot.execute(SendMessage(originalCommand.message().chat().id(), "Got parameters: $params"))
    }
}
