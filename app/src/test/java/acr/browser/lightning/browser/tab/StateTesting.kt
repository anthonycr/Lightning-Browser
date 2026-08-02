package acr.browser.lightning.browser.tab

import kotlin.reflect.KClass

fun <T : Any> KClass<T>.checkAllPermutations(
    permutations: Map<Pair<T, T>, Boolean>
) {
    val subclasses = this.sealedSubclasses

    val klassPermutations = subclasses.flatMap { subclass ->
        subclasses.map {
            subclass to it
        }
    }

    val permutationsMissed = klassPermutations.filter { (oneKlass, anotherKlass) ->
        permutations.keys.none { (one, another) ->
            (oneKlass.isInstance(one) && anotherKlass.isInstance(another)) ||
                (anotherKlass.isInstance(one) && oneKlass.isInstance(another))
        }
    }.joinToString(
        separator = "\n",
        transform = { (oneKlass, anotherKlass) -> "[${oneKlass.simpleName} <-> ${anotherKlass.simpleName}]" }
    )

    if (permutationsMissed.isNotEmpty()) {
        error("Missed permutations: $permutationsMissed")
    }
}
