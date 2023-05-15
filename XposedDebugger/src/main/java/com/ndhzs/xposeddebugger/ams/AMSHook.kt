package com.ndhzs.xposeddebugger.ams

import com.ndhzs.debugger.xposed.base.BaseHook
import com.ndhzs.xposeddebugger.utils.find
import com.ndhzs.xposeddebugger.utils.hookAfterAllConstructors
import com.ndhzs.xposeddebugger.utils.hookAfterMethod
import com.ndhzs.xposeddebugger.utils.log

/**
 * .
 *
 * @author 985892345
 * 2023/5/15 16:13
 */
class AMSHook(classLoader: ClassLoader) : BaseHook(classLoader) {
  override fun hook() {
    "android.app.ActivityThread".hookAfterMethod(
      classLoader,
      "systemMain"
    ) {
      val tAMSClassLoader =
        Thread.currentThread().contextClassLoader!!.find("com.android.server.am.ActivityManagerService")
      tAMSClassLoader.hookAfterAllConstructors {
        log("AMS Constructor")
      }
    }
  }
}