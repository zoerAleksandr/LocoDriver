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

fun <T> MutableList<T>.addAllOrSkip(collection: MutableCollection<T>) {
    collection.forEach { element ->
        val searchElement = this.find {
            it == element
        }

        if (searchElement == null) {
            this.add(element)
        }
    }
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
