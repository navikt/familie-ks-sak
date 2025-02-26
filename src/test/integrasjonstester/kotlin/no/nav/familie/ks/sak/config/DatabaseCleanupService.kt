package no.nav.familie.ks.sak.config

import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.relational.core.mapping.RelationalMappingContext
import org.springframework.data.relational.core.sql.IdentifierProcessing
import org.springframework.stereotype.Service
import kotlin.reflect.full.findAnnotation
import org.springframework.data.relational.core.mapping.Table as JdbcTable

@Service
@Profile("dev", "postgres", "integrasjonstest")
class DatabaseCleanupService(
    private val entityManager: EntityManager,
    private val relationalMappingContext: RelationalMappingContext,
) {
    private val logger = LoggerFactory.getLogger(DatabaseCleanupService::class.java)

    private var tableNames: List<String>? = null
        /**
         * Uses the JPA metamodel to find all managed types then try to get the [Table] annotation's from each (if present) to discover the table name.
         * If the [Table] annotation is not defined then we skip that entity (oops :p)
         * JDBC tables must be found out in another way
         */
        get() {
            if (field == null) {
                val metaModel = entityManager.metamodel
                field = metaModel.managedTypes
                    .filter {
                        it.javaType.kotlin.findAnnotation<Table>() != null || it.javaType.kotlin.findAnnotation<JdbcTable>() != null
                    }.map {
                        val tableAnnotation: Table? = it.javaType.kotlin.findAnnotation()
                        val jdbcTableAnnotation: JdbcTable? = it.javaType.kotlin.findAnnotation()
                        tableAnnotation?.name ?: jdbcTableAnnotation!!.value
                    } + getJdbcTableNames()
            }
            return field
        }

    private fun getJdbcTableNames(): List<String> = relationalMappingContext.persistentEntities.map { it.tableName.toSql(IdentifierProcessing.NONE) }

    /**
     * Utility method that truncates all identified tables
     */
    @Transactional
    fun truncate() {
        logger.info("Truncating tables: $tableNames")
        entityManager.flush()
        tableNames?.forEach {
            retryFunksjon(antallGanger = 2) {
                entityManager.createNativeQuery("TRUNCATE TABLE $it CASCADE").executeUpdate()
            }
        }
    }

    private fun <T> retryFunksjon(
        antallGanger: Int = 2,
        forsinkelseIms: Long = 1000,
        funksjon: () -> T,
    ): T? {
        repeat(antallGanger - 1) {
            try {
                return funksjon()
            } catch (e: Exception) {
                logger.error("Funksjon kjørte med feil", e)
            }
            Thread.sleep(forsinkelseIms)
        }
        return funksjon()
    }
}
