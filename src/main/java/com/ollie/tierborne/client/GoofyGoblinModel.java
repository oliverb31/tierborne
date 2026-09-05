package com.ollie.tierborne.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.entity.GoofyGoblin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders the supplied free-format Blockbench model without requiring a model library. */
public final class GoofyGoblinModel extends EntityModel<GoofyGoblin> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(Tierborne.MOD_ID, "models/entity/goofy_goblin.bbmodel");
    private static final float TEXTURE_SIZE = 64.0F;
    private static final Point ZERO = new Point(0.0F, 0.0F, 0.0F);

    private final List<Bone> roots;
    private float limbSwing;
    private float limbSwingAmount;
    private float netHeadYaw;
    private float headPitch;

    public GoofyGoblinModel() {
        this.roots = loadModel();
    }

    @Override
    public void setupAnim(GoofyGoblin entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.netHeadYaw = netHeadYaw;
        this.headPitch = headPitch;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        for (Bone root : this.roots) {
            renderBone(root, ZERO, poseStack, consumer, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private void renderBone(Bone bone, Point parentOrigin, PoseStack poseStack, VertexConsumer consumer,
                            int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate((bone.origin.x - parentOrigin.x) / 16.0F,
                -(bone.origin.y - parentOrigin.y) / 16.0F,
                (bone.origin.z - parentOrigin.z) / 16.0F);
        applyRotation(poseStack, bone.rotation);
        applyAnimation(poseStack, bone.name);

        for (Cube cube : bone.cubes) {
            renderCube(cube, bone.origin, poseStack, consumer, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        for (Bone child : bone.children) {
            renderBone(child, bone.origin, poseStack, consumer, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private void applyAnimation(PoseStack poseStack, String boneName) {
        float walk = Mth.cos(this.limbSwing * 0.6662F) * 1.15F * this.limbSwingAmount;
        switch (boneName) {
            case "Head" -> {
                poseStack.mulPose(Vector3f.YP.rotation(this.netHeadYaw * Mth.DEG_TO_RAD));
                poseStack.mulPose(Vector3f.XP.rotation(-this.headPitch * Mth.DEG_TO_RAD));
            }
            case "L-Thigh" -> poseStack.mulPose(Vector3f.XP.rotation(-walk));
            case "R-Thigh" -> poseStack.mulPose(Vector3f.XP.rotation(walk));
            case "L-Arm" -> poseStack.mulPose(Vector3f.XP.rotation(walk * 0.7F));
            case "R-Arm" -> poseStack.mulPose(Vector3f.XP.rotation(-walk * 0.7F));
            default -> {
            }
        }
    }

    private static void renderCube(Cube cube, Point parentOrigin, PoseStack poseStack,
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

        renderFace(cube.faces.get("north"), poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha, 0.0F, 0.0F, -1.0F,
                x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1);
        renderFace(cube.faces.get("south"), poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha, 0.0F, 0.0F, 1.0F,
                x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2);
        renderFace(cube.faces.get("east"), poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha, 1.0F, 0.0F, 0.0F,
                x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1);
        renderFace(cube.faces.get("west"), poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha, -1.0F, 0.0F, 0.0F,
                x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2);
        renderFace(cube.faces.get("up"), poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha, 0.0F, -1.0F, 0.0F,
                x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        renderFace(cube.faces.get("down"), poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha, 0.0F, 1.0F, 0.0F,
                x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2);
        poseStack.popPose();
    }

    private static void renderFace(Face face, PoseStack poseStack, VertexConsumer consumer,
                                   int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha,
                                   float normalX, float normalY, float normalZ, float... vertices) {
        if (face == null || face.texture < 0) {
            return;
        }

        float[][] uv = {
                {face.u1 / TEXTURE_SIZE, face.v1 / TEXTURE_SIZE},
                {face.u1 / TEXTURE_SIZE, face.v2 / TEXTURE_SIZE},
                {face.u2 / TEXTURE_SIZE, face.v2 / TEXTURE_SIZE},
                {face.u2 / TEXTURE_SIZE, face.v1 / TEXTURE_SIZE}
        };
        int turns = Math.floorMod(face.rotation / 90, 4);
        PoseStack.Pose pose = poseStack.last();
        for (int index = 0; index < 4; index++) {
            int uvIndex = Math.floorMod(index - turns, 4);
            consumer.vertex(pose.pose(), vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2])
                    .color(red, green, blue, alpha)
                    .uv(uv[uvIndex][0], uv[uvIndex][1])
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(pose.normal(), normalX, normalY, normalZ)
                    .endVertex();
        }
    }

    private static void applyRotation(PoseStack poseStack, Point rotation) {
        if (rotation.z != 0.0F) {
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(-rotation.z));
        }
        if (rotation.y != 0.0F) {
            poseStack.mulPose(Vector3f.YP.rotationDegrees(rotation.y));
        }
        if (rotation.x != 0.0F) {
            poseStack.mulPose(Vector3f.XP.rotationDegrees(-rotation.x));
        }
    }

    private static List<Bone> loadModel() {
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(MODEL)) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, JsonObject> elements = indexByUuid(model.getAsJsonArray("elements"));
            Map<String, JsonObject> groups = indexByUuid(model.getAsJsonArray("groups"));
            List<Bone> roots = new ArrayList<>();
            for (JsonElement root : model.getAsJsonArray("outliner")) {
                if (root.isJsonObject()) {
                    roots.add(parseBone(root.getAsJsonObject(), elements, groups));
                }
            }
            return List.copyOf(roots);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load the Goofy Goblin Blockbench model", exception);
        }
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
        JsonObject definition = groups.get(uuid);
        if (definition == null) {
            throw new IllegalArgumentException("Missing Blockbench group " + uuid);
        }

        List<Cube> cubes = new ArrayList<>();
        List<Bone> children = new ArrayList<>();
        for (JsonElement child : outliner.getAsJsonArray("children")) {
            if (child.isJsonPrimitive()) {
                JsonObject element = elements.get(child.getAsString());
                if (element != null
                        && (!element.has("visibility") || element.get("visibility").getAsBoolean())) {
                    cubes.add(parseCube(element));
                }
            } else if (child.isJsonObject()) {
                children.add(parseBone(child.getAsJsonObject(), elements, groups));
            }
        }
        return new Bone(definition.get("name").getAsString(), point(definition, "origin"),
                point(definition, "rotation"), List.copyOf(cubes), List.copyOf(children));
    }

    private static Cube parseCube(JsonObject element) {
        Map<String, Face> faces = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces").entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            JsonArray uv = face.getAsJsonArray("uv");
            faces.put(entry.getKey().toLowerCase(Locale.ROOT), new Face(
                    uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
                    uv.get(2).getAsFloat(), uv.get(3).getAsFloat(),
                    face.get("texture").getAsInt(),
                    face.has("rotation") ? face.get("rotation").getAsInt() : 0));
        }
        return new Cube(point(element, "from"), point(element, "to"), point(element, "origin"),
                point(element, "rotation"), element.has("inflate") ? element.get("inflate").getAsFloat() : 0.0F,
                Map.copyOf(faces));
    }

    private static Point point(JsonObject object, String key) {
        if (!object.has(key)) {
            return ZERO;
        }
        JsonArray values = object.getAsJsonArray(key);
        return new Point(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    private record Point(float x, float y, float z) {}

    private record Face(float u1, float v1, float u2, float v2, int texture, int rotation) {}

    private record Cube(Point from, Point to, Point origin, Point rotation, float inflate,
                        Map<String, Face> faces) {}

    private record Bone(String name, Point origin, Point rotation, List<Cube> cubes,
                        List<Bone> children) {}
}
