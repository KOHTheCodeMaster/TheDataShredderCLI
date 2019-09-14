package dev.koh.utils;


import dev.koh.utils.enums.TimeUnit;

public class MyTimer {
    private double currentTime;
    private double totalTimeTaken;
    private double pauseTime;
    private double totalPauseTime;
    private TimeUnit timeUnit;

    public void startTimer() {
        currentTime = System.nanoTime();
    }

    public void stopTimer() {

        double endTime = System.nanoTime();
        totalTimeTaken = endTime - currentTime;
        findTimeUnit();

    }

    public void pauseTimer() {
        pauseTime = System.nanoTime();
    }

    public void continueTimer() {
        double endTime = System.nanoTime();
        totalPauseTime = endTime - pauseTime;
        currentTime += totalPauseTime;
    }

    public void stopTimer(boolean shouldDisplayTimeTaken) {

        double endTime = System.nanoTime();
        totalTimeTaken = endTime - currentTime;
        findTimeUnit();

        if (shouldDisplayTimeTaken)
            displayTimeTaken();

    }

    private void displayTimeTaken() {
        System.out.println("Time Taken: " + this.getTotalTimeTaken() + " " + this.getTimeUnit());
    }

    private void findTimeUnit() {

    /*
        final int SECOND = 1000;
        final int MINUTE = 60 * SECOND;
        final int HOUR = 60 * MINUTE;
        final int DAY = 24 * HOUR;
        final int WEEK = 7 * DAY;
        final long MONTH = 30L * DAY;
        final long YEAR = 12 * MONTH;
        final long DECADE = 10 * YEAR;
    */

        if (totalTimeTaken < TimeUnit.MICRO_SECOND.getTimeInMilliSeconds())
            timeUnit = TimeUnit.NANO_SECOND;
        else if (totalTimeTaken < TimeUnit.MILLI_SECOND.getTimeInMilliSeconds())
            timeUnit = TimeUnit.MICRO_SECOND;
        else if (totalTimeTaken < TimeUnit.SECOND.getTimeInMilliSeconds())
            timeUnit = TimeUnit.MILLI_SECOND;
        else if (totalTimeTaken < TimeUnit.MINUTE.getTimeInMilliSeconds())
            timeUnit = TimeUnit.SECOND;
        else if (totalTimeTaken < TimeUnit.HOUR.getTimeInMilliSeconds())
            timeUnit = TimeUnit.MINUTE;
        else if (totalTimeTaken < TimeUnit.DAY.getTimeInMilliSeconds())
            timeUnit = TimeUnit.HOUR;
        else if (totalTimeTaken < TimeUnit.WEEK.getTimeInMilliSeconds())
            timeUnit = TimeUnit.DAY;
        else if (totalTimeTaken < TimeUnit.MONTH.getTimeInMilliSeconds())
            timeUnit = TimeUnit.WEEK;
        else if (totalTimeTaken < TimeUnit.YEAR.getTimeInMilliSeconds())
            timeUnit = TimeUnit.MONTH;
        else if (totalTimeTaken < TimeUnit.DECADE.getTimeInMilliSeconds())
            timeUnit = TimeUnit.YEAR;

        totalTimeTaken /= timeUnit.getTimeInMilliSeconds();

    }

    public double getTotalTimeTaken() {
        return totalTimeTaken;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
