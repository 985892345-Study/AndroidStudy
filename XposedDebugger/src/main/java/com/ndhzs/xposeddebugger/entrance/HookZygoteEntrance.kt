package com.ndhzs.xposeddebugger.entrance

import com.ndhzs.xposeddebugger.ams.AMSHook
import de.robv.android.xposed.IXposedHookZygoteInit

/**
 * .
 *
 * @author 985892345
 * 2023/5/15 16:09
 */
class HookZygoteEntrance : IXposedHookZygoteInit {
  override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
    val classLoader = Thread.currentThread().contextClassLoader!!
    AMSHook(classLoader).hook()
  }
}