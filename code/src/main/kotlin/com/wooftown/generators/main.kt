package com.wooftown.generators

import com.wooftown.database.DatabaseFactory
import com.wooftown.generators.tables.*
import com.wooftown.utils.clearAll


fun main() {
    DatabaseFactory.connect()

    clearAll()

    val ingredientGenerator = IngredientGenerator()
    ingredientGenerator.load(GenerationConfig.ingredientSourceFile)
    ingredientGenerator.generate(GenerationConfig.ingredientsNumber)
    SizeLoader.load(GenerationConfig.sizeSourceFile)
    PizzaGenerator.generate(GenerationConfig.pizzasNumber)
    PostLoader.load(GenerationConfig.postSourceFile)
    RestaurantGenerator().generate(GenerationConfig.restaurantsNumber)
    EmployeeGenerator().generate(GenerationConfig.employeeNumber)
    OrderGenerator().generate(GenerationConfig.ordersNumber)
    ReviewGenerator().generate(GenerationConfig.reviewNumber)
}
