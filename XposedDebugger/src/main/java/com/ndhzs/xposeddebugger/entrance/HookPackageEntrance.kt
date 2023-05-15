package com.ndhzs.xposeddebugger.entrance

import com.ndhzs.xposeddebugger.recyeerview.RecyclerViewHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * .
 *
 * @author 985892345
 * 2023/5/15 16:04
 */
class HookPackageEntrance : IXposedHookLoadPackage {
  override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
    val classLoader = lpparam.classLoader
    RecyclerViewHook(classLoader).hook()
  }
}