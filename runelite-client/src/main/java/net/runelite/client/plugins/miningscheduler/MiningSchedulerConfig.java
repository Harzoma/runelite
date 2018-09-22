package net.runelite.client.plugins.miningscheduler;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;


@ConfigGroup("miningscheduler")
public interface MiningSchedulerConfig extends Config
{
    @ConfigItem(
            position = 1,
            keyName = "showNotifications",
            name = "Show notifications",
            description = "Display rock respawn/depletion notifications"
    )
    default boolean showNotifications()
    {
        return true;
    }

    @ConfigItem(
            keyName = "targetRockType",
            name = "Target rock type",
            description = "Determines which type of rock will be tracked",
            position = 2
    )
    default RockType targetRockType()
    {
        return RockType.RUNITE;
    }

    @ConfigItem(
            keyName = "respawnArrow",
            name = "Display respawn arrow",
            description = "Determines if a hint arrow should be displayed before the next rock respawns",
            position = 3
    )
    default boolean displayRespawnArrow()
    {
        return true;
    }

    @ConfigItem(
            keyName = "respawnArrowTime",
            name = "Respawn arrow time",
            description = "Determines time in seconds before the next rock respawns the hint arrow should be displayed",
            position = 4
    )
    default int getRespawnArrowTime()
    {
        return 20;
    }

}
