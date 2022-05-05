package immersive_airships.network.c2s;

import immersive_airships.cobalt.network.Message;
import immersive_airships.entity.AirshipEntity;
import net.minecraft.entity.player.PlayerEntity;

public class EnginePowerMessage implements Message {
    private final float engineTarget;

    public EnginePowerMessage(float engineTarget) {
        this.engineTarget = engineTarget;
    }

    @Override
    public void receive(PlayerEntity e) {
        if (e.getRootVehicle() instanceof AirshipEntity entity) {
            entity.setEngineTarget(engineTarget);
        }
    }
}
