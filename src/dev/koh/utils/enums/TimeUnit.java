package dev.koh.utils.enums;

public enum TimeUnit {

    NANO_SECOND("ns", 1),
    MICRO_SECOND("us", NANO_SECOND.getTimeInMilliSeconds() * 1000),
    MILLI_SECOND("ms", MICRO_SECOND.getTimeInMilliSeconds() * 1000),
    SECOND("s", MILLI_SECOND.getTimeInMilliSeconds() * 1000),
    MINUTE("min", 60 * SECOND.timeInMilliSeconds),
    HOUR("hr", 60 * MINUTE.timeInMilliSeconds),
    DAY("day", 24 * HOUR.timeInMilliSeconds),
    WEEK("week", 7 * DAY.timeInMilliSeconds),
    MONTH("month", 30 * DAY.timeInMilliSeconds),
    YEAR("year", 12 * MONTH.timeInMilliSeconds),
    DECADE("decade", 10 * YEAR.timeInMilliSeconds);

    private final String unit;
    private final double timeInMilliSeconds;

    TimeUnit(String unit, double timeInMilliSeconds) {
        this.unit = unit;
        this.timeInMilliSeconds = timeInMilliSeconds;
    }

    public String getUnit() {
        return unit;
    }

    public double getTimeInMilliSeconds() {
        return timeInMilliSeconds;
    }

    @Override
    public String toString() {
        return getUnit();
    }
}