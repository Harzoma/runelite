package net.runelite.client.plugins.miningscheduler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
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
 * TODO LIST:
 *  - Rename validDepletion variable to something better
 *  - Implement Schedule Manager class and move timer methods to it
 *  - Add warning when trying to hop too many times in a row
 *  - Refactor logic into smaller classes (timer manager)
 *  - Implement cleanup method when loading a new area
 *  - TESTS!
 *
 * BUG LIST:
 *  - Adjust timer and respawn (there are some delays sometimes)
 *  - Restrict mining guild half respawn time regions to P2P only
 */

@Slf4j
public class MiningSchedulerPlugin extends Plugin
{
    private static final Logger logger = LoggerFactory.getLogger(MiningSchedulerPlugin.class);

    @Inject
    private Client client;

    @Inject
    private ItemManager itemManager;

    @Inject
    private Notifier notifier;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private InfoBoxManager infoBoxManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MiningSchedulerOverlay overlay;

    @Inject
    private MiningSchedulerConfig config;

    @Provides
    MiningSchedulerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MiningSchedulerConfig.class);
    }

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<Integer, Map<WorldPoint, Rock>> rockStates = new HashMap<>();
    private final Queue<RespawnTimer> timers = new LinkedList<>();


    private Map<WorldPoint, Rock> rocks;
    private RespawnTimer currentTimer;
    private RockType currentTargetType;
    private int currentWorld;
    private boolean hasLoaded;
    private boolean alreadyTicked;

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        logger.info("Started Runite Plugin");
        currentWorld = client.getWorld();
        currentTargetType = config.targetRockType();
        hasLoaded = false;
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        reset();
    }

    private void reset()
    {
        logger.info("Resetting...");
        rockStates.clear();
        timers.clear();
        infoBoxManager.removeInfoBox(currentTimer);
        currentTimer = null;
        client.clearHintArrow();
    }

    public void debugRocks(int world) {
        logger.info("\n\nDebugging rock states for world " + world);

        for (WorldPoint location : rockStates.get(world).keySet()) {
            Rock rock = rockStates.get(world).get(location);
            if (rock.isDepleted()) {
                logger.info("Depleted " + rock.getTypeString() + " at " + location.toString());
                if (rock.getNextRespawnTime() != null)
                {
                    logger.info("Respawns at " + rock.getNextRespawnTime().format(dateFormat));
                }

            }
            else {
                logger.info(rock.getTypeString() + " at " + location.toString());
            }
        }
        logger.info("\n\n");
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        alreadyTicked = true;

        /* Distance debug * /
        if (!rocks.isEmpty())
        {
            WorldPoint rockLoc = rocks.entrySet().iterator().next().getValue().getLocation();
            //logger.info("" + client.getLocalPlayer().getWorldLocation().isInScene(client));
            logger.info("" + rockLoc.distanceTo2D(client.getLocalPlayer().getWorldLocation()));
        }
        /* */

        if (currentTimer == null || currentTimer.getEnd().isBefore(Instant.now()))
        {
            //todo refactor dumb logic below
            if (currentTimer != null)
            {
                String chatMessage = "Respawned rock at " + currentTimer.getTooltip();
                sendChatMessage(chatMessage, ChatColorType.HIGHLIGHT, ChatMessageType.GAME);
                infoBoxManager.removeInfoBox(currentTimer);
                client.clearHintArrow();
            }
            currentTimer = timers.poll();
            if (currentTimer != null) {
                infoBoxManager.addInfoBox(currentTimer);
            }
        }
        else if ( config.displayRespawnArrow() &&
                    currentWorld == currentTimer.getWorld() &&
                    currentTimer.getEnd().minusSeconds(config.getRespawnArrowTime() + 1).isBefore(Instant.now()))
        {
                client.setHintArrow(currentTimer.getRock().getLocation());

        }
    }

    private void sendChatMessage(String chatMessage, ChatColorType colorType, ChatMessageType messageType)
    {
        final String message = new ChatMessageBuilder()
                .append(colorType)
                .append(chatMessage)
                .build();

        chatMessageManager.queue(QueuedMessage.builder()
                .type(messageType)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void cleanupRocks(Map<WorldPoint, Rock> rocks)
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

    private void onLoading()
    {
        alreadyTicked = false;
        hasLoaded = false;
        currentWorld = client.getWorld();
        if (!rockStates.containsKey(currentWorld))
        {
            logger.info("Creating new rocks state for world " + currentWorld);
            Map<WorldPoint, Rock> newRockState = new HashMap<>();
            rockStates.put(currentWorld, newRockState);
            rocks = newRockState;
        }
        else
        {
            logger.info("Loading rocks state for world " + currentWorld);
            rocks = rockStates.get(currentWorld);
            cleanupRocks(rocks);
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        logger.info("" + event.getGameState());
        switch (event.getGameState()) {
            case LOADING:
                onLoading();
                //todo remove debug code below
                //for (int world : rockStates.keySet())
                //    debugRocks(world);
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

        if (currentTargetType.getGameIds().contains(objectId))
        {
            if (rocks.containsKey(location))
            {
                Rock rock = rocks.get(location);
                if (rock.isDepleted())
                {
                    if (hasLoaded || rock.isRespawnUnknown())
                    {
                        if (config.showNotifications())
                        {
                            notifier.notify("Respawned rock!");
                        }
                        rock.respawn();
                    }
                }
            }
            else
            {
                Rock newRock = new Rock(currentTargetType, location);
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

        if (hasLoaded && currentTargetType.getGameIds().contains(objectId))
        {
            Rock rock;

            if (rocks.containsKey(location))
            {
                rock = rocks.get(location);
            }
            else
            {
                rock = new Rock(currentTargetType, location);
            }

            if (!rock.isDepleted())
            {
                boolean validDepletion = alreadyTicked &&
                        rock.getLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()) <= 3;

                rock.deplete(validDepletion);
                if (validDepletion)
                {
                    if (config.showNotifications())
                    {
                        notifier.notify("Depleted rock!");
                    }

                    RespawnTimer timer = new RespawnTimer(rock,
                                                          currentWorld,
                                                          itemManager.getImage(rock.getType().getIconId()),
                                                          this);
                    timer.setTooltip("World " + Integer.toString(this.currentWorld));
                    timers.add(timer);
                }
            }
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals("miningscheduler"))
        {
            if (currentTargetType != config.targetRockType())
            {
                logger.info("Changed target type to: " + config.targetRockType().getName());
                currentTargetType = config.targetRockType();
                reset();
            }
        }
    }
}
