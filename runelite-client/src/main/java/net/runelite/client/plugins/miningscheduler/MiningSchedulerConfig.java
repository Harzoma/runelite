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
            position = 4
    )
    default RockType targetRockType()
    {
        return RockType.RUNITE;
    }

}
