package tzengshinfu.simplenote

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.support.annotation.Nullable
import android.support.v7.util.DiffUtil
import org.jetbrains.anko.db.*


class NoteDbOpenHelper(context: Context) : ManagedSQLiteOpenHelper(context, "notedb", null, 1) {
    companion object {
        private var instance: NoteDbOpenHelper? = null

        @Synchronized
        fun getInstance(context: Context): NoteDbOpenHelper {
            if (instance == null) {
                instance = NoteDbOpenHelper(context.getApplicationContext())
            }

            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(NoteType.Todo.name, true,
                "seq" to INTEGER + PRIMARY_KEY + UNIQUE,
                "record_second" to INTEGER,
                "color_index" to INTEGER,
                "content" to TEXT)

        db.createTable(NoteType.Completed.name, true,
                "seq" to INTEGER + PRIMARY_KEY + UNIQUE,
                "record_second" to INTEGER,
                "color_index" to INTEGER,
                "content" to TEXT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.dropTable(NoteType.Todo.name, true)
        db.dropTable(NoteType.Completed.name, true)
    }
}

val Context.notedb: NoteDbOpenHelper
    get() = NoteDbOpenHelper.getInstance(applicationContext)

data class NoteRecordRow(var seq: Long, var recordSecond: Long, var colorIndex: Long, var content: String)

class NoteRecordRowParser : MapRowParser<NoteRecordRow> {
    override fun parseRow(columns: Map<String, Any?>): NoteRecordRow {
        return NoteRecordRow(columns["seq"] as Long, columns["record_second"] as Long, columns["color_index"] as Long, columns["content"] as String)
    }
}

class NoteRecordDifferenceComparer(private var oldNoteRecords: List<NoteRecordRow>, private var newNoteRecords: List<NoteRecordRow>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldNoteRecords.size
    }

    override fun getNewListSize(): Int {
        return newNoteRecords.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldNoteRecords[oldItemPosition].seq == newNoteRecords[newItemPosition].seq
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldNoteRecords[oldItemPosition] == newNoteRecords[newItemPosition]
    }

    @Nullable
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}

class NoteRecordTableAdapter(var context: Context) {
    fun getCurrentSeconds(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }

    fun getRecords(selectedNoteTableName: String, selectedSeq: Int? = null): List<NoteRecordRow> {
        lateinit var noteRecords: List<NoteRecordRow>

        context.notedb.use {
            if (selectedSeq != null) {
                noteRecords = select(selectedNoteTableName).whereArgs("(seq = {selectedSeq})", "selectedSeq" to selectedSeq).parseList(NoteRecordRowParser())
            } else {
                noteRecords = select(selectedNoteTableName).orderBy("seq", SqlOrderDirection.ASC).parseList(NoteRecordRowParser())
            }
        }

        return noteRecords
    }

    fun getNewSeq(selectedNoteTableName: String): Int {
        var result = 1

        context.notedb.use {
            var noteSeqQueryResult = select(selectedNoteTableName, "seq").orderBy("seq", SqlOrderDirection.DESC).limit(1).parseOpt(IntParser)

            if (noteSeqQueryResult != null) {
                result = noteSeqQueryResult.toInt() + 1
            }
        }

        return result
    }

    fun sortRecords(selectedNoteTableName: String) {
        var currentNoteRecordQueryResult = getRecords(selectedNoteTableName)

        context.notedb.use {
            for (currentNoteIndex in 0 until currentNoteRecordQueryResult.count()) {
                var oldSeq = currentNoteRecordQueryResult[currentNoteIndex].seq.toInt()
                var newSeq = currentNoteIndex + 1

                if (oldSeq != newSeq) {
                    update(selectedNoteTableName, "seq" to newSeq)
                            .whereArgs("(seq = {oldSeq})",
                                    "oldSeq" to oldSeq).exec()
                }
            }
        }
    }

    fun swapRecords(selectedNoteTableName: String, oldSeq: Int, newSeq: Int) {
        context.notedb.use {
            update(selectedNoteTableName, "seq" to 0)
                    .whereArgs("(seq = {newSeq})",
                            "newSeq" to newSeq).exec()

            update(selectedNoteTableName, "seq" to newSeq)
                    .whereArgs("(seq = {oldSeq})",
                            "oldSeq" to oldSeq).exec()


            update(selectedNoteTableName, "seq" to oldSeq)
                    .whereArgs("(seq = {newSeq})",
                            "newSeq" to 0).exec()
        }
    }
}

enum class NoteType(val value: Int) {
    Todo(R.string.todo),
    Completed(R.string.completed)
}