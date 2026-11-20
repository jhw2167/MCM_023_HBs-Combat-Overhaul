package com.holybuckets.combat.client;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import com.holybuckets.combat.Constants;
import com.holybuckets.combat.core.DashContext;
import com.holybuckets.combat.core.DashDirection;
import com.holybuckets.combat.core.DashPhase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.keyframe.AnimationPoint;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientDashRenderer {

    public static final ResourceLocation ANIMATION_FILE = new ResourceLocation(Constants.MOD_ID, "animations/player_dash.animation.json");

    private static final float FADE_IN_TICKS = 1.0F;
    private static final float FADE_OUT_TICKS = 2.0F;
    private static final float ANTICIPATION_LEVEL = 0.35F;
    private static final float MAX_ROLL_DEGREES = 6.0F;
    private static final float MAX_PITCH_DEGREES = 2.5F;
    private static final float FOV_KICK_FORWARD = 0.12F;
    private static final float FOV_KICK_BACK = -0.04F;
    private static final float FOV_KICK_SIDE = 0.07F;

    private static final DustParticleOptions WIND_MOTE = new DustParticleOptions(new Vector3f(0.55F, 0.92F, 1.0F), 0.9F);
    private static final int MOTES_PER_TICK = 3;
    private static final int PUFFS_PER_TICK = 2;

    private static final float[] HIP_PIVOT = {0.0F, 12.0F, 0.0F};

    private enum Channel { X, Y, Z }

    public static void applyPose(PlayerModel<?> model, Player player, float ageInTicks) {
        if (ageInTicks == 0.0F) return;
        DashContext ctx = ClientDashManager.getActive(player);
        if (ctx == null) return;
        Animation animation = getAnimation(ctx);
        if (animation == null) return;

        float partialTick = Mth.clamp(ageInTicks - player.tickCount, 0.0F, 1.0F);
        float time = ctx.timeline(partialTick);
        float total = DashPhase.totalTicks(ctx.profile);
        double animTick = (time / total) * animation.length();
        float weight = Mth.clamp(Math.min(time / FADE_IN_TICKS, (total - time) / FADE_OUT_TICKS), 0.0F, 1.0F);

        Map<String, BoneAnimation> bones = new HashMap<>();
        for (BoneAnimation bone : animation.boneAnimations()) bones.put(bone.boneName(), bone);

        BonePose body = pose(bones, animTick, "body");
        double[] bodyRot = rotation(body.rx, body.ry, body.rz);
        float[] bodyOffset = {body.px, body.py, body.pz};

        PartPose bodyBase = model.body.getInitialPose();
        float[] bodyPivot = attach(bodyRot, bodyOffset, new float[]{bodyBase.x, bodyBase.y, bodyBase.z}, BonePose.REST);
        blend(model.body, bodyPivot, bodyRot, weight);

        applyChild(model.head, pose(bones, animTick, "head"), bodyRot, bodyOffset, weight, true);
        applyChild(model.rightArm, pose(bones, animTick, "right_arm", "rightArm"), bodyRot, bodyOffset, weight, false);
        applyChild(model.leftArm, pose(bones, animTick, "left_arm", "leftArm"), bodyRot, bodyOffset, weight, false);
        applyRoot(model.rightLeg, pose(bones, animTick, "right_leg", "rightLeg"), weight);
        applyRoot(model.leftLeg, pose(bones, animTick, "left_leg", "leftLeg"), weight);

        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    public static void applyCameraTilt(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        DashContext ctx = ClientDashManager.getActive(mc.player);
        if (ctx == null) return;

        float level = intensity(ctx, partialTick) * mc.options.screenEffectScale().get().floatValue();
        float roll = -ctx.direction.strafe * MAX_ROLL_DEGREES * level;
        float pitch = ctx.direction.forward * MAX_PITCH_DEGREES * level;
        if (roll != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        if (pitch != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    public static float getFovFactor(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player) return 1.0F;
        DashContext ctx = ClientDashManager.getActive(player);
        if (ctx == null) return 1.0F;

        float kick = switch (ctx.direction) {
            case FORWARD -> FOV_KICK_FORWARD;
            case BACK -> FOV_KICK_BACK;
            default -> FOV_KICK_SIDE;
        };
        return 1.0F + kick * intensity(ctx, 0.0F) * mc.options.fovEffectScale().get().floatValue();
    }

    public static void spawnGlideParticles(Player player, DashContext ctx) {
        Level level = player.level();
        RandomSource random = player.getRandom();
        Vec3 back = ctx.glideMotion.lengthSqr() > 1.0E-6D ? ctx.glideMotion.normalize().scale(-1.0D) : Vec3.ZERO;
        double width = player.getBbWidth();
        double height = player.getBbHeight();

        for (int i = 0; i < MOTES_PER_TICK; i++) {
            double x = player.getX() + back.x * 0.6D + (random.nextDouble() - 0.5D) * width;
            double y = player.getY() + random.nextDouble() * height;
            double z = player.getZ() + back.z * 0.6D + (random.nextDouble() - 0.5D) * width;
            level.addParticle(WIND_MOTE, x, y, z, back.x * 0.2D, 0.0D, back.z * 0.2D);
        }

        for (int i = 0; i < PUFFS_PER_TICK; i++) {
            double x = player.getX() + back.x * 0.4D + (random.nextDouble() - 0.5D) * width;
            double y = player.getY() + 0.1D;
            double z = player.getZ() + back.z * 0.4D + (random.nextDouble() - 0.5D) * width;
            level.addParticle(ParticleTypes.CLOUD, x, y, z, back.x * 0.15D, 0.02D, back.z * 0.15D);
        }
    }

    private static float intensity(DashContext ctx, float partialTick) {
        float p = ctx.phaseProgress(partialTick);
        return switch (ctx.phase) {
            case ANTICIPATION -> ANTICIPATION_LEVEL * smooth(p);
            case GLIDE -> ANTICIPATION_LEVEL + (1.0F - ANTICIPATION_LEVEL) * smooth(Math.min(1.0F, p * 2.5F));
            case RECOVERY -> 1.0F - smooth(p);
            default -> 0.0F;
        };
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static Animation getAnimation(DashContext ctx) {
        BakedAnimations baked = GeckoLibCache.getBakedAnimations().get(ANIMATION_FILE);
        if (baked == null) return null;
        String prefix = ctx.profile.animationPrefix;
        String[] names = {prefix + ctx.direction.animation, prefix + DashDirection.FALLBACK_ANIMATION,
            ctx.direction.animation, DashDirection.FALLBACK_ANIMATION};
        for (String name : names) {
            Animation animation = baked.getAnimation(name);
            if (animation != null) return animation;
        }
        return null;
    }

    private record BonePose(float rx, float ry, float rz, float px, float py, float pz) {
        static final BonePose REST = new BonePose(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private static BonePose pose(Map<String, BoneAnimation> bones, double tick, String... names) {
        for (String name : names) {
            BoneAnimation bone = bones.get(name);
            if (bone == null) continue;
            return new BonePose(
                -sample(bone.rotationKeyFrames().xKeyframes(), tick, true, Channel.X),
                -sample(bone.rotationKeyFrames().yKeyframes(), tick, true, Channel.Y),
                sample(bone.rotationKeyFrames().zKeyframes(), tick, true, Channel.Z),
                sample(bone.positionKeyFrames().xKeyframes(), tick, false, Channel.X),
                -sample(bone.positionKeyFrames().yKeyframes(), tick, false, Channel.Y),
                sample(bone.positionKeyFrames().zKeyframes(), tick, false, Channel.Z));
        }
        return BonePose.REST;
    }

    private static void applyChild(ModelPart part, BonePose pose, double[] parentRot, float[] parentOffset,
                                   float weight, boolean keepLook) {
        PartPose base = part.getInitialPose();
        double[] rot = multiply(parentRot, rotation(pose.rx, pose.ry, pose.rz));
        if (keepLook) rot = multiply(rot, rotation(part.xRot, part.yRot, 0.0F));
        float[] pivot = attach(parentRot, parentOffset, new float[]{base.x, base.y, base.z}, pose);
        blend(part, pivot, rot, weight);
    }

    private static void applyRoot(ModelPart part, BonePose pose, float weight) {
        PartPose base = part.getInitialPose();
        float[] pivot = {base.x + pose.px, base.y + pose.py, base.z + pose.pz};
        blend(part, pivot, rotation(base.xRot + pose.rx, base.yRot + pose.ry, base.zRot + pose.rz), weight);
    }

    private static float[] attach(double[] parentRot, float[] parentOffset, float[] restPivot, BonePose pose) {
        double lx = restPivot[0] + pose.px - HIP_PIVOT[0];
        double ly = restPivot[1] + pose.py - HIP_PIVOT[1];
        double lz = restPivot[2] + pose.pz - HIP_PIVOT[2];
        return new float[]{
            (float) (parentOffset[0] + HIP_PIVOT[0] + parentRot[0] * lx + parentRot[1] * ly + parentRot[2] * lz),
            (float) (parentOffset[1] + HIP_PIVOT[1] + parentRot[3] * lx + parentRot[4] * ly + parentRot[5] * lz),
            (float) (parentOffset[2] + HIP_PIVOT[2] + parentRot[6] * lx + parentRot[7] * ly + parentRot[8] * lz)
        };
    }

    private static void blend(ModelPart part, float[] pivot, double[] rot, float weight) {
        float[] euler = toEulerZYX(rot);
        part.xRot = Mth.lerp(weight, part.xRot, euler[0]);
        part.yRot = Mth.lerp(weight, part.yRot, euler[1]);
        part.zRot = Mth.lerp(weight, part.zRot, euler[2]);
        part.x = Mth.lerp(weight, part.x, pivot[0]);
        part.y = Mth.lerp(weight, part.y, pivot[1]);
        part.z = Mth.lerp(weight, part.z, pivot[2]);
    }

    private static double[] rotation(float x, float y, float z) {
        double cx = Math.cos(x), sx = Math.sin(x);
        double cy = Math.cos(y), sy = Math.sin(y);
        double cz = Math.cos(z), sz = Math.sin(z);
        return new double[]{
            cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx,
            sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx,
            -sy, cy * sx, cy * cx
        };
    }

    private static double[] multiply(double[] a, double[] b) {
        double[] out = new double[9];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                out[row * 3 + col] = a[row * 3] * b[col] + a[row * 3 + 1] * b[3 + col] + a[row * 3 + 2] * b[6 + col];
            }
        }
        return out;
    }

    private static float[] toEulerZYX(double[] m) {
        double sy = Mth.clamp(-m[6], -1.0D, 1.0D);
        double y = Math.asin(sy);
        if (Math.abs(sy) < 0.9999D) {
            return new float[]{(float) Math.atan2(m[7], m[8]), (float) y, (float) Math.atan2(m[3], m[0])};
        }
        return new float[]{0.0F, (float) y, (float) Math.atan2(-m[1], m[4])};
    }

    private static float sample(List<Keyframe<IValue>> frames, double tick, boolean rotation, Channel channel) {
        if (frames.isEmpty()) return 0.0F;

        Keyframe<IValue> frame = frames.get(frames.size() - 1);
        double local = frame.length();
        double total = 0.0D;
        for (Keyframe<IValue> candidate : frames) {
            total += candidate.length();
            if (total > tick) {
                frame = candidate;
                local = tick - (total - candidate.length());
                break;
            }
        }

        double start = frame.startValue().get();
        double end = frame.endValue().get();
        if (rotation) {
            if (!(frame.startValue() instanceof Constant)) start = toGeckoRadians(start, channel);
            if (!(frame.endValue() instanceof Constant)) end = toGeckoRadians(end, channel);
        }

        return (float) EasingType.lerpWithOverride(new AnimationPoint(frame, local, frame.length(), start, end), null);
    }

    private static double toGeckoRadians(double degrees, Channel channel) {
        double radians = Math.toRadians(degrees);
        return channel == Channel.Z ? radians : -radians;
    }
}
