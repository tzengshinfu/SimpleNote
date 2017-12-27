package tzengshinfu.simplenote

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.toast
import java.util.*


class RecyclerViewTouchHandler : ItemTouchHelper.SimpleCallback(0, 0) {
    lateinit var recyclerViewAdapter: RecyclerViewAdapter
    lateinit var mainActivity: MainActivity
    var fromToHistory = mutableListOf<Pair<Int, Int>>()

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.RIGHT

        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        recyclerViewAdapter.notifyItemRemoved(viewHolder.adapterPosition)

        var selectedNoteRecord = viewHolder.itemView.getSelectedNoteRecord()
        mainActivity.notedb.use {
            delete(mainActivity.currentNoteTableName, "(seq = {selectedSeq})",
                    "selectedSeq" to selectedNoteRecord.seq)
        }

        mainActivity.noteRecordTableAdapter.sortRecords(mainActivity.currentNoteTableName)
        recyclerViewAdapter.updateNoteRecords(mainActivity.noteRecordTableAdapter.getRecords(mainActivity.currentNoteTableName))

        mainActivity.toast(R.string.delete)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        Collections.swap(recyclerViewAdapter.currentNoteRecords, viewHolder.adapterPosition, target.adapterPosition)
        recyclerViewAdapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)

        fromToHistory.add(Pair(viewHolder.adapterPosition, target.adapterPosition))

        return true
    }

    override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?) {
        super.clearView(recyclerView, viewHolder)

        for (actionIndex in 0 until fromToHistory.count()) {
            mainActivity.noteRecordTableAdapter.swapRecords(mainActivity.currentNoteTableName, recyclerViewAdapter.currentNoteRecords[fromToHistory[actionIndex].first].seq.toInt()
                    , recyclerViewAdapter.currentNoteRecords[fromToHistory[actionIndex].second].seq.toInt())
        }

        fromToHistory.clear()
    }
}