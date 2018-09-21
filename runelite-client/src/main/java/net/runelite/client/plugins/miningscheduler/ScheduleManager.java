package net.runelite.client.plugins.miningscheduler;

import java.util.LinkedList;
import java.util.Queue;

public class ScheduleManager {

    private final Queue<RespawnTimer> timers = new LinkedList<>();
    private RespawnTimer currentTimer;

    public ScheduleManager()
    {
        currentTimer = null;
    }

    public void pushTimer()
    {

    }

    public void popTimer()
    {

    }

}
