package postus.workers

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import postus.controllers.Social.SocialsController

class ScheduleCheckWorker(private val socialController: SocialsController) : Runnable {
    override fun run() {
        try {
            println("Running ScheduleCheckWorker Job")
            val posts = ScheduleRepository.getPostsScheduledWithinNextHours(3)
            for (post in posts) {
                PostWorker(post, socialController).schedule()
            }
            println("Completed ScheduleCheckWorker Job")
        } catch (e: Exception) {
            println("Error during ScheduleCheckWorker Job: ${e.message}")
            e.printStackTrace()
        }
    }
}

fun startScheduledPostsChecker(socialController: SocialsController) {
    val scheduler = Executors.newScheduledThreadPool(1)
    val worker = ScheduleCheckWorker(socialController)
    scheduler.scheduleAtFixedRate(worker, 0, 3, TimeUnit.HOURS)
}
