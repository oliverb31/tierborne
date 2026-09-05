package com.ollie.tierborne.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.ollie.tierborne.entity.OrcMob;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reads the original Blockbench geometry and animation timelines at runtime. */
public final class OrcModel extends EntityModel<OrcMob> {
    private static final Point ZERO = new Point(0.0F, 0.0F, 0.0F);
    private static final Point ONE = new Point(1.0F, 1.0F, 1.0F);

    private final List<Bone> roots;
    private final Map<String, Animation> animations;
    private final float textureWidth;
    private final float textureHeight;
    private OrcMob entity;
    private float ageInTicks;
    private String manualAnimationName;
    private float manualAnimationTime;

    public OrcModel(ResourceLocation modelLocation) {
        ModelData data = loadModel(modelLocation);
        this.roots = data.roots;
        this.animations = data.animations;
        this.textureWidth = data.textureWidth;
        this.textureHeight = data.textureHeight;
    }

    @Override
    public void setupAnim(OrcMob entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.entity = entity;
        this.ageInTicks = ageInTicks;
        this.manualAnimationName = null;
    }

    public void setupManualAnimation(String animationName, float animationTime) {
        this.entity = null;
        this.manualAnimationName = animationName;
        this.manualAnimationTime = animationTime;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        AnimationState state = animationState();
        for (Bone root : this.roots) {
            renderBone(root, ZERO, state, poseStack, consumer, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private AnimationState animationState() {
        String animationName;
        float time;
        if (this.entity != null) {
            animationName = this.entity.getAnimationName();
            time = this.entity.hasActiveAttackAnimation()
                    ? Math.max(0.0F, (this.ageInTicks - this.entity.getAnimationStartTick()) / 20.0F)
                    : this.ageInTicks / 20.0F;
        } else if (this.manualAnimationName != null) {
            animationName = this.manualAnimationName;
            time = this.manualAnimationTime;
        } else {
            return new AnimationState(null, "", 0.0F);
        }

        Animation animation = this.animations.get(animationName);
        if (animation == null) return new AnimationState(null, animationName, 0.0F);
        if (animation.length > 0.0F) {
            if (animation.loop.equals("loop")) time = time % animation.length;
            else time = Math.min(time, animation.length);
        }
        return new AnimationState(animation, animationName, time);
    }

    private void renderBone(Bone bone, Point parentOrigin, AnimationState state, PoseStack poseStack,
                            VertexConsumer consumer, int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate((bone.origin.x - parentOrigin.x) / 16.0F,
                -(bone.origin.y - parentOrigin.y) / 16.0F,
                (bone.origin.z - parentOrigin.z) / 16.0F);
        applyRotation(poseStack, bone.rotation);
        applyAnimation(poseStack, state, bone.uuid, bone.name);

        for (Cube cube : bone.cubes) {
            renderCube(cube, bone.origin, poseStack, consumer, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        for (Bone child : bone.children) {
            renderBone(child, bone.origin, state, poseStack, consumer, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private void applyAnimation(PoseStack poseStack, AnimationState state, String boneUuid, String boneName) {
        if (state.animation == null) return;
        BoneAnimation animation = state.animation.bones.get(boneUuid);
        if (animation == null) return;
        Point position = evaluate(animation.channels.get("position"), state.time, ZERO);
        Point rotation = evaluate(animation.channels.get("rotation"), state.time, ZERO);
        Point scale = evaluate(animation.channels.get("scale"), state.time, ONE);
        if (state.name.equals("walk") && boneName.equals("torso")) {
            rotation = new Point(0.0F, rotation.y * 0.35F, 0.0F);
        }
        poseStack.translate(position.x / 16.0F, -position.y / 16.0F, position.z / 16.0F);
        applyRotation(poseStack, rotation);
        poseStack.scale(scale.x, scale.y, scale.z);
    }

    private static Point evaluate(List<Keyframe> keyframes, float time, Point fallback) {
        if (keyframes == null || keyframes.isEmpty()) return fallback;
        if (time <= keyframes.get(0).time) return keyframes.get(0).value;
        if (time >= keyframes.get(keyframes.size() - 1).time) return keyframes.get(keyframes.size() - 1).value;
        int nextIndex = 1;
        while (nextIndex < keyframes.size() && keyframes.get(nextIndex).time < time) nextIndex++;
        Keyframe previous = keyframes.get(nextIndex - 1);
        Keyframe next = keyframes.get(nextIndex);
        float span = Math.max(0.0001F, next.time - previous.time);
        float progress = Mth.clamp((time - previous.time) / span, 0.0F, 1.0F);
        if (previous.interpolation.equals("step")) return previous.value;
        if (previous.interpolation.equals("catmullrom")) {
            Point before = keyframes.get(Math.max(0, nextIndex - 2)).value;
            Point after = keyframes.get(Math.min(keyframes.size() - 1, nextIndex + 1)).value;
            return catmull(before, previous.value, next.value, after, progress);
        }
        if (previous.interpolation.equals("bezier")) {
            return hermite(previous, next, span, progress);
        }
        return lerp(previous.value, next.value, progress);
    }

    private static Point lerp(Point from, Point to, float progress) {
        return new Point(Mth.lerp(progress, from.x, to.x), Mth.lerp(progress, from.y, to.y),
                Mth.lerp(progress, from.z, to.z));
    }

    private static Point catmull(Point a, Point b, Point c, Point d, float t) {
        return new Point(Mth.catmullrom(t, a.x, b.x, c.x, d.x),
                Mth.catmullrom(t, a.y, b.y, c.y, d.y),
                Mth.catmullrom(t, a.z, b.z, c.z, d.z));
    }

    private static Point hermite(Keyframe from, Keyframe to, float span, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        float h00 = 2.0F * t3 - 3.0F * t2 + 1.0F;
        float h10 = t3 - 2.0F * t2 + t;
        float h01 = -2.0F * t3 + 3.0F * t2;
        float h11 = t3 - t2;
        Point fromTangent = tangent(from.bezierRightValue, from.bezierRightTime);
        Point toTangent = tangent(to.bezierLeftValue, to.bezierLeftTime);
        return new Point(
                h00 * from.value.x + h10 * span * fromTangent.x + h01 * to.value.x + h11 * span * toTangent.x,
                h00 * from.value.y + h10 * span * fromTangent.y + h01 * to.value.y + h11 * span * toTangent.y,
                h00 * from.value.z + h10 * span * fromTangent.z + h01 * to.value.z + h11 * span * toTangent.z);
    }

    private static Point tangent(Point value, Point time) {
        return new Point(safeDivide(value.x, time.x), safeDivide(value.y, time.y), safeDivide(value.z, time.z));
    }

    private static float safeDivide(float value, float time) {
        return Math.abs(time) < 0.0001F ? 0.0F : value / time;
    }

    private void renderCube(Cube cube, Point parentOrigin, PoseStack poseStack,
                            VertexConsumer consumer, int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate((cube.origin.x - parentOrigin.x) / 16.0F,
                -(cube.origin.y - parentOrigin.y) / 16.0F,
                (cube.origin.z - parentOrigin.z) / 16.0F);
        applyRotation(poseStack, cube.rotation);
        float x1 = (cube.from.x - cube.origin.x - cube.inflate) / 16.0F;
        float x2 = (cube.to.x - cube.origin.x + cube.inflate) / 16.0F;
        float y1 = -(cube.from.y - cube.origin.y - cube.inflate) / 16.0F;
        float y2 = -(cube.to.y - cube.origin.y + cube.inflate) / 16.0F;
        float z1 = (cube.from.z - cube.origin.z - cube.inflate) / 16.0F;
        float z2 = (cube.to.z - cube.origin.z + cube.inflate) / 16.0F;

        renderFace(cube.faces.get("north"), poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha,
                0, 0, -1, x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1);
        renderFace(cube.faces.get("south"), poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha,
                0, 0, 1, x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2);
        renderFace(cube.faces.get("east"), poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha,
                1, 0, 0, x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1);
        renderFace(cube.faces.get("west"), poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha,
                -1, 0, 0, x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2);
        renderFace(cube.faces.get("up"), poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha,
                0, -1, 0, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        renderFace(cube.faces.get("down"), poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha,
                0, 1, 0, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2);
        poseStack.popPose();
    }

    private void renderFace(Face face, PoseStack poseStack, VertexConsumer consumer,
                            int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                            float normalX, float normalY, float normalZ, float... vertices) {
        if (face == null || face.texture < 0) return;
        float[][] uv = {
                {face.u1 / this.textureWidth, face.v1 / this.textureHeight},
                {face.u1 / this.textureWidth, face.v2 / this.textureHeight},
                {face.u2 / this.textureWidth, face.v2 / this.textureHeight},
                {face.u2 / this.textureWidth, face.v1 / this.textureHeight}
        };
        int turns = Math.floorMod(face.rotation / 90, 4);
        PoseStack.Pose pose = poseStack.last();
        for (int index = 0; index < 4; index++) {
            int uvIndex = Math.floorMod(index - turns, 4);
            consumer.vertex(pose.pose(), vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2])
                    .color(red, green, blue, alpha).uv(uv[uvIndex][0], uv[uvIndex][1])
                    .overlayCoords(packedOverlay).uv2(packedLight)
                    .normal(pose.normal(), normalX, normalY, normalZ).endVertex();
        }
    }

    private static void applyRotation(PoseStack poseStack, Point rotation) {
        if (rotation.z != 0.0F) poseStack.mulPose(Vector3f.ZP.rotationDegrees(-rotation.z));
        if (rotation.y != 0.0F) poseStack.mulPose(Vector3f.YP.rotationDegrees(rotation.y));
        if (rotation.x != 0.0F) poseStack.mulPose(Vector3f.XP.rotationDegrees(-rotation.x));
    }

    private static ModelData loadModel(ResourceLocation location) {
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(location)) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, JsonObject> elements = indexByUuid(model.getAsJsonArray("elements"));
            Map<String, JsonObject> groups = model.has("groups")
                    ? indexByUuid(model.getAsJsonArray("groups")) : Map.of();
            List<Bone> roots = new ArrayList<>();
            for (JsonElement root : model.getAsJsonArray("outliner")) {
                if (root.isJsonObject()) roots.add(parseBone(root.getAsJsonObject(), elements, groups));
            }
            JsonObject resolution = model.getAsJsonObject("resolution");
            return new ModelData(List.copyOf(roots), parseAnimations(model.getAsJsonArray("animations")),
                    resolution.get("width").getAsFloat(), resolution.get("height").getAsFloat());
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load animated Blockbench model " + location, exception);
        }
    }

    private static Map<String, Animation> parseAnimations(JsonArray entries) {
        Map<String, Animation> animations = new HashMap<>();
        if (entries == null) return Map.of();
        for (JsonElement entry : entries) {
            JsonObject value = entry.getAsJsonObject();
            Map<String, BoneAnimation> bones = new HashMap<>();
            for (Map.Entry<String, JsonElement> animatorEntry : value.getAsJsonObject("animators").entrySet()) {
                JsonObject animator = animatorEntry.getValue().getAsJsonObject();
                if (!animator.has("type") || !animator.get("type").getAsString().equals("bone")) continue;
                Map<String, List<Keyframe>> channels = new HashMap<>();
                for (JsonElement keyframeElement : animator.getAsJsonArray("keyframes")) {
                    JsonObject keyframe = keyframeElement.getAsJsonObject();
                    String channel = keyframe.get("channel").getAsString();
                    if (!channel.equals("position") && !channel.equals("rotation") && !channel.equals("scale")) continue;
                    JsonArray points = keyframe.getAsJsonArray("data_points");
                    if (points == null || points.isEmpty()) continue;
                    channels.computeIfAbsent(channel, ignored -> new ArrayList<>()).add(new Keyframe(
                            keyframe.get("time").getAsFloat(), point(points.get(0).getAsJsonObject(), null),
                            keyframe.has("interpolation") ? keyframe.get("interpolation").getAsString() : "linear",
                            arrayPoint(keyframe, "bezier_left_time"), arrayPoint(keyframe, "bezier_left_value"),
                            arrayPoint(keyframe, "bezier_right_time"), arrayPoint(keyframe, "bezier_right_value")));
                }
                channels.values().forEach(list -> list.sort(Comparator.comparingDouble(Keyframe::time)));
                bones.put(animatorEntry.getKey(), new BoneAnimation(Map.copyOf(channels)));
            }
            animations.put(value.get("name").getAsString(), new Animation(
                    value.get("length").getAsFloat(), value.has("loop") ? value.get("loop").getAsString() : "once",
                    Map.copyOf(bones)));
        }
        return Map.copyOf(animations);
    }

    private static Map<String, JsonObject> indexByUuid(JsonArray entries) {
        Map<String, JsonObject> indexed = new HashMap<>();
        for (JsonElement entry : entries) {
            JsonObject object = entry.getAsJsonObject();
            indexed.put(object.get("uuid").getAsString(), object);
        }
        return indexed;
    }

    private static Bone parseBone(JsonObject outliner, Map<String, JsonObject> elements,
                                  Map<String, JsonObject> groups) {
        String uuid = outliner.get("uuid").getAsString();
        JsonObject definition = groups.getOrDefault(uuid, outliner);
        List<Cube> cubes = new ArrayList<>();
        List<Bone> children = new ArrayList<>();
        for (JsonElement child : outliner.getAsJsonArray("children")) {
            if (child.isJsonPrimitive()) {
                JsonObject element = elements.get(child.getAsString());
                if (element != null && (!element.has("visibility") || element.get("visibility").getAsBoolean())) {
                    cubes.add(parseCube(element));
                }
            } else if (child.isJsonObject()) {
                children.add(parseBone(child.getAsJsonObject(), elements, groups));
            }
        }
        return new Bone(uuid, definition.has("name") ? definition.get("name").getAsString() : "",
                point(definition, "origin"), point(definition, "rotation"),
                List.copyOf(cubes), List.copyOf(children));
    }

    private static Cube parseCube(JsonObject element) {
        Map<String, Face> faces = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces").entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            JsonArray uv = face.getAsJsonArray("uv");
            faces.put(entry.getKey().toLowerCase(Locale.ROOT), new Face(
                    uv.get(0).getAsFloat(), uv.get(1).getAsFloat(), uv.get(2).getAsFloat(), uv.get(3).getAsFloat(),
                    face.has("texture") && !face.get("texture").isJsonNull() ? face.get("texture").getAsInt() : -1,
                    face.has("rotation") ? face.get("rotation").getAsInt() : 0));
        }
        return new Cube(point(element, "from"), point(element, "to"), point(element, "origin"),
                point(element, "rotation"), element.has("inflate") ? element.get("inflate").getAsFloat() : 0.0F,
                Map.copyOf(faces));
    }

    private static Point point(JsonObject object, String key) {
        if (key != null) {
            if (!object.has(key)) return ZERO;
            return arrayPoint(object, key);
        }
        return new Point(number(object.get("x")), number(object.get("y")), number(object.get("z")));
    }

    private static Point arrayPoint(JsonObject object, String key) {
        if (!object.has(key)) return ZERO;
        JsonArray values = object.getAsJsonArray(key);
        return new Point(number(values.get(0)), number(values.get(1)), number(values.get(2)));
    }

    private static float number(JsonElement element) {
        try {
            return element.getAsFloat();
        } catch (RuntimeException exception) {
            return 0.0F;
        }
    }

    private record ModelData(List<Bone> roots, Map<String, Animation> animations,
                             float textureWidth, float textureHeight) {}
    private record Animation(float length, String loop, Map<String, BoneAnimation> bones) {}
    private record AnimationState(Animation animation, String name, float time) {}
    private record BoneAnimation(Map<String, List<Keyframe>> channels) {}
    private record Keyframe(float time, Point value, String interpolation, Point bezierLeftTime,
                            Point bezierLeftValue, Point bezierRightTime, Point bezierRightValue) {}
    private record Point(float x, float y, float z) {}
    private record Bone(String uuid, String name, Point origin, Point rotation,
                        List<Cube> cubes, List<Bone> children) {}
    private record Face(float u1, float v1, float u2, float v2, int texture, int rotation) {}
    private record Cube(Point from, Point to, Point origin, Point rotation, float inflate, Map<String, Face> faces) {}
}
