package com.example.bithavoc.myapplication.foundation

data class GlobalStateIdentifier(var name:String = "", var instanceName:String = "") {
    fun toUniqueId() : String {
        return "${name}/${instanceName}"
    }
}