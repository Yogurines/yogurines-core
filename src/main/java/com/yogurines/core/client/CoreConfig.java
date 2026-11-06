package com.yogurines.core.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Configuración del menú. Se guarda en config/yogurines_core.json y, al arrancar,
 * se actualiza con server.json del repositorio de GitHub para que la IP llegue a todos.
 */
public final class CoreConfig {
    private static final Logger LOG = LoggerFactory.getLogger("yogurines_core");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DEFAULT_PASSWORD = "overclock";
    public static final String REPO = "Yogurines/yogurines-core";

    public String ip = "";
    public String playText = "Jugar";
    public String quitText = "Salir";
    public boolean showPlus = true;
    public String passHash = sha256(DEFAULT_PASSWORD);
    public String onlineUrl = "https://raw.githubusercontent.com/" + REPO + "/main/server.json";

    private static CoreConfig instance = new CoreConfig();

    public static CoreConfig get() {
        return instance;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("yogurines_core.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                CoreConfig c = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), CoreConfig.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            LOG.warn("No se pudo leer yogurines_core.json", e);
        }
        if (instance.passHash == null || instance.passHash.isEmpty()) instance.passHash = sha256(DEFAULT_PASSWORD);
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warn("No se pudo guardar yogurines_core.json", e);
        }
    }

    /** Descarga server.json del GitHub en segundo plano; si falla, se queda lo último guardado. */
    public static void fetchOnline() {
        String url = instance.onlineUrl;
        if (url == null || url.isBlank()) return;
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                    .header("Cache-Control", "no-cache").GET().build();
            client.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenAccept(res -> {
                if (res.statusCode() != 200) {
                    LOG.info("server.json no disponible ({}), uso la configuración local", res.statusCode());
                    return;
                }
                try {
                    applyServerJson(JsonParser.parseString(res.body()).getAsJsonObject());
                    save();
                    LOG.info("Configuración del menú actualizada desde GitHub");
                } catch (Exception e) {
                    LOG.warn("server.json no es válido", e);
                }
            }).exceptionally(t -> {
                LOG.info("Sin conexión con GitHub, uso la configuración local");
                return null;
            });
        } catch (Exception e) {
            LOG.warn("No se pudo pedir server.json", e);
        }
    }

    private static void applyServerJson(JsonObject o) {
        CoreConfig c = instance;
        if (o.has("ip")) c.ip = o.get("ip").getAsString().trim();
        if (o.has("playText")) c.playText = o.get("playText").getAsString();
        if (o.has("quitText")) c.quitText = o.get("quitText").getAsString();
        if (o.has("showPlus")) c.showPlus = o.get("showPlus").getAsBoolean();
    }

    /** El contenido que hay que poner en server.json para que les llegue a todos. */
    public String serverJson() {
        JsonObject o = new JsonObject();
        o.addProperty("ip", ip);
        o.addProperty("playText", playText);
        o.addProperty("quitText", quitText);
        o.addProperty("showPlus", showPlus);
        return GSON.toJson(o) + "\n";
    }

    public static String editUrl() {
        return "https://github.com/" + REPO + "/edit/main/server.json";
    }

    public boolean checkPassword(String pw) {
        return sha256(pw).equals(passHash);
    }

    public static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(("yogurines:" + s).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
