package net.runelite.client.plugins.miningscheduler;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;

import java.util.Set;

public enum RockType
{
    //DEPLETED        (ImmutableSet.of(7469, 7468), -1),
    //IRON            (ImmutableSet.of(7455, 7488)),
    //COAL            (ImmutableSet.of(7456, 7489)),
    //MITHRIL         (ImmutableSet.of(7459, 7492), 2),
    ADAMANTITE      (ImmutableSet.of(7460, 7493), 4),
    RUNITE          (ImmutableSet.of(7461, 7494), 12);

    @Getter
    private final Set gameIds;

    @Getter
    private final Integer respawnTime;

    RockType(Set gameIds, Integer respawnTime)
    {
        this.gameIds = gameIds;
        this.respawnTime = respawnTime;
    }

    public static RockType getRock(int id)
    {
        for (RockType rock : values()) {
            if (rock.gameIds.contains(id))
            {
                return rock;
            }
        }
        return null;
    }

    public String getName()
    {
        return this.name().toLowerCase();
    }
}
