package net.runelite.client.plugins.miningscheduler;

import lombok.Getter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Timer;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RespawnTimer extends Timer {

    @Getter
    private final Instant start;

    @Getter
    private final Instant end;

    @Getter
    private final int world;

    @Getter
    private final Rock rock;

    public RespawnTimer(Rock rock, int world, BufferedImage image, Plugin plugin)
    {
        super(rock.getRespawnDuration(),
                ChronoUnit.SECONDS,
                image,
                plugin);
        start = Instant.now();
        end = start.plus(rock.getRespawnDuration(), ChronoUnit.SECONDS);
        this.world = world;
        this.rock = rock;
    }
}
