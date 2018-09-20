package net.runelite.client.plugins.miningscheduler;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;

@PluginDescriptor(
        name = "Mining scheduler",
        description = "Scheduling plugin for mining ores",
        tags = {"skilling", "mining", "ore", "pickaxe", "schedule"}
)


/**
 * TODO:
 *  - Add configuration (notification on, target rocks, max timers in queue)
 *  - Add warning when trying to hop too many times in a row
 *  - Refactor logic into smaller classes
 */

@Slf4j
public class MiningSchedulerPlugin extends Plugin
{
    private static final Logger logger = LoggerFactory.getLogger(MiningSchedulerPlugin.class);

    @Inject
    private Client client;

    @Inject
    private Notifier notifier;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private InfoBoxManager infoBoxManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MiningSchedulerOverlay overlay;

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<Integer, Map<WorldPoint, Rock>> rockStates = new HashMap<>();
    private final Queue<RespawnTimer> timers = new LinkedList<>();


    private Map<WorldPoint, Rock> rocks;
    private RespawnTimer currentTimer;
    private Integer currentWorld;
    private boolean hasLoaded;
    private boolean alreadyTicked;

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        System.out.println("Started Runite Plugin");
        currentWorld = client.getWorld();
        hasLoaded = false;
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        rockStates.clear();
        timers.clear();
    }

    public void debugRocks(int world) {
        System.out.println("\n\nDebugging rock states for world " + world);

        for (WorldPoint location : rockStates.get(world).keySet()) {
            Rock rock = rockStates.get(world).get(location);
            if (rock.isDepleted()) {
                System.out.println("Depleted " + rock.getTypeString() + " at " + location.toString());
                if (rock.getNextRespawnTime() != null)
                {
                    System.out.println("Respawns at " + rock.getNextRespawnTime().format(dateFormat));
                }

            }
            else {
                System.out.println(rock.getTypeString() + " at " + location.toString());
            }
        }
        System.out.println("\n\n");
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        alreadyTicked = true;

        if (currentTimer == null || currentTimer.getEnd().isBefore(Instant.now()))
        {
            //todo refactor dumb logic below
            if (currentTimer != null)
            {
                String chatMessage = "Respawned rock at " + currentTimer.getTooltip();

                final String message = new ChatMessageBuilder()
                        .append(ChatColorType.HIGHLIGHT)
                        .append(chatMessage)
                        .build();

                chatMessageManager.queue(QueuedMessage.builder()
                        .type(ChatMessageType.GAME)
                        .runeLiteFormattedMessage(message)
                        .build());
                infoBoxManager.removeInfoBox(currentTimer);
            }
            currentTimer = timers.poll();
            if (currentTimer != null) {
                infoBoxManager.addInfoBox(currentTimer);
            }
        }
    }

    public void cleanupRocks(Map<WorldPoint, Rock> rocks)
    {
        for (Entry<WorldPoint, Rock> entry : rocks.entrySet())
        {
            Rock rock = entry.getValue();
            if (rock.isDepleted() &&
                    rock.getNextRespawnTime() != null &&
                    LocalDateTime.now().isAfter(rock.getNextRespawnTime()))
            {
                rock.respawn();
            }
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        System.out.println(event.getGameState());
        switch (event.getGameState()) {
            case LOADING:
                //TODO create method for logic below
                chatMessageManager.
                alreadyTicked = false;
                hasLoaded = false;
                currentWorld = client.getWorld();
                if (!rockStates.containsKey(currentWorld))
                {
                    System.out.println("Creating new rocks state for world " + currentWorld);
                    Map<WorldPoint, Rock> newRockState = new HashMap<>();
                    rockStates.put(currentWorld, newRockState);
                    rocks = newRockState;
                }
                else
                {
                    System.out.println("Loading rocks state for world " + currentWorld);
                    rocks = rockStates.get(currentWorld);
                    cleanupRocks(rocks);
                }
                //todo remove debug code below
                for (int world : rockStates.keySet())
                    debugRocks(world);
                break;
            case LOGGED_IN:
                hasLoaded = true;
                break;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject gameObject = event.getGameObject();
        int objectId = gameObject.getId();
        WorldPoint location = gameObject.getWorldLocation();
        RockType rockType = RockType.getRock(objectId);

        if (rockType != null)
        {
            if (rocks.containsKey(location))
            {
                Rock rock = rocks.get(location);
                if (rock.isDepleted())
                {
                    if (hasLoaded || rock.isRespawnUnknown())
                    {
                        rock.respawn();
                    }
                }
            }
            else
            {
                Rock newRock = new Rock(rockType, location);
                rocks.put(location, newRock);
            }
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        GameObject gameObject = event.getGameObject();
        int objectId = gameObject.getId();
        WorldPoint location = gameObject.getWorldLocation();
        RockType rockType = RockType.getRock(objectId);

        if (rockType != null)
        {
            Rock rock = rocks.get(location);
            if (!rock.isDepleted())
            {
                rock.deplete(!alreadyTicked);
                if (alreadyTicked)
                {
                    String chatMessage = "Depleted rock!";

                    final String message = new ChatMessageBuilder()
                            .append(ChatColorType.HIGHLIGHT)
                            .append(chatMessage)
                            .build();
                    notifier.notify(chatMessage);
                    chatMessageManager.queue(QueuedMessage.builder()
                            .type(ChatMessageType.GAME)
                            .runeLiteFormattedMessage(message)
                            .build());
                    RespawnTimer timer = new RespawnTimer(rock.getRespawnDuration(),
                                                          ChronoUnit.MINUTES,
                                                          itemManager.getImage(ItemID.DRAGON_PICKAXE),
                                                        this);
                    timer.setTooltip("World " + Integer.toString(this.currentWorld));
                    timers.add(timer);
                }
            }
        }
    }
}
