package postus.workers

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ScheduleCheckWorker : Runnable {
    override fun run() {
        try {
            println("Running ScheduleCheckWorker Job")
            val posts = ScheduleRepository.getPostsScheduledWithinNextHours(3)
            for (post in posts) {
                PostWorker(post).schedule()
            }
            println("Completed ScheduleCheckWorker Job")
        } catch (e: Exception) {
            println("Error during ScheduleCheckWorker Job: ${e.message}")
            e.printStackTrace()
        }
    }
}

fun startScheduledPostsChecker() {
    val scheduler = Executors.newScheduledThreadPool(1)
    val worker = ScheduleCheckWorker()
    scheduler.scheduleAtFixedRate(worker, 0, 3, TimeUnit.HOURS)
}
