package life.matthewbushell.tideline

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import life.matthewbushell.tideline.beach.Beach
import java.util.concurrent.TimeUnit

/*
 * The job that drains the queue to the beach (spine:tideline 7.12 c): each
 * waiting line appended at the catch's root under its edit latch, then the
 * count note to the room when one is due. It runs when there is a network,
 * and Android runs it again later when the beach could not be reached.
 */
class LandWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    companion object {
        private const val LAND = "land"
        private const val NOTE = "room-note"

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Land what is waiting, after any landing already under way. */
        fun start(context: Context) = WorkManager.getInstance(context).enqueueUniqueWork(
            LAND,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<LandWorker>()
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )

        /** Come back when the next count note may go. */
        private fun later(context: Context, millis: Long) = WorkManager.getInstance(context).enqueueUniqueWork(
            NOTE,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<LandWorker>()
                .setConstraints(online)
                .setInitialDelay(millis, TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    // A landing and a count note's later run can both be under way; only one
    // drains the queue at a time, or a line could land twice.
    override fun doWork(): Result = synchronized(LandWorker::class.java) { land() }

    private fun land(): Result {
        val context = applicationContext
        val latch = Keys.latch(context) ?: return Result.success()
        val beach = Beach(userAgent = "Tideline")
        val note = Hands.roomNote(context)

        val outcome = Landing(Hands.queue(context)) { beach.appendAtRoot(Places.CATCH, it, latch) }.drain()
        val now = System.currentTimeMillis()
        note.add(outcome.apps)
        if (outcome.apps.isNotEmpty()) Status.landed(context, now)
        if (outcome is Landing.Outcome.Refused) Status.landingProblem(context, outcome.reason)

        val wait = note.dueIn(now)
        if (wait == 0L) {
            when (val a = beach.appendAtRoot(Places.ROOM, RoomNote.entry(note.counts()), null)) {
                is Beach.Answer.Landed -> {
                    note.sent(now)
                    Status.noted(context, now)
                }
                is Beach.Answer.Refused -> Status.noteProblem(context, a.reason)
                is Beach.Answer.Unreached -> later(context, 2 * 60 * 1000L)
            }
        } else if (wait != null) {
            later(context, wait)
        }

        return if (outcome is Landing.Outcome.Unreached) Result.retry() else Result.success()
    }
}
