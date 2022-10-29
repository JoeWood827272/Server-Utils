package net.kyrptonaught.serverutils.advancementSync;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyrptonaught.serverutils.ModuleWConfig;
import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AdvancementSyncMod extends ModuleWConfig<AdvancementSyncConfig> {
    public static String MOD_ID = "advancementsync";
    public static HttpClient client;

    @Override
    public void onInitialize() {
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (getConfig().syncOnJoin) {
                try {
                    HttpRequest request = buildGetRequest(getUrl("getAdvancements", handler.player));

                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenAccept(stringHttpResponse -> {
                                if (!didRequestFail(stringHttpResponse)) {
                                    String json = stringHttpResponse.body();
                                    server.execute(() -> {
                                        ((PATLoadFromString) handler.player.getAdvancementTracker()).loadFromString(server.getAdvancementLoader(), json);
                                    });
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public AdvancementSyncConfig createDefaultConfig() {
        return new AdvancementSyncConfig();
    }

    public static void syncGrantedAdvancement(ServerPlayerEntity serverPlayerEntity, String json) {
        try {
            HttpRequest request = buildPostRequest(getUrl("addAdvancements", serverPlayerEntity), json);
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void syncRevokedAdvancement(ServerPlayerEntity serverPlayerEntity, String json) {
        try {
            HttpRequest request = buildPostRequest(getUrl("removeAdvancements", serverPlayerEntity), json);
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getUrl(String route, ServerPlayerEntity player) {
        return ServerUtilsMod.AdvancementSyncModule.getConfig().getApiURL() + "/" + route + "/" + player.getUuidAsString();
    }

    public static HttpRequest buildPostRequest(String url, String json) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    public static HttpRequest buildGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .GET()
                .build();
    }

    public boolean didRequestFail(HttpResponse<String> response) {
        return response == null || response.statusCode() != 200 || response.body().equalsIgnoreCase("failed");
    }
}
