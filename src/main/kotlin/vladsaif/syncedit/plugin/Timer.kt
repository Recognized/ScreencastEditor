package vladsaif.syncedit.plugin

object Timer {

  private var myLastStatementTime = 0L
  private var myPausedTime = 0L
  val isStarted get() = myLastStatementTime != 0L
  val isPaused get() = myPausedTime != 0L
  val offsetToLastStatement: Long
    get() = when {
      !isStarted -> throw IllegalStateException("Timer has not been started")
      isPaused -> throw IllegalStateException("Timer is paused")
      else -> System.currentTimeMillis() - myLastStatementTime
    }

  fun start() {
    if (isPaused) {
      val pauseTimeLength = System.currentTimeMillis() - myPausedTime
      myLastStatementTime += pauseTimeLength
      myPausedTime = 0
    } else {
      myLastStatementTime = System.currentTimeMillis()
    }
  }

  fun stop() {
    myLastStatementTime = 0
    myPausedTime = 0
  }

  fun pause() {
    myPausedTime = System.currentTimeMillis()
  }

  /**
   * Create 'timeOffset(Long)' statement using current time and time of previous statement.
   *
   * @return null if time pasted since last created statement or since start of [Timer] less than [THRESHOLD],
   * or if [Timer] is not active.
   */
  fun newTimeOffsetStatement(): String {
    val statement = "timeOffset(ms = ${offsetToLastStatement}L)"
    myLastStatementTime = System.currentTimeMillis()
    return statement
  }
}