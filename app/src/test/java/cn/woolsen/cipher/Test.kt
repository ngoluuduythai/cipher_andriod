package cn.woolsen.cipher

import cn.woolsen.cipher.util.HexKt
import cn.woolsen.cipher.util.HexUtils
import org.junit.Assert
import org.junit.Test
import java.lang.StringBuilder

/**
 * @author woolsen
 * @date 2021/03/08 16:22
 */
class Test {

    private var i: Int? = null

    private val stringBuilder by lazy {
        println("init StringBuilder $i")
        StringBuilder("hello")
    }

    @Test
    fun test() {
        i = 1
        println("start 1")
        stringBuilder
        println("start 2")
        stringBuilder.toString()
    }
}