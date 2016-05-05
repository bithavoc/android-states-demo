package com.example.bithavoc.myapplication.foundation
import de.danielbechler.diff.instantiation.TypeInfo
import de.danielbechler.diff.introspection.Introspector
import de.danielbechler.diff.introspection.PropertyAccessor

class DataClassIntrospector : Introspector {
    override fun introspect(type: Class<*>?): TypeInfo? {
        if(type == null) {
            return null
        }
        var getterNames = type.methods.filter { it.name.startsWith("get") }.map { it.name.replaceFirst("get", "") }.filterNot { skippedPropertyNames.contains(it) }
        var info = TypeInfo(type)
        getterNames.map { PropertyAccessor(it, type.getMethod("get$it"), null) }.forEach { info.addPropertyAccessor(it) }
        return info
    }
    companion object {
        private val skippedPropertyNames = setOf("Class", "MetaClass");
    }
}