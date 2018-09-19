package net.runelite.client.plugins.miningscheduler;

import lombok.Getter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Timer;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RespawnTimer extends Timer {

    @Getter
    private final Instant start;

    @Getter
    private final Instant end;

    public RespawnTimer(long period, ChronoUnit unit, BufferedImage image, Plugin plugin) {
        super(period, unit, image, plugin);
        start = Instant.now();
        end = start.plus(period, unit);
    }
}
