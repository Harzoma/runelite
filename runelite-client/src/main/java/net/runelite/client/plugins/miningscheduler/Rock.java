package net.runelite.client.plugins.miningscheduler;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Rock
{
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final RockType type;
    private final WorldPoint location;

    @Getter
    private Boolean isDepleted;
    private LocalDateTime nextRespawn;

    public Rock(RockType type, WorldPoint location)
    {
        System.out.println("Registering " + type + " at " + location.toString());
        this.type = type;
        this.location = location;
        isDepleted = false;
        nextRespawn = null;
    }

    public String getType()
    {
        return this.type.toString();
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
        return this.type.getRespawnTime();
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

    public void deplete(boolean depletedOnLogin)
    {
        System.out.println("Depleted " + this.getType() + " at " + location.toString());
        isDepleted = true;
        if (!depletedOnLogin) {
            nextRespawn = LocalDateTime.now().plusMinutes(this.getRespawnDuration());
            System.out.println("Next respawn at " +  this.getNextRespawnTime().format(dateFormat));
        }
    }

    public void respawn()
    {
        isDepleted = false;
        nextRespawn = null;
        System.out.println("Respawned " + this.getType() + " at " + location.toString());
    }

    public boolean isRespawnUnknown()
    {
        return this.isDepleted && this.nextRespawn == null;
    }
}
