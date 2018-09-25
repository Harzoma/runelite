package net.runelite.client.plugins.miningscheduler;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class Rock
{
    private static final Logger logger = LoggerFactory.getLogger(MiningSchedulerPlugin.class);

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Set<Integer> MINING_GUILD_REGIONS = ImmutableSet.of(11927, 11928, 12183, 12184, 12439, 12440);

    private final RockType type;
    private final WorldPoint location;

    @Getter
    private Boolean isDepleted;
    private LocalDateTime nextRespawn;
    private Boolean isInMiningGuild;

    public Rock(RockType type, WorldPoint location)
    {
        logger.info("Registering " + type + " at " + location.toString());
        this.type = type;
        this.location = location;
        isDepleted = false;
        isInMiningGuild = isInMiningGuild(location);
        nextRespawn = null;
    }

    public static boolean isInMiningGuild(WorldPoint location)
    {
        return MINING_GUILD_REGIONS.contains(location.getRegionID());
    }

    public RockType getType()
    {
        return this.type;
    }

    public String getTypeString()
    {
        return this.type.toString();
    }

    public WorldPoint getLocation()
    {
        return this.location;
    }

    public long getRespawnDuration()
    {
        long respawnTime = this.type.getRespawnTime();
        return isInMiningGuild ? respawnTime / 2 : respawnTime;
    }

    public LocalDateTime getNextRespawnTime()
    {
        return this.nextRespawn;
    }

    public String getNextRespawnTimeString()
    {
        return this.nextRespawn.format(dateFormat);
    }

    public boolean isDepleted()
    {
        return isDepleted;
    }

    public void deplete(boolean validDepletion)
    {
        logger.info("Depleted " + this.getType() + " at " + location.toString());
        isDepleted = true;
        if (validDepletion)
        {
            nextRespawn = LocalDateTime.now().plusSeconds(this.getRespawnDuration());
            logger.info("Next respawn at " +  this.getNextRespawnTime().format(dateFormat));
        }
    }

    public void respawn()
    {
        isDepleted = false;
        nextRespawn = null;
        logger.info("Respawned " + this.getType() + " at " + location.toString());
    }

    public boolean isRespawnUnknown()
    {
        return this.isDepleted && this.nextRespawn == null;
    }
}
