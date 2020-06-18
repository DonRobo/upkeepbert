package at.robbert.upkeep

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.*
import com.pengrad.telegrambot.model.Chat.Type.*
import com.pengrad.telegrambot.model.request.ForceReply
import com.pengrad.telegrambot.request.GetChatAdministrators
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SetMyCommands
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

data class AwaitingReply(val chatId: Long, val messageId: Int)

class UpkeepBot : CoroutineScope {
    private val commands = listOf(
        MonitorLinkCommand,
        UnmonitorLinkCommand,
        ListLinksCommand,
        DemoCommand
    )

    private val job = Job()
    override val coroutineContext: CoroutineContext get() = job + Dispatchers.Default
    private val database = Database.connect(
        url = ConfigLoader.config.database,
        driver = "org.postgresql.Driver",
        user = ConfigLoader.config.databaseUser,
        password = ConfigLoader.config.databasePassword
    )
    private val bot = TelegramBot(ConfigLoader.config.botToken)
    private val awaitingReplies = ConcurrentHashMap<AwaitingReply, Pair<LocalDateTime, CompletableFuture<String>>>()

    fun start() {
        transaction {
            SchemaUtils.create(Chats)
        }
        runBlocking {
            val setCommands = bot.awaitExecution(
                SetMyCommands(
                    *commands.map { BotCommand(it.command, it.description) }.toTypedArray()
                )
            )
            if (!setCommands.response.isOk) {
                println("Couldn't set commands! $setCommands")
                exitProcess(1)
            }
            bot.setUpdatesListener {
                it.forEach {
                    handleUpdate(it)
                }

                return@setUpdatesListener UpdatesListener.CONFIRMED_UPDATES_ALL
            }
            println("Bot started!")
        }
    }

    private fun handleUpdate(update: Update) {
//        println("Received update: $update")
        launch {
            update.message()?.let { updateChat(it) }
            if (update.message()?.chat()?.id()?.let { isChatEnabled(it) } == true && update.message()
                    .replyToMessage() != null) {
                val awaitingReply = awaitingReplies.remove(
                    AwaitingReply(
                        update.message().chat().id(),
                        update.message().replyToMessage().messageId()
                    )
                )
                awaitingReply?.second?.complete(update.message().text())
                awaitingReplies.filterValues { it.first.isAfter(now()) }.forEach { (k, _) ->
                    awaitingReplies.remove(k)
                }
            } else if (update.message()?.chat()?.id()?.let { isChatEnabled(it) } == true && update.message().entities()
                    ?.any { it.type() == MessageEntity.Type.bot_command } == true) {
                val command = commands.singleOrNull { it.command == update.message().text().substring(1) }
                if (command == null) {
                    bot.execute(SendMessage(update.message().chat().id(), "Invalid command!"))
                } else {
                    val params = command.parameters.map {
                        it.first to askUser(update.message().chat().id(), update.message().messageId(), it.second)
                    }.toMap()
                    command.execute(bot, update, params)
                }
            }
        }
    }

    private suspend fun askUser(chatId: Long, originalMessageId: Int, question: String): String {
        val myMessage = bot.awaitExecution(
            SendMessage(chatId, question)
                .replyMarkup(ForceReply(true))
                .replyToMessageId(originalMessageId)
        )
        return awaitReply(chatId, myMessage.response.message().messageId())
    }

    private suspend fun awaitReply(chatId: Long, messageId: Int, validity: Duration = Duration.ofHours(1)): String {
        val future = CompletableFuture<String>()
        awaitingReplies[AwaitingReply(chatId, messageId)] = now() + validity to future
        return future.await()
    }

    private fun isChatEnabled(chatId: Long): Boolean {
        return transaction {
            Chats.select { Chats.enabled.eq(true) and Chats.id.eq(chatId) }.count() > 0
        }
    }

    private suspend fun updateChat(msg: Message) {
        val chat = msg.chat() ?: return
        val members = when (chat.type()!!) {
            Private -> listOf(msg.chat().fullName())
            channel, supergroup, group -> bot.awaitExecution(GetChatAdministrators(chat.id()))
                .response
                .administrators()
                .map { it.user().fullName() }

        }
        val chatName = when (msg.chat().type()!!) {
            Private -> members.single()
            channel, supergroup, group -> msg.chat().title()
        }
        println("Chat $chatName has members $members")
        val chatId = chat.id()!!

        val chatMembersDto = Members(members.map { memberName -> Member(memberName) })

        transaction {
            val foundChats = Chats.select { Chats.id.eq(chatId) }.count()
            if (foundChats > 0) {
                Chats.update(where = {
                    Chats.id.eq(chatId)
                }, body = {
                    it[Chats.members] = chatMembersDto
                    it[lastMessage] = now()
                    it[name] = chatName
                })
            } else {
                Chats.insert {
                    it[id] = EntityID(chatId, Chats)
                    it[name] = chatName
                    it[Chats.members] = chatMembersDto
                }
            }
            Unit
        }
    }
}
