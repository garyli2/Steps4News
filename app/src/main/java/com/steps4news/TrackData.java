package com.steps4news;

public class TrackData
{
    private static TrackData one = null;

    public boolean isWorkoutActive = false;
    public long mSteps = 0; // steps in current session
    public int initialCounterStepsWhenRegistered = 0; // Value of the step counter sensor when the listener was registered. //

    private TrackData(){}

    public static TrackData getInstance()
    {
        if (one == null) {
            one = new TrackData();
        }

        return one;
    }
}
