package immersive_aircraft.forge;

import immersive_aircraft.ClientMain;
import immersive_aircraft.Main;
import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.forge.cobalt.registration.RegistrationImpl.DataLoaderRegister;
import immersive_aircraft.item.upgrade.AircraftStat;
import immersive_aircraft.item.upgrade.AircraftUpgrade;
import immersive_aircraft.item.upgrade.AircraftUpgradeRegistry;
import immersive_aircraft.network.s2c.AircraftBaseUpgradesMessage;
import immersive_aircraft.network.s2c.AircraftUpgradesMessage;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class ForgeBusEvents {

    public static DataLoaderRegister DATA_REGISTRY; // Require access to the DataLoaderRegister here as forge uses events, could put this in RegistrationImpl but it would just be messy
    private static final DecimalFormat fmt = new DecimalFormat("+#;-#");
    public static boolean firstLoad = true;

    @SubscribeEvent
    public static void onClientStart(TickEvent.ClientTickEvent event) {
        //forge decided to be funny and won't trigger the client load event
        if (firstLoad) {
            ClientMain.postLoad();
            firstLoad = false;
        }

        ClientMain.tick();
    }

    @SubscribeEvent
    public static void addReloadListenerEvent(AddReloadListenerEvent event) {
        if(DATA_REGISTRY != null) {
            for(JsonDataLoader loader : DATA_REGISTRY.getLoaders())
                event.addListener(loader);
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if(event.getPlayer() != null) { // Syncing aircraft upgrades to players.
            NetworkHandler.sendToPlayer(new AircraftUpgradesMessage(), event.getPlayer());
            NetworkHandler.sendToPlayer(new AircraftBaseUpgradesMessage(), event.getPlayer());
        }
        else {
            for(ServerPlayerEntity player : event.getPlayerList().getPlayerList()) {
                NetworkHandler.sendToPlayer(new AircraftUpgradesMessage(), player);
                NetworkHandler.sendToPlayer(new AircraftBaseUpgradesMessage(), player);
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltips(ItemTooltipEvent event) {
        AircraftUpgrade upgrade = AircraftUpgradeRegistry.INSTANCE.getUpgrade(event.getItemStack().getItem());
        if(upgrade != null) {
            List<Text> tooltip = event.getToolTip();

            tooltip.add(Text.translatable("item.immersive_aircraft.item.upgrade").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));

            for (Map.Entry<AircraftStat, Float> entry : upgrade.getAll().entrySet()) {
                tooltip.add(Text.translatable("immersive_aircraft.upgrade." + entry.getKey().name().toLowerCase(Locale.ROOT),
                        fmt.format(entry.getValue() * 100)
                ).formatted(entry.getValue() * (entry.getKey().isPositive() ? 1 : -1) > 0 ? Formatting.GREEN : Formatting.RED));
            }
        }
    }

}
