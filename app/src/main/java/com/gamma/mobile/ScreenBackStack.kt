package com.gamma.mobile

/**
 * Pila de navegación mínima para las pantallas secundarias (sin NavHost).
 * Permite que "Atrás" regrese a la pantalla realmente anterior en lugar de cerrar la app.
 */
class ScreenBackStack {
    private val stack = mutableListOf<Screen>()

    val isEmpty: Boolean get() = stack.isEmpty()

    fun push(screen: Screen) {
        stack.add(screen)
    }

    /** Devuelve la pantalla anterior, o [fallback] si la pila está vacía. */
    fun pop(fallback: Screen = Screen.Dashboard): Screen =
        if (stack.isEmpty()) fallback else stack.removeAt(stack.lastIndex)

    fun clear() {
        stack.clear()
    }
}

/** Pantallas que se abren encima de otra y deben responder al botón/gesto Atrás. */
fun Screen.isSecondary(): Boolean = this == Screen.NuevaTransaccion || this == Screen.SifenScanner
