package dev.joschua.endertorch;

import dev.joschua.endertorch.database.TorchLocationDatabase;
import dev.joschua.endertorch.event.EnderTorchListener;
import dev.joschua.endertorch.item.EnderTorchItem;
import org.mineacademy.fo.plugin.SimplePlugin;

public class EnderTorch extends SimplePlugin {
    @Override
    protected void onPluginStart() {

        final TorchLocationDatabase torchLocationDatabase = new TorchLocationDatabase();

        torchLocationDatabase.connect();

        final EnderTorchItem enderTorchItem = new EnderTorchItem(this);

        registerEvents(new EnderTorchListener());
    }
}
