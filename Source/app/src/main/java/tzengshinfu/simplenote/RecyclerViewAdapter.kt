package tzengshinfu.simplenote

import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.toast


class RecyclerViewAdapter(var context: Context, var currentNoteRecords: List<NoteRecordRow>) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var textView_Content: TextView = view.findViewById(R.id.textView_Content)
        var checkBox_Completed : CheckBox = view.findViewById(R.id.checkBox_Completed)
    }

    var mainActivity = context as MainActivity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewAdapter.ViewHolder {
        //修改記事內容
        var view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        view.setOnClickListener {
            var selectedNoteRecord = view.getSelectedNoteRecord()

            mainActivity.currentColorIndex = selectedNoteRecord.colorIndex.toInt()
            mainActivity.editText_Content.setText(selectedNoteRecord.content)
            mainActivity.textView_SelectedSeq.text = selectedNoteRecord.seq.toString()
        }

        //按[完成]
        var checkBox_Completed: CheckBox = view.findViewById(R.id.checkBox_Completed)
        checkBox_Completed.setOnClickListener {
            var selectedNoteRecord = view.getSelectedNoteRecord()

            if (checkBox_Completed.isChecked) {
                parent.context.notedb.use {
                    insert(NoteType.Completed.name,
                            "seq" to mainActivity.noteRecordTableAdapter.getNewSeq(NoteType.Completed.name),
                            "record_second" to selectedNoteRecord.recordSecond,
                            "color_index" to selectedNoteRecord.colorIndex,
                            "content" to selectedNoteRecord.content)

                    delete(NoteType.Todo.name, "(seq = {selectedSeq})",
                            "selectedSeq" to selectedNoteRecord.seq)
                }

                mainActivity.noteRecordTableAdapter.sortRecords(NoteType.Todo.name)
                this@RecyclerViewAdapter.updateNoteRecords(mainActivity.noteRecordTableAdapter.getRecords(NoteType.Todo.name))

                parent.context.toast(R.string.complete)
            } else {
                parent.context.notedb.use {
                    insert(NoteType.Todo.name,
                            "seq" to mainActivity.noteRecordTableAdapter.getNewSeq(NoteType.Todo.name),
                            "record_second" to selectedNoteRecord.recordSecond,
                            "color_index" to selectedNoteRecord.colorIndex,
                            "content" to selectedNoteRecord.content)

                    delete(NoteType.Completed.name, "(seq = {selectedSeq})",
                            "selectedSeq" to selectedNoteRecord.seq)
                }

                mainActivity.noteRecordTableAdapter.sortRecords(NoteType.Completed.name)
                this@RecyclerViewAdapter.updateNoteRecords(mainActivity.noteRecordTableAdapter.getRecords(NoteType.Completed.name))

                parent.context.toast(R.string.todo)
            }
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var seq = currentNoteRecords[position].seq
        var recordSecond = currentNoteRecords[position].recordSecond
        var colorIndex = currentNoteRecords[position].colorIndex
        var content = currentNoteRecords[position].content

        holder.textView_Content.text = content
        holder.textView_Content.setBackgroundColor(colorIndex.toInt())
        if(mainActivity.currentNoteTableName == NoteType.Todo.name) holder.checkBox_Completed.isChecked = false else holder.checkBox_Completed.isChecked = true

        holder.itemView.setTag(R.string.seq, seq)
        holder.itemView.setTag(R.string.record_second, recordSecond)
        holder.itemView.setTag(R.string.color_index, colorIndex)
        holder.itemView.setTag(R.string.content, content)
    }

    override fun getItemCount(): Int {
        return currentNoteRecords.size
    }

    fun updateNoteRecords(newNoteRecords: List<NoteRecordRow>) {
        val diffCallback = NoteRecordDifferenceComparer(currentNoteRecords, newNoteRecords)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        currentNoteRecords = newNoteRecords
        diffResult.dispatchUpdatesTo(this)
    }
}