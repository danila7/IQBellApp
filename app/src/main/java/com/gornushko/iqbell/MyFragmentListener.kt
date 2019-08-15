package com.gornushko.iqbell

interface MyFragmentListener {
    fun sendData(data: ByteArray, updateExtra: Boolean = false)
    fun noEdit()
    fun edit()
}