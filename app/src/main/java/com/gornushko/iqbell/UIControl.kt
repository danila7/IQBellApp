package com.gornushko.iqbell

interface UIControl {
    fun btOff()
    fun btOnNotPaired()
    fun connected()
    fun connecting()
    fun reconnecting()
    fun authFailed()
    fun authSucceed()
    fun authenticating()
}