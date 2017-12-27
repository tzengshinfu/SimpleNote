package tzengshinfu.simplenote

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import org.jetbrains.anko.db.*
import android.support.v7.widget.Toolbar
import android.widget.ImageButton
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast


class MainActivity : AppCompatActivity(), ColorPickerDialogListener {
    private val colorPickerDialogId: Int = 1
    private val expiredPeriodDay = 30
    private val expiredPeriodSecond = expiredPeriodDay * 24 * 60 * 60 //過期記事基準(30天)

    var currentColorIndex: Int = Color.WHITE
    var currentNoteTableName: String = NoteType.Todo.name

    lateinit var noteRecordTableAdapter: NoteRecordTableAdapter
    private lateinit var recyclerViewAdapter: RecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initialNoteParams()
        noteRecordTableAdapter = NoteRecordTableAdapter(this@MainActivity)

        //region 新增工具列
        val toolbar_Menu: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar_Menu)
        toolbar_Menu.title = this@MainActivity.getString(R.string.app_name)
        var button_Todo: ImageButton = toolbar_Menu.findViewById(R.id.button_Todo)
        var button_Completed: ImageButton = toolbar_Menu.findViewById(R.id.button_Completed)

        button_Todo.setOnClickListener {
            currentNoteTableName = NoteType.Todo.name
            recyclerViewAdapter.updateNoteRecords(noteRecordTableAdapter.getRecords(currentNoteTableName))
            button_AddOrUpdateNote.isEnabled = true
            button_PickColor.isEnabled = true
        }
        button_Completed.setOnClickListener {
            currentNoteTableName = NoteType.Completed.name
            recyclerViewAdapter.updateNoteRecords(noteRecordTableAdapter.getRecords(currentNoteTableName))
            button_AddOrUpdateNote.isEnabled = false
            button_PickColor.isEnabled = false
        }
        //endregion

        //region 初始化RecyclerView
        recyclerViewAdapter = RecyclerViewAdapter(this@MainActivity, noteRecordTableAdapter.getRecords(currentNoteTableName))
        val layoutManager = LinearLayoutManager(this@MainActivity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_NoteRecord.layoutManager = layoutManager
        recyclerView_NoteRecord.adapter = recyclerViewAdapter

        val recyclerViewTouchHandler = RecyclerViewTouchHandler()
        recyclerViewTouchHandler.attachToRecyclerView(recyclerView_NoteRecord)
        //endregion

        button_AddOrUpdateNote.setOnClickListener {
            if (editText_Content.text.isValid()) {
                if (textView_SelectedSeq.text.isValid()) {
                    notedb.use {
                        update(NoteType.Todo.name,
                                "color_index" to currentColorIndex,
                                "content" to editText_Content.text.toString())
                                .whereArgs("(seq = {selectedSeq})",
                                        "selectedSeq" to textView_SelectedSeq.text.getInt()).exec()
                    }
                } else {
                    notedb.use {
                        insert(NoteType.Todo.name,
                                "seq" to noteRecordTableAdapter.getNewSeq(NoteType.Todo.name),
                                "record_second" to noteRecordTableAdapter.getCurrentSeconds(),
                                "color_index" to currentColorIndex,
                                "content" to editText_Content.text.toString())
                    }
                }

                initialNoteParams()
                recyclerViewAdapter.updateNoteRecords(noteRecordTableAdapter.getRecords(currentNoteTableName))
            }
        }

        button_PickColor.setOnClickListener {
            ColorPickerDialog.newBuilder()
                    .setColor(currentColorIndex)
                    .setDialogId(colorPickerDialogId)
                    .show(this@MainActivity)
        }
    }

    override fun onResume() {
        super.onResume()

        //刪除過期記事
        var expiredNoteRecordSecond = (noteRecordTableAdapter.getCurrentSeconds() - expiredPeriodSecond).toLong()

        notedb.use {
            var expriedNoteRecordQueryResult = select(NoteType.Completed.name, "seq").whereArgs("(record_second < {expiredNoteRecordSecond})",
                    "expiredNoteRecordSecond" to expiredNoteRecordSecond).limit(1).parseOpt(IntParser)

            if (expriedNoteRecordQueryResult != null) {
                alert(this@MainActivity.getString(R.string.confirmDeletedExpiredCompletedNote).replace("[expiredPeriodDay]", expiredPeriodDay.toString()), this@MainActivity.getString(R.string.tip)) {
                    positiveButton(this@MainActivity.getString(R.string.confirm)) {
                        notedb.use {
                            delete(NoteType.Completed.name, "(record_second < {expiredNoteRecordSecond})", "expiredNoteRecordSecond" to expiredNoteRecordSecond)
                        }

                        toast(this@MainActivity.getString(R.string.expiredCompletedNoteDeleted))
                    }

                    negativeButton(this@MainActivity.getString(R.string.cancel)) {

                    }
                }.show()
            }

            noteRecordTableAdapter.sortRecords(NoteType.Completed.name)
            recyclerViewAdapter.updateNoteRecords(noteRecordTableAdapter.getRecords(currentNoteTableName))
        }
    }

    private fun initialNoteParams() {
        currentColorIndex = Color.WHITE
        editText_Content.setBackgroundColor(currentColorIndex)
        editText_Content.text.clear()
        textView_SelectedSeq.text = ""
    }


    override fun onColorSelected(dialogId: Int, color: Int) {
        if (dialogId == colorPickerDialogId) {
            currentColorIndex = color

            editText_Content.setBackgroundColor(currentColorIndex)
        }
    }

    override fun onDialogDismissed(dialogId: Int) {

    }
}
