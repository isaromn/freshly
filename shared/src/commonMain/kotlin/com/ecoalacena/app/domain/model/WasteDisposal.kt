package com.ecoalacena.app.domain.model

/** What to do with food that can no longer be eaten, so it still has a use. */
enum class DisposalMethod {
    /** Home compost or plant fertiliser: fruit, vegetables, bread, grains. */
    COMPOST,

    /** The organic waste bin, well closed: meat, fish, dairy. They attract pests in home compost. */
    ORGANIC_WASTE,

    /** Pour out the drink and recycle the container. */
    RECYCLE_CONTAINER,
}

val Category.disposalMethod: DisposalMethod
    get() = when (this) {
        Category.FRUITS_VEGETABLES, Category.BAKERY, Category.PANTRY_STAPLE -> DisposalMethod.COMPOST
        Category.MEAT_FISH, Category.DAIRY, Category.OTHER -> DisposalMethod.ORGANIC_WASTE
        Category.BEVERAGES -> DisposalMethod.RECYCLE_CONTAINER
    }
