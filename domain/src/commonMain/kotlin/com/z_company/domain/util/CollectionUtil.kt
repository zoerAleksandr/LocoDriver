package com.z_company.domain.util

fun <T> MutableList<T>.addOrReplace(element: T) {
    val searchElement = this.find {
        it == element
    }

    if (searchElement == null) {
        this.add(element)
    } else {
        val index = this.indexOf(searchElement)
        this[index] = element
    }
}

fun <T> MutableList<T>.addOrSkip(element: T): Boolean {
    val searchElement = this.find {
        it == element
    }

    return if (searchElement == null) {
        this.add(element)
        true
    } else {
        false
    }
}

fun <T> MutableList<T>.addOrSkip(index: Int, element: T): Boolean {
    val searchElement = this.find {
        it == element
    }

    return if (searchElement == null) {
        this.add(index, element)
        true
    } else {
        false
    }
}

/**
 * Добавляет элементы [collection], которых ещё нет в списке (по equals), сохраняя порядок
 * и пропуская дубликаты внутри самой [collection].
 *
 * Раньше для каждого элемента делался линейный `find` по SnapshotStateList: словарь
 * станций на тысячи записей давал O(n²) на главном потоке при каждом открытии формы.
 * Теперь O(n) через HashSet и одна запись `addAll`.
 */
@Suppress("SENSELESS_COMPARISON")
fun <T> MutableList<T>.addAllOrSkip(collection: Collection<T>?) {
    if (collection == null || this == null || collection.isEmpty()) return
    val seen = HashSet<T>(size + collection.size)
    seen.addAll(this)
    val toAdd = ArrayList<T>()
    for (element in collection) {
        if (seen.add(element)) toAdd.add(element)
    }
    if (toAdd.isNotEmpty()) addAll(toAdd)
}

fun <T> List<T>.safetySubList(fromIndex: Int, toIndex: Int): List<T>{
    if (this.isEmpty()){
        return this
    }

    val lastIndex = if (this.size > toIndex){
        toIndex
    } else {
        this.size
    }

    return this.subList(fromIndex, lastIndex)
}

fun List<Double?>.sum(): Double {
    var sum: Double = 0.toDouble()
    this.forEach {
        sum += it
    }
    return sum
}
