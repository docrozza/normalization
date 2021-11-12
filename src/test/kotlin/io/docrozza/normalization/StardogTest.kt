package io.docrozza.normalization

import com.complexible.stardog.Stardog
import com.complexible.stardog.api.Connection
import com.complexible.stardog.api.admin.AdminConnectionConfiguration
import com.complexible.stardog.db.DatabaseOptions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterEach as after
import org.junit.jupiter.api.BeforeEach as before
import java.util.function.Supplier

abstract class StardogTest {

    @Suppress("unused")
    companion object {
        private val DB = StardogTest::class.simpleName
        private const val ADMIN = "admin"

        private lateinit var stardog: Stardog

        @BeforeAll
        @JvmStatic internal fun setup() {
            stardog = Stardog.builder().create()
        }

        @AfterAll
        @JvmStatic internal fun teardown() {
            stardog.shutdown()
        }
    }

    protected lateinit var connectionFactory: Supplier<Connection>

    @before fun setup() {
        val db = AdminConnectionConfiguration.toEmbeddedServer()
            .credentials(ADMIN, ADMIN)
            .connect()
            .use {
                if (it.list().contains(DB)) {
                    it.drop(DB)
                }

                it.newDatabase(DB).set(DatabaseOptions.PRESERVE_BNODE_IDS, true).create()
            }

        connectionFactory = Supplier { db.connect() }
    }

    @after fun teardown() {
        AdminConnectionConfiguration.toEmbeddedServer()
            .credentials(ADMIN, ADMIN)
            .connect()
            .use {
                if (it.list().contains(DB)) {
                    it.drop(DB)
                }
            }
    }

    protected fun <R> Supplier<Connection>.use(block: (Connection) -> R) : R {
        return get().use {
            try {
                it.begin()
                val result = block(it)
                it.commit()
                result
            } catch (error: Exception) {
                it.rollback()
                throw error
            }
        }
    }
}