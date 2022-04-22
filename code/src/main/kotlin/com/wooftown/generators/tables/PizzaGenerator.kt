package com.wooftown.generators.tables

import com.wooftown.database.tables.Ingredient
import com.wooftown.database.tables.IngredientsToPizza
import com.wooftown.database.tables.Pizza
import com.wooftown.database.tables.SizeMods

import com.wooftown.utils.existInTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.absoluteValue
import kotlin.math.min

object PizzaGenerator : Generator {


    override fun generate(num: Int) {

        val ingredientIds = transaction {
            Ingredient.slice(Ingredient.ingredientId, Ingredient.ingredientName).selectAll()
                .map { it[Ingredient.ingredientId] to it[Ingredient.ingredientName] }
        }
        val sizeIds = transaction {
            SizeMods.slice(SizeMods.sizeId).selectAll().map { it[SizeMods.sizeId] }
        }

        for (i in 1..num) {
            val ingredientsSet = ingredientIds.shuffled().take((2..10).random())

            var naming = generateName(ingredientsSet.map { it.second })

            while (existInTable(Pizza, Op.build { Pizza.pizzaName eq naming })) {
                naming =
                    generateName(ingredientsSet.map { it.second }.take((1..ingredientsSet.size).random()).shuffled())
            }

            transaction {

                val generatedPizzas = Pizza.batchInsert(sizeIds, shouldReturnGeneratedValues = true) { batch ->
                    this[Pizza.pizzaName] = naming
                    this[Pizza.sizeId] = batch
                }
                val pizzaIds = generatedPizzas.map { it[Pizza.pizzaId].absoluteValue }

                for (pizzaId in pizzaIds) {
                    for (ingredientId in ingredientsSet) {
                        IngredientsToPizza.insert { row ->
                            row[IngredientsToPizza.ingredientId] = ingredientId.first
                            row[IngredientsToPizza.pizzaId] = pizzaId
                        }
                    }
                }
            }

        }


    }

    private fun generateName(ingredients: List<String>): String {
        val portion = (45 / ingredients.size)
        val result = StringBuilder()
        for (name in ingredients) {
            result.append(name.substring(0 until min(portion, name.length)))
        }
        // Для того чтобы убрать двойные пробелы и начало каждого слова с большой буквы
        return result.toString().split(" ").joinToString(separator = " ", transform = String::capitalize)
    }


}