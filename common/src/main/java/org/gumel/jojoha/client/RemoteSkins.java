package org.gumel.jojoha.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.supporter.SkinCatalog;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteSkins {
    private static final long RETRY_MS = 60L * 1000L;
    private static final int MAX_TEXTURE_SIDE = 1024;

    private record Loaded(int version, ResourceLocation texture, ResourceLocation model, BakedGeoModel baked,
                          ResourceLocation animations, BakedAnimations clips) {
    }

    private record Files(byte[] texture, byte[] model, byte[] animations) {
    }

    private static final Map<Integer, Loaded> READY = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> PENDING = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> FAILED = new ConcurrentHashMap<>();

    private RemoteSkins() {
    }

    private static Loaded loaded(int skin) {
        if (!SkinCatalog.remote(skin)) {
            return null;
        }
        SkinCatalog.refreshIfStale();
        SkinCatalog.Entry entry = SkinCatalog.get(skin);
        if (entry == null) {
            return null;
        }
        Loaded ready = READY.get(entry.id());
        if (ready != null && ready.version() == entry.version()) {
            restore(ready);
            return ready;
        }
        fetch(entry);
        return ready;
    }

    public static ResourceLocation texture(int skin) {
        Loaded ready = loaded(skin);
        return ready == null ? null : ready.texture();
    }

    public static ResourceLocation model(int skin) {
        Loaded ready = loaded(skin);
        return ready == null ? null : ready.model();
    }

    public static ResourceLocation animations(int skin) {
        Loaded ready = loaded(skin);
        return ready == null ? null : ready.animations();
    }

    private static void restore(Loaded ready) {
        try {
            if (ready.model() != null && !GeckoLibCache.getBakedModels().containsKey(ready.model())) {
                GeckoLibCache.getBakedModels().put(ready.model(), ready.baked());
            }
            if (ready.animations() != null && !GeckoLibCache.getBakedAnimations().containsKey(ready.animations())) {
                GeckoLibCache.getBakedAnimations().put(ready.animations(), ready.clips());
            }
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private static void fetch(SkinCatalog.Entry entry) {
        Long failed = FAILED.get(entry.id());
        if (failed != null && System.currentTimeMillis() - failed < RETRY_MS) {
            return;
        }
        Integer pending = PENDING.putIfAbsent(entry.id(), entry.version());
        if (pending != null) {
            return;
        }
        CompletableFuture<byte[]> texture = SkinCatalog.download(entry, SkinCatalog.TEXTURE);
        CompletableFuture<byte[]> model = SkinCatalog.download(entry, SkinCatalog.MODEL);
        CompletableFuture<byte[]> animations = SkinCatalog.download(entry, SkinCatalog.ANIMATIONS);
        CompletableFuture.allOf(texture, model, animations)
                .thenApply(ignored -> new Files(texture.join(), model.join(), animations.join()))
                .whenComplete((files, error) -> Minecraft.getInstance().execute(() -> {
                    PENDING.remove(entry.id());
                    if (error != null || files == null) {
                        FAILED.put(entry.id(), System.currentTimeMillis());
                        Jojoha.LOGGER.warn("Could not load supporter skin {}: {}", entry.id(),
                                error == null ? "no files" : error.getMessage());
                        return;
                    }
                    try {
                        install(entry, files);
                        FAILED.remove(entry.id());
                    } catch (IOException | RuntimeException failure) {
                        FAILED.put(entry.id(), System.currentTimeMillis());
                        Jojoha.LOGGER.warn("Could not load supporter skin {}", entry.id(), failure);
                    }
                }));
    }

    private static void install(SkinCatalog.Entry entry, Files files) throws IOException {
        String stem = "remote_skins/" + entry.id() + "_v" + entry.version();
        NativeImage image = NativeImage.read(new ByteArrayInputStream(files.texture()));
        if (image.getWidth() > MAX_TEXTURE_SIDE || image.getHeight() > MAX_TEXTURE_SIDE) {
            image.close();
            throw new IOException("texture is larger than " + MAX_TEXTURE_SIDE + " pixels");
        }
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/" + stem + ".png");

        ResourceLocation model = null;
        BakedGeoModel baked = null;
        if (files.model() != null) {
            Model raw = KeyFramesAdapter.GEO_GSON.fromJson(new String(files.model(), StandardCharsets.UTF_8), Model.class);
            baked = BakedModelFactory.getForNamespace(Jojoha.MOD_ID).constructGeoModel(GeometryTree.fromModel(raw));
            model = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "geo/" + stem + ".geo.json");
        }

        ResourceLocation animations = null;
        BakedAnimations clips = null;
        if (files.animations() != null) {
            JsonObject json = JsonParser.parseString(new String(files.animations(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            clips = KeyFramesAdapter.GEO_GSON.fromJson(GsonHelper.getAsJsonObject(json, "animations"),
                    BakedAnimations.class);
            animations = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "animations/" + stem + ".animation.json");
        }

        Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(image));
        Loaded ready = new Loaded(entry.version(), texture, model, baked, animations, clips);
        Loaded old = READY.put(entry.id(), ready);
        if (old != null && !old.texture().equals(texture)) {
            Minecraft.getInstance().getTextureManager().release(old.texture());
        }
        restore(ready);
    }
}
