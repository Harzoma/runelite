package net.runelite.client.plugins.miningscheduler;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.Set;

public enum RockType
{
    COAL            (ImmutableSet.of(7456, 7489), 30,  ItemID.COAL),
    MITHRIL         (ImmutableSet.of(7459, 7492), 120, ItemID.MITHRIL_ORE),
    ADAMANTITE      (ImmutableSet.of(7460, 7493), 240, ItemID.ADAMANTITE_ORE),
    RUNITE          (ImmutableSet.of(7461, 7494), 720, ItemID.RUNITE_ORE);

    @Getter
    private final Set gameIds;

    @Getter
    private final long respawnTime;

    @Getter
    private final Integer iconId;

    RockType(Set gameIds, Integer respawnTime, Integer iconId)
    {
        this.gameIds = gameIds;
        this.respawnTime = respawnTime;
        this.iconId = iconId;
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
