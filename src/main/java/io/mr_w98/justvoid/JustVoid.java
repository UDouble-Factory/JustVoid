package io.mr_w98.justvoid;

import plugin.PluginMetadata;
import plugin.PumpkinPlugin;
import pumpkin.context.Context;
import pumpkin.logging.Level;
import pumpkin.logging.Logging;
import pumpkin.runtime.Result;

import java.util.List;

public class JustVoid extends PumpkinPlugin {

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "JustVoid",
                "0.0.1",
                List.of("mr-w98"),
                "Quickly create a void world.",
                List.of(),
                List.of()
        );
    }

    @Override
    public void initPlugin() {
        Logging.log(Level.INFO, "Initialized JustVoid");
    }

    @Override
    public Result<Void, String> onLoad(Context context) {
        return Result.success(null);
    }

    @Override
    public Result<Void, String> onUnload(Context context) {
        return Result.success(null);
    }
}
