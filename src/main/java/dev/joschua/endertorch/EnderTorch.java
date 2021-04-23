package dev.joschua.endertorch;

import dev.joschua.endertorch.database.TorchDatabase;
import dev.joschua.endertorch.event.EnderTorchListener;
import dev.joschua.endertorch.item.EnderTorchItem;
import org.mineacademy.fo.plugin.SimplePlugin;

public class EnderTorch extends SimplePlugin {
    @Override
    protected void onPluginStart() {
        TorchDatabase.getInstance().connect();

        final EnderTorchItem enderTorchItem = new EnderTorchItem(this);

        registerEvents(new EnderTorchListener());
    }
}
