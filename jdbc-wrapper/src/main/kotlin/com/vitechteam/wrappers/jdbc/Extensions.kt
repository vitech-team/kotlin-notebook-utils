package com.vitechteam.wrappers.jdbc

import java.sql.Connection

/**
 * Executes the query and returns exactly one row as a map of column label to value.
 *
 * Throws [IllegalStateException] if the result set is empty or contains more than one row.
 */
fun Connection.querySingle(sql: String): Map<String, Any?> {
    createStatement().use { stmt ->
        stmt.executeQuery(sql).use { rs ->
            check(rs.next()) { "Expected exactly one result row" }

            val metaData = rs.metaData
            val columnCount = metaData.columnCount
            val row = buildMap<String, Any?> {
                for (i in 1..columnCount) {
                    put(metaData.getColumnLabel(i), rs.getObject(i))
                }
            }

            check(!(rs.next())) { "Expected exactly one result row, but more than one row was returned" }

            return row
        }
    }
}

/**
 * Executes the query and returns all rows as a list of maps of column label to value.
 */
fun Connection.query(sql: String): List<Map<String, Any?>> {
    createStatement().use { stmt ->
        stmt.executeQuery(sql).use { rs ->
            val results = mutableListOf<Map<String, Any?>>()
            val metaData = rs.metaData
            val columnCount = metaData.columnCount
            while (rs.next()) {
                val data = buildMap {
                    for (i in 1..columnCount) {
                        put(metaData.getColumnLabel(i), rs.getObject(i))
                    }
                }
                results.add(data)
            }
            return results
        }
    }
}
