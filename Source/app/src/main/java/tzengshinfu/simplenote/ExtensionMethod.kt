package tzengshinfu.simplenote

import android.support.annotation.Nullable
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.view.View


fun Any.isValid(): Boolean {
    var result = true

    if (this == null) {
        result = false
    } else {
        if (this is String) {
            if (this == "") {
                result = false
            }
        } else if (this is Editable) {
            if (this.toString() == "") {
                result = false
            }
        } else if (this is Boolean) {
            if (this == false) {
                result = false
            }
        }
    }

    return result
}

fun Any.getInt(): Int {
    var result = 0

    if (this != null) {
        if (this is String) {
            if (this.isValid()) {
                result = this.toInt()
            }
        } else if (this is Editable) {
            if (this.isValid()) {
                result = this.toString().toInt()
            }
        } else if (this is Boolean) {
            if (this == true) {
                result = 1
            }
        }
    }

    return result
}

fun View.getSelectedNoteRecord(): NoteRecordRow {
    var seq = this.getTag(R.string.seq).toString().toLong()
    var recordSecond = this.getTag(R.string.record_second).toString().toLong()
    var colorIndex = this.getTag(R.string.color_index).toString().toLong()
    var content = this.getTag(R.string.content).toString()

    return NoteRecordRow(seq, recordSecond, colorIndex, content)
}

fun ItemTouchHelper.SimpleCallback.attachToRecyclerView(@Nullable recyclerView: RecyclerView) {
    var recyclerViewTouchHandler = this as RecyclerViewTouchHandler
    recyclerViewTouchHandler.recyclerViewAdapter = recyclerView.adapter as RecyclerViewAdapter
    recyclerViewTouchHandler.mainActivity = recyclerView.context as MainActivity
    ItemTouchHelper(recyclerViewTouchHandler).attachToRecyclerView(recyclerView)
}