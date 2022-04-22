package com.wooftown.generators.tables

import com.wooftown.database.tables.Ingredient
import com.wooftown.utils.existInTable
import com.wooftown.utils.readFileAsLinesUsingGetResourceAsStream
import com.wooftown.utils.warn
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

class IngredientGenerator : Generator, Loader {

    private lateinit var ingredientList: ListIterator<String>

    override fun load(resourceFilename: String) {
        ingredientList =
            readFileAsLinesUsingGetResourceAsStream(resourceFilename).filter { it.length < 50 }.shuffled()
                .listIterator()
    }

    override fun generate(num: Int) {
        var counter = 0
        while (counter != num) {
            if (!ingredientList.hasNext()) {
                warn("Dataset does not contains $num ingredients!")
                return
            }

            val nextIngredientName = ingredientList.next()
            if (existInTable(Ingredient, Op.build { Ingredient.ingredientName eq nextIngredientName })) {
                continue
            }

            transaction {
                Ingredient.insert { row ->
                    row[ingredientName] = nextIngredientName
                    row[ingredientPrice] = ((Random.nextFloat() + 0.1) * 200).toBigDecimal()
                }
                counter++
            }
        }
    }
}

/*
    override fun generate(num: Int) {

        for (i in 1..num) {
            if (!ingredientList.hasNext()) {
                warn("Dataset does not contains $num ingredients!")
                return
            }

            transaction {
                Ingredient.insert { row ->
                    row[ingredientName] = ingredientList.next()
                    row[ingredientPrice] = ((Random.nextFloat() + 0.1) * 200).toBigDecimal()
                }
            }
        }
    }
*/
