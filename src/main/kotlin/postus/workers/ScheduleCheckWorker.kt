package postus.workers

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ScheduleCheckWorker : Runnable {
    override fun run() {
        val posts = ScheduleRepository.getPostsScheduledWithinNextHours(12)
        for (post in posts) {
            PostWorker(post).schedule()
        }
    }
}

fun startScheduledPostsChecker() {
    val scheduler = Executors.newScheduledThreadPool(1)
    val worker = ScheduleCheckWorker()
    scheduler.scheduleAtFixedRate(worker, 0, 12, TimeUnit.HOURS)
}
