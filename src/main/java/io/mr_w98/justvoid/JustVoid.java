package io.mr_w98.justvoid;

import io.mr_w98.justvoid.config.ConfigException;
import io.mr_w98.justvoid.config.VoidConfig;
import io.mr_w98.justvoid.platform.Platform;
import plugin.PluginMetadata;
import plugin.PumpkinPlugin;
import pumpkin.biomes.Biome;
import pumpkin.common.BlockPos;
import pumpkin.context.Context;
import pumpkin.event.Event;
import pumpkin.event.EventPriority;
import pumpkin.event.EventType;
import pumpkin.logging.Level;
import pumpkin.logging.Logging;
import pumpkin.runtime.Option;
import pumpkin.runtime.Result;
import pumpkin.server.Server;
import pumpkin.world.ChunkBuffer;
import pumpkin.world.GenerationPhase;
import pumpkin.world.World;
import pumpkin.world.WorldApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JustVoid extends PumpkinPlugin {

    private static final long WORLD_INIT_HANDLER_ID = 0;

    private final Map<Long, Platform> platforms = new HashMap<>();
    private long nextGeneratorId;
    private VoidConfig config;
    private int bedrockStateId;

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "JustVoid",
                "0.0.1",
                List.of("mr-w98"),
                "Quickly create a void world.",
                List.of(),
                List.of("fs.read.data", "fs.write.data")
        );
    }

    @Override
    public Result<Void, String> onLoad(Context context) {
        try (context) {
            Option<Integer> bedrock = WorldApi.resolveBlockState("minecraft:bedrock", List.of());
            if (!bedrock.isSome()) return Result.failure("Could not find the bedrock block.");
            bedrockStateId = bedrock.value();

            try {
                config = VoidConfig.load(context.getDataFolder());
            } catch (ConfigException e) {
                return Result.failure(e.getMessage());
            }

            context.registerEvent(WORLD_INIT_HANDLER_ID, EventType.WORLD_INIT_EVENT, EventPriority.NORMAL, true);

            try (Server server = context.getServer()) {
                List<World> worlds = server.getAllWorlds();
                for (int i = 0; i < worlds.size(); i++) {
                    try (World world = worlds.get(i)) {
                        applyIfVoid(world, i == 0);
                    }
                }
            }
        }
        return Result.success(null);
    }

    @Override
    public Event handleEvent(long eventId, Server server, Event event) {
        try (server) {
            if (eventId == WORLD_INIT_HANDLER_ID && event instanceof Event.WorldInitEvent init) {
                applyIfVoid(init.value().targetWorld(), false);
            }
        }
        return event;
    }

    @Override
    public void handleGeneratePhase(long generatorId, GenerationPhase phase, ChunkBuffer chunk) {
        try (chunk) {
            if (phase == GenerationPhase.BIOMES) {
                chunk.fillBiome(Biome.THE_VOID);
            } else if (phase == GenerationPhase.NOISE) {
                Platform platform = platforms.get(generatorId);
                if (platform != null) {
                    platform.place(chunk, bedrockStateId);
                }
            }
        }
    }

    private void applyIfVoid(World world, boolean isDefaultWorld) {
        String name = world.getName();
        String dimension = world.getDimension();
        if (!(isDefaultWorld && config.defaultWorld()) && !config.matches(name, dimension)) return;

        BlockPos spawn = world.getSpawnLocation().pos();
        long generatorId = nextGeneratorId++;
        platforms.put(generatorId, Platform.under(spawn, config.platformSize()));
        world.setChunkGenerator(generatorId);

        Logging.log(Level.INFO, "Void world enabled: " + name + " (" + dimension + "), platform at " + spawn.x() + ", " + (spawn.y() - 1) + ", " + spawn.z());
    }
}
