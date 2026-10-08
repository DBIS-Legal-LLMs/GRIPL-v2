package de.mertendieckmann.griplbackend.application.dataset

/**
 * Reads and writes CSV in the format of the Postgres CSV export (`COPY ... WITH CSV HEADER FORCE QUOTE *`):
 * comma separated, every non-null value double-quoted, quotes escaped by doubling, NULL as empty unquoted field.
 * Quoted fields may contain line breaks (e.g. BPMN XML).
 */
object PostgresCsv {

    fun write(header: List<String>, rows: List<List<String?>>): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",") { quote(it) }).append('\n')
        rows.forEach { row ->
            sb.append(row.joinToString(",") { it?.let(::quote) ?: "" }).append('\n')
        }
        return sb.toString()
    }

    private fun quote(value: String) = "\"" + value.replace("\"", "\"\"") + "\""

    /**
     * Parses CSV text into a list of rows, each row mapped by header name.
     * Empty unquoted fields are returned as null, empty quoted fields as "".
     */
    fun read(text: String): List<Map<String, String?>> {
        val records = parseRecords(text.removePrefix("﻿"))
        if (records.isEmpty()) return emptyList()
        val header = records.first().map { it?.trim() ?: "" }
        return records.drop(1)
            .filter { record -> !(record.size == 1 && record[0] == null) } // skip blank lines
            .mapIndexed { index, record ->
                if (record.size != header.size) {
                    throw DatasetImportException(
                        "Row ${index + 2} has ${record.size} columns, but the header has ${header.size}"
                    )
                }
                header.zip(record).toMap()
            }
    }

    private fun parseRecords(text: String): List<List<String?>> {
        val records = mutableListOf<List<String?>>()
        var record = mutableListOf<String?>()
        val field = StringBuilder()
        var inQuotes = false
        var wasQuoted = false
        var i = 0

        fun endField() {
            record.add(if (field.isEmpty() && !wasQuoted) null else field.toString())
            field.clear()
            wasQuoted = false
        }

        fun endRecord() {
            endField()
            records.add(record)
            record = mutableListOf()
        }

        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> { inQuotes = true; wasQuoted = true }
                    ',' -> endField()
                    '\r' -> { if (i + 1 < text.length && text[i + 1] == '\n') i++; endRecord() }
                    '\n' -> endRecord()
                    else -> field.append(c)
                }
            }
            i++
        }

        if (inQuotes) throw DatasetImportException("CSV ends inside a quoted field")
        if (field.isNotEmpty() || wasQuoted || record.isNotEmpty()) endRecord()
        return records
    }
}
