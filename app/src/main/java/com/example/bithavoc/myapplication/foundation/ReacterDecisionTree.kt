package com.example.bithavoc.myapplication.foundation

/**
 * Created by bithavoc on 5/7/16.
 */
class ReacterDecisionTree<T>(val parent: ReacterDecisionTree<T>?,
        val name:String) {
    private var condition:((state:T) -> Boolean)? = null
    private var thenCallback:((state:T) -> Unit)? = null
    private val decisions = mutableMapOf<String, ReacterDecisionTree<T>>()
    var satisifed = false
        private set
    fun on(condition:(state:T) -> Boolean) {
        this.condition = condition
    }

    fun then(callback:(state:T) -> Unit) {
        thenCallback = callback
    }

    fun so(name:String, build: ReacterDecisionTree<T>.() -> Unit) {
        if(decisions.containsKey(name)) {
            throw Exception("Decision '${generateChildPath(name)}' can only be configured once")
        }
        val tree = ReacterDecisionTree<T>(this, name)
        tree.build()
        decisions[name] = tree
    }

    fun check(state:T) : Boolean {
        if(!satisifed) {
            satisifed = (condition?.invoke(state) ?: false)
            if(satisifed) {
                thenCallback?.invoke(state)
            }
        }
        if(satisifed) {
            checkChildren(state)
        }
        return satisifed
    }

    private fun checkChildren(state:T) {
        decisions.values.forEach { it.check(state) }
    }

    private fun generateChildPath(name:String) : String {
        return "${path}/${name}"
    }

    val path:String
        get() {
            if(parent == null) {
                return name
            }
            return parent.generateChildPath(name)
        }
}