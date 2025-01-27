package immersive_aircraft.entity;

import immersive_aircraft.Items;
import immersive_aircraft.Sounds;
import immersive_aircraft.entity.misc.AircraftProperties;
import immersive_aircraft.entity.misc.VehicleInventoryDescription;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class GyrodyneEntity extends Rotorcraft {
    private static final float PUSH_SPEED = 0.25f;

    private final AircraftProperties properties = new AircraftProperties(this)
            .setYawSpeed(5.0f)
            .setPitchSpeed(5.0f)
            .setEngineSpeed(0.3f)
            .setVerticalSpeed(0.04f)
            .setDriftDrag(0.01f)
            .setLift(0.1f)
            .setRollFactor(30.0f)
            .setWindSensitivity(0.05f)
            .setMass(4.0f);

    private static final VehicleInventoryDescription inventoryDescription = new VehicleInventoryDescription()
            .addSlot(VehicleInventoryDescription.SlotType.WEAPON, 8 + 6, 8 + 6)
            .addSlot(VehicleInventoryDescription.SlotType.UPGRADE, 8 + 28, 8 + 6)
            .addSlot(VehicleInventoryDescription.SlotType.UPGRADE, 8 + 6, 8 + 6 + 22)
            .addSlot(VehicleInventoryDescription.SlotType.UPGRADE, 8 + 28, 8 + 6 + 22)
            .addSlots(VehicleInventoryDescription.SlotType.INVENTORY, 8 + 18 * 3, 8, 6, 3)
            .build();

    @Override
    public VehicleInventoryDescription getInventoryDescription() {
        return inventoryDescription;
    }

    @Override
    public GUI_STYLE getGuiStyle() {
        return GUI_STYLE.NONE;
    }

    public GyrodyneEntity(EntityType<? extends AircraftEntity> entityType, Level world) {
        super(entityType, world, false);
    }

    protected SoundEvent getEngineStartSound() {
        return Sounds.WOOSH.get();
    }

    protected SoundEvent getEngineSound() {
        return Sounds.WOOSH.get();
    }

    @Override
    protected float getStabilizer() {
        return 0.3f;
    }

    @Override
    public AircraftProperties getProperties() {
        return properties;
    }

    @Override
    protected float getGroundVelocityDecay() {
        return falloffGroundVelocityDecay(0.8f);
    }

    @Override
    protected float getHorizontalVelocityDelay() {
        return 0.925f;
    }

    @Override
    protected float getVerticalVelocityDelay() {
        return 0.9f;
    }

    @Override
    public Item asItem() {
        return Items.GYRODYNE.get();
    }

    final List<List<Vec3>> PASSENGER_POSITIONS = List.of(
            List.of(
                    new Vec3(0.0f, -0.1f, 0.3f)
            ),
            List.of(
                    new Vec3(0.0f, -0.1f, 0.3f),
                    new Vec3(0.0f, -0.1f, -0.6f)
            )
    );

    protected List<List<Vec3>> getPassengerPositions() {
        return PASSENGER_POSITIONS;
    }

    @Override
    protected float getGravity() {
        return (1.0f - getEnginePower()) * super.getGravity();
    }

    private void updateEnginePowerTooltip() {
        if (getControllingPassenger() instanceof Player player && player.getLevel().isClientSide && getFuelUtilization() > 0.0) {
            player.displayClientMessage(Component.translatable("immersive_aircraft.gyrodyne_target", (int) (getEngineTarget() * 100.f + 0.5f)), true);
        }
    }

    @Override
    protected String getFuelType() {
        return "fat";
    }

    @Override
    protected boolean isFuelLow() {
        return false;
    }

    @Override
    protected void updateController() {
        super.updateController();

        // launch that engine
        if (getEngineTarget() < 1.0f) {
            setEngineTarget(Math.max(0.0f, Math.min(1.0f, getEngineTarget() + pressingInterpolatedZ.getValue() * 0.05f - 0.035f)));
            updateEnginePowerTooltip();

            if (getEngineTarget() == 1.0 && getControllingPassenger() instanceof Player player && player.getLevel().isClientSide) {
                player.displayClientMessage(Component.translatable("immersive_aircraft.gyrodyne_target_reached"), true);
                if (onGround) {
                    setDeltaMovement(getDeltaMovement().add(0, 0.25f, 0));
                }
            }
        }

        // up and down
        float power = getEnginePower() * properties.getVerticalSpeed() * pressingInterpolatedY.getSmooth();
        setDeltaMovement(getDeltaMovement().add(getTopDirection().scale(power)));

        // get direction
        Vec3 direction = getForwardDirection();

        // speed
        float sin = Mth.sin(getXRot() * ((float) Math.PI / 180));
        float thrust = (float) (Math.pow(getEnginePower(), 2.0) * properties.getEngineSpeed()) * sin;
        if (onGround && getEngineTarget() < 1.0) {
            thrust = PUSH_SPEED / (1.0f + (float) getDeltaMovement().length() * 5.0f) * pressingInterpolatedZ.getSmooth() * (pressingInterpolatedZ.getSmooth() > 0.0 ? 1.0f : 0.5f) * getEnginePower();
        }

        // accelerate
        setDeltaMovement(getDeltaMovement().add(direction.scale(thrust)));
    }

    @Override
    public void tick() {
        super.tick();

        if (getControllingPassenger() instanceof ServerPlayer player) {
            float consumption = getFuelConsumption() * 0.025f;
            player.getFoodData().addExhaustion(consumption);
        }
    }

    @Override
    public float getFuelUtilization() {
        if (getControllingPassenger() instanceof Player player && player.getFoodData().getFoodLevel() > 5) {
            return 1.0f;
        }
        return 0.0f;
    }
}
