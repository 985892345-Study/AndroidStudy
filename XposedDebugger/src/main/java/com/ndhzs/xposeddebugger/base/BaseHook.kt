package com.ndhzs.debugger.xposed.base

/**
 * .
 *
 * @author 985892345
 * 2023/5/13 11:34
 */
abstract class BaseHook(val classLoader: ClassLoader) {
  abstract fun hook()
}