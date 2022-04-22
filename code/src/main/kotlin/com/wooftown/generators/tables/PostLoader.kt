package com.wooftown.generators.tables

import com.wooftown.database.tables.Post
import com.wooftown.utils.fileFromResource
import com.wooftown.utils.existInTable
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object PostLoader : Loader {
    override fun load(resourceFilename: String) {
        val bufferedReader = BufferedReader(FileReader(File(fileFromResource(resourceFilename))))
        val csvParser = CSVParser(bufferedReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())
        for (csvRecord in csvParser) {

            if (existInTable(Post, Op.build { Post.postName eq csvRecord[0].toString() })) {
                continue
            }

            transaction {
                Post.insert { row ->
                    row[postName] = csvRecord[0].toString()
                    row[postCook] = csvRecord[1].toBoolean()
                    row[postCashbox] = csvRecord[2].toBoolean()
                    row[postDelivery] = csvRecord[3].toBoolean()
                }
            }
        }
    }
}