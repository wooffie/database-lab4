package com.wooftown.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object IngredientsToPizza : Table("ingredients_to_pizza") {
    val ingredientId = integer("ingredient_id").references(Ingredient.ingredientId, onDelete = ReferenceOption.RESTRICT)
    val pizzaId = integer("pizza_id").references(Pizza.pizzaId, onDelete = ReferenceOption.CASCADE)

    init {
        index(true, ingredientId, pizzaId)
    }
}