package de.onebacon.drops_into_shulker;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropsIntoShulker implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("DropsIntoShulker");

    @Override
    public void onInitialize() {
        LOGGER.info("DropsIntoShulker loaded!");
    }
}