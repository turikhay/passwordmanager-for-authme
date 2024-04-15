package com.turikhay.mc.pwam.mc

import com.turikhay.mc.pwam.common.*
import com.turikhay.mc.pwam.common.text.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.Executor

class Session(
    client: IClient,
    private val info: SessionInfo,
    private val db: Database,
    executor: Executor,
    commandNodeAccessor: ICommandNodeAccessor<ICommandSource>,
    askServerSuggestion: IAskServerSuggestion,
    pattern: PasswordPattern,
) : PasswordChangeCallback, Invalidatable, Disposable {

    private val registerPwdProvider = CacheableTextProvider(
        provideText("register", executor) {
            generatePassword(12)
        }
    )

    private val registerPwdPatternProvider = CacheableTextProvider(
        pattern.providePwdPattern(registerPwdProvider)
    )

    private val loginPwdProvider = CacheableTextProvider(
        PatternAwarePasswordGenerator(
            provideText("login", executor) {
                transaction(db) {
                    getPassword(info.username, info.server)
                }
            },
            pattern,
            registerPwdPatternProvider,
            maxAttempts = 2,
        )
    )

    private val loginPwdPatternProvider = CacheableTextProvider(
        pattern.providePwdPattern(loginPwdProvider)
    )

    val pwdPatternCache = TextProvidersCache(
        listOf(
            loginPwdPatternProvider,
            registerPwdPatternProvider,
        )
    )

    private val pairs = Pairs(
        login = PasswordPairProvider(
            loginPwdProvider,
            loginPwdPatternProvider,
        ),
        register = PasswordPairProvider(
            registerPwdProvider,
            registerPwdPatternProvider,
        ),
    )

    private val notificator = Notificator(
        client,
        pattern
    )

    val commandRewriter = PatternCommandRewriter(
        pairs.asList(),
        notificator,
        pattern,
    )

    val commandDispatcherHandler = CommandDispatcherHandler(
        KnownCommandHandler(
            client,
            commandRewriter,
            pairs,
            this,
            notificator,
            pattern,
        ),
        pairs,
        commandNodeAccessor,
        askServerSuggestion,
        ManagementCmd(
            db,
            pattern,
            info,
            client,
            this,
            client.version,
        ),
        client,
        executor,
    )

    override fun changePassword(newPassword: String) {
        transaction(db) {
            setPassword(
                info.username,
                info.server,
                newPassword,
            )
        }
        invalidate()
        notificator.passwordChangeNotification(newPassword)
    }

    override fun invalidate() {
        invalidateAll(
            loginPwdProvider,
            loginPwdPatternProvider,
            registerPwdProvider,
            registerPwdPatternProvider,
            pwdPatternCache,
        )
    }

    override fun cleanUp() {
        invalidate()
    }

    companion object {
        var session: Session? = null
    }
}