package com.wooftown.generators.tables

import com.wooftown.database.tables.SizeMods
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

object SizeLoader : Loader {

    override fun load(resourceFilename: String) {
        val bufferedReader = BufferedReader(FileReader(File(fileFromResource(resourceFilename))))
        val csvParser = CSVParser(bufferedReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())
        for (csvRecord in csvParser) {

            if (existInTable(SizeMods, Op.build { SizeMods.sizeCm eq csvRecord[0].toInt() })) {
                continue
            }

            transaction {
                SizeMods.insert { row ->
                    row[sizeCm] = csvRecord[0].toInt()
                    row[sizeMod] = csvRecord[1].toBigDecimal()
                }
            }

        }
    }
}