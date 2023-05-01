package com.ndhuz.recyclerview.adapter

import android.database.Observable
import androidx.recyclerview.widget.RecyclerView

/**
 * [RecyclerView.AdapterDataObservable]
 *
 * @author 985892345
 * 2023/5/1 14:23
 */
class MyAdapterDataObservable : Observable<MyAdapterDataObserver>() {
  
  fun hasObservers(): Boolean {
    return mObservers.isNotEmpty()
  }
}