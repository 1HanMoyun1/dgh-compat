package com.hanmoyun.dghcompat.corpse;

import com.hanmoyun.dghcompat.CompatMod;
import com.hanmoyun.dghcompat.StasisPatients;
import com.lastimp.dgh.common.capability.HealthCapability;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@EventBusSubscriber(modid = CompatMod.ID)
public final class Corpses {
    private static final java.util.List<Runnable> PENDING = new java.util.ArrayList<>();

    @SubscribeEvent
    public static void finishDeaths(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        var ready = java.util.List.copyOf(PENDING);
        PENDING.clear();
        ready.forEach(Runnable::run);
    }

    private static final java.util.Map<net.minecraft.world.entity.LivingEntity, CompoundTag>
            SNAPSHOTS = new java.util.WeakHashMap<>();

    public static void capture(net.minecraft.world.entity.LivingEntity source) {
        if (!source.level().isClientSide() && !isCorpse(source) && HealthCapability.has(source))
            SNAPSHOTS.put(
                    source,
                    HealthCapability.getAndApply(
                            source, h -> h.serialize(source.registryAccess()), new CompoundTag()));
    }

    public static boolean onSurgeryBed(LivingEntity patient) {
        return patient.getVehicle() instanceof com.lastimp.dgh.common.entity.StretcherEntity carrier
                && com.hanmoyun.dghcompat.PatientTransport.bed(carrier) != null;
    }

    public static boolean permitsSurgery(
            LivingEntity patient, net.minecraft.world.item.ItemStack stack) {
        if (!onSurgeryBed(patient)) return false;
        return switch (stack.getItem()) {
            case com.lastimp.dgh.common.item.tool.Scalpel ignored -> true;
            case com.lastimp.dgh.common.item.tool.Retractor ignored -> true;
            case com.lastimp.dgh.common.item.tool.Hemostat ignored -> true;
            case com.lastimp.dgh.common.item.tool.SurgerySaw ignored -> true;
            case com.lastimp.dgh.common.item.tool.SurgicalDrill ignored -> true;
            case com.lastimp.dgh.common.item.tool.Tweezer ignored -> true;
            case com.lastimp.dgh.common.item.tool.SurgeryBones ignored -> true;
            case com.lastimp.dgh.common.item.tool.BoneImplants ignored -> true;
            case com.lastimp.dgh.common.item.medicine.Sutures ignored -> true;
            case com.lastimp.dgh.common.item.medicine.PlasticSkin ignored -> true;
            default -> false;
        };
    }

    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CompatMod.ID);
    public static final Supplier<EntityType<CorpseEntity>> TYPE =
            TYPES.register(
                    "surgical_corpse",
                    () ->
                            EntityType.Builder.of(CorpseEntity::new, MobCategory.MISC)
                                    .sized(0.6f, 1.8f)
                                    .clientTrackingRange(10)
                                    .updateInterval(1)
                                    .build("surgical_corpse"));

    public static final Supplier<EntityType<CorpseLoot>> LOOT =
            TYPES.register(
                    "corpse_loot",
                    () ->
                            EntityType.Builder.of(CorpseLoot::new, MobCategory.MISC)
                                    .sized(0.2f, 0.2f)
                                    .clientTrackingRange(10)
                                    .build("corpse_loot"));

    private Corpses() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    public static boolean isCorpse(Entity entity) {
        return entity instanceof CorpseEntity;
    }

    public static boolean needsCorpse(LivingEntity entity) {
        return !isCorpse(entity) && HealthCapability.has(entity);
    }

    public static CompoundTag deathHealth(net.minecraft.world.entity.LivingEntity source) {
        CompoundTag saved = SNAPSHOTS.get(source);
        return saved != null
                ? saved.copy()
                : HealthCapability.getAndApply(
                        source, h -> h.serialize(source.registryAccess()), new CompoundTag());
    }

    public static boolean blocksTreatment(Entity entity) {
        return StasisPatients.wrapped(entity) && !isCorpse(entity);
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), net.minecraft.world.entity.Mob.createMobAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        var source = event.getEntity();
        if (!(source.level() instanceof ServerLevel level)
                || isCorpse(source)
                || !needsCorpse(source)) return;
        // Copy once after confirmed death callbacks, but do not copy equipment, inventory or XP.
        CompoundTag saved = SNAPSHOTS.get(source);
        final CompoundTag health =
                saved != null
                        ? saved
                        : HealthCapability.getAndApply(
                                source,
                                h -> h.serialize(source.registryAccess()),
                                new CompoundTag());
        var carrier =
                source.getVehicle()
                                instanceof com.lastimp.dgh.common.entity.StretcherEntity stretcher
                        ? stretcher
                        : null;
        var name = source.getName().copy();
        var position = source.position();
        float yaw = source.getYRot();
        var sourceId = source.getUUID();
        CompoundTag visual = new CompoundTag();
        visual.putString(
                "type",
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                        .getKey(source.getType())
                        .toString());
        visual.putUUID("owner", sourceId);
        if (source instanceof net.minecraft.world.entity.player.Player player)
            com.hanmoyun.dghcompat.CyberPatientData.captureCorpseSkin(player, visual);
        visual.putString("name", source.getName().getString());
        visual.putBoolean("baby", source.isBaby());
        visual.putLong("fall_time", level.getGameTime());
        visual.putFloat(
                "physics_scale", com.hanmoyun.dghcompat.integration.RagdollBodies.scale(source));
        visual.putInt("source_entity", source.getId());
        visual.putBoolean("loot_only", !HealthCapability.has(source));
        if (com.hanmoyun.dghcompat.integration.RagdollBodies.active(source))
            com.hanmoyun.dghcompat.integration.DeathPose.capture(source, visual);
        boolean packed = StasisPatients.wrapped(source);
        var limbImplants = com.hanmoyun.dghcompat.BodyItems.limbImplants(source);
        PENDING.add(
                () -> {
                    SNAPSHOTS.remove(source);

                    if (!source.isDeadOrDying() || source.isAlive()) {

                        return;
                    }
                    var corpse = TYPE.get().create(level);
                    if (corpse == null) {

                        return;
                    }
                    corpse.moveTo(position.x, position.y, position.z, yaw, 0);
                    corpse.setCustomName(name);
                    corpse.visual(visual);
                    com.hanmoyun.dghcompat.BodyItems.rememberLimbs(corpse, limbImplants);
                    corpse.getPersistentData().putString("dgh_version", "version_1.3.0");
                    corpse.getPersistentData().putUUID("dghcompat:deceased_uuid", sourceId);
                    HealthCapability.getAndApply(
                            corpse,
                            h -> {
                                h.deserialize(corpse.registryAccess(), health);
                                // DGH already drops equipment on its normal death path.
                                // Never duplicate it.
                                if (!packed)
                                    h.getAutoPulse()
                                            .setStackInSlot(
                                                    0, net.minecraft.world.item.ItemStack.EMPTY);
                                h.getOxygenMask()
                                        .setStackInSlot(
                                                0, net.minecraft.world.item.ItemStack.EMPTY);
                            });
                    StasisPatients.setVisual(corpse, packed);
                    if (level.addFreshEntity(corpse)) {
                        if (carrier != null
                                && carrier.isAlive()
                                && (carrier.getPatient() == source
                                        || carrier.getPatient() == null)) {
                            if (source.getVehicle() == carrier) source.stopRiding();
                            corpse.startRiding(carrier, true);
                        }

                        StasisPatients.sync(corpse);
                    }
                });
    }
}
