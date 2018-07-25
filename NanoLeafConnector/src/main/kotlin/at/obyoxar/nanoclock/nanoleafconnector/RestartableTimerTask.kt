package at.obyoxar.nanoclock.nanoleafconnector

import java.util.*

class RestartableTimerTask(val task: () -> Unit){

    var timerTask: TimerTask? = null

    fun cancel(){
        timerTask?.cancel()?.also { timerTask = null } ?: throw Error("Task is already cancelled")
    }

    fun extractNewTimerTask(): TimerTask {
        if(this.timerTask != null){
            throw Error("Task is already scheduled")
        }
        val timerTask = object: TimerTask() {
            override fun run() = task()
        }
        this.timerTask = timerTask
        return timerTask
    }
}

fun restartableTimerTask(task: () -> Unit) = RestartableTimerTask(task)


fun Timer.schedule(restartableTimerTask: RestartableTimerTask, initialDelay: Long, period: Long) {

    this.schedule(restartableTimerTask.extractNewTimerTask(), initialDelay, period)
}
fun Timer.schedule(restartableTimerTask: RestartableTimerTask, date: Date) {
    this.schedule(restartableTimerTask.extractNewTimerTask(), date)
}