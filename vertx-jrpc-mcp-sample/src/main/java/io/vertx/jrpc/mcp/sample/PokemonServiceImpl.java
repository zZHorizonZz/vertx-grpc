package io.vertx.jrpc.mcp.sample;

import com.google.protobuf.Descriptors;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.Service;
import io.vertx.jrpc.mcp.sample.pokemon.*;
import io.vertx.mcp.proto.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Pokemon Service implementation that fetches data from PokeAPI.
 * Provides Pokemon information with images converted to base64.
 */
public class PokemonServiceImpl implements Service {

    private static final String POKEAPI_BASE_URL = "https://pokeapi.co/api/v2";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static final ServiceName POKEMON_SERVICE_NAME = ServiceName.create(PokemonServiceGrpc.SERVICE_NAME);

    // Message encoders and decoders
    public static GrpcMessageDecoder<PokemonRequest> POKEMON_REQUEST_DECODER = GrpcMessageDecoder.decoder(PokemonRequest.newBuilder());
    public static GrpcMessageDecoder<SpeciesRequest> SPECIES_REQUEST_DECODER = GrpcMessageDecoder.decoder(SpeciesRequest.newBuilder());
    public static GrpcMessageDecoder<TypeRequest> TYPE_REQUEST_DECODER = GrpcMessageDecoder.decoder(TypeRequest.newBuilder());
    public static GrpcMessageEncoder<TextContent> TEXT_CONTENT_ENCODER = GrpcMessageEncoder.encoder();
    public static GrpcMessageEncoder<Content> CONTENT_ENCODER = GrpcMessageEncoder.encoder();

    @Override
    public ServiceName name() {
        return POKEMON_SERVICE_NAME;
    }

    @Override
    public Descriptors.ServiceDescriptor descriptor() {
        return PokemonServiceOuterClass.getDescriptor().findServiceByName("PokemonService");
    }

    @Override
    public void bind(GrpcServer server) {
        // GetPokemon method - returns TextContent
        server.callHandler(new GetPokemonMethod(), request -> request.handler(requestBody -> {
            fetchPokemon(requestBody).whenComplete((textContent, err) -> {
                TextContent response = textContent != null ? textContent : TextContent.newBuilder().setText("Error fetching Pokemon").build();
                request.response().end(response);
            });
        }));

        // GetRichPokemon method - returns Content with multiple types
        server.callHandler(new GetRichPokemonMethod(), request -> request.handler(requestBody -> {
            fetchRichPokemon(requestBody).whenComplete((content, err) -> {
                Content response = content != null ? content : Content.newBuilder().addContent(ContentItem.newBuilder().setText(TextContent.newBuilder().setText("Error fetching rich Pokemon").build()).build()).build();
                request.response().end(response);
            });
        }));

        // GetPokemonSpecies method - returns Content with species info
        server.callHandler(new GetPokemonSpeciesMethod(), request -> request.handler(requestBody -> {
            fetchPokemonSpecies(requestBody).whenComplete((content, err) -> {
                Content response = content != null ? content :
                    Content.newBuilder()
                        .addContent(ContentItem.newBuilder()
                            .setText(TextContent.newBuilder().setText("Error fetching Pokemon species").build())
                            .build())
                        .build();
                request.response().end(response);
            });
        }));

        // GetPokemonType method - returns Content with type info
        server.callHandler(new GetPokemonTypeMethod(), request -> request.handler(requestBody -> {
            fetchPokemonType(requestBody).whenComplete((content, err) -> {
                Content response = content != null ? content :
                    Content.newBuilder()
                        .addContent(ContentItem.newBuilder()
                            .setText(TextContent.newBuilder().setText("Error fetching Pokemon type").build())
                            .build())
                        .build();
                request.response().end(response);
            });
        }));
    }

    // Service method implementations
    private static class GetPokemonMethod implements ServiceMethod<PokemonRequest, TextContent> {
        @Override
        public ServiceName serviceName() { return POKEMON_SERVICE_NAME; }
        @Override
        public String methodName() { return "GetPokemon"; }
        @Override
        public GrpcMessageEncoder<TextContent> encoder() { return TEXT_CONTENT_ENCODER; }
        @Override
        public GrpcMessageDecoder<PokemonRequest> decoder() { return POKEMON_REQUEST_DECODER; }
    }

    private static class GetRichPokemonMethod implements ServiceMethod<PokemonRequest, Content> {
        @Override
        public ServiceName serviceName() { return POKEMON_SERVICE_NAME; }
        @Override
        public String methodName() { return "GetRichPokemon"; }
        @Override
        public GrpcMessageEncoder<Content> encoder() { return CONTENT_ENCODER; }
        @Override
        public GrpcMessageDecoder<PokemonRequest> decoder() { return POKEMON_REQUEST_DECODER; }
    }

    private static class GetPokemonSpeciesMethod implements ServiceMethod<SpeciesRequest, Content> {
        @Override
        public ServiceName serviceName() { return POKEMON_SERVICE_NAME; }
        @Override
        public String methodName() { return "GetPokemonSpecies"; }
        @Override
        public GrpcMessageEncoder<Content> encoder() { return CONTENT_ENCODER; }
        @Override
        public GrpcMessageDecoder<SpeciesRequest> decoder() { return SPECIES_REQUEST_DECODER; }
    }

    private static class GetPokemonTypeMethod implements ServiceMethod<TypeRequest, Content> {
        @Override
        public ServiceName serviceName() { return POKEMON_SERVICE_NAME; }
        @Override
        public String methodName() { return "GetPokemonType"; }
        @Override
        public GrpcMessageEncoder<Content> encoder() { return CONTENT_ENCODER; }
        @Override
        public GrpcMessageDecoder<TypeRequest> decoder() { return TYPE_REQUEST_DECODER; }
    }

    // Implementation methods
    private CompletableFuture<TextContent> fetchPokemon(PokemonRequest request) {
        String identifier = getPokemonIdentifier(request);
        String url = POKEAPI_BASE_URL + "/pokemon/" + identifier.toLowerCase();

        return fetchJsonFromUrl(url)
            .thenApply(json -> {
                String name = json.getString("name");
                int id = json.getInteger("id");
                int height = json.getInteger("height");
                int weight = json.getInteger("weight");

                String text = String.format("Pokemon: %s (ID: %d)\nHeight: %d decimeters\nWeight: %d hectograms",
                    name, id, height, weight);

                return TextContent.newBuilder()
                    .setText(text)
                    .build();
            })
            .exceptionally(throwable -> {
                return TextContent.newBuilder()
                    .setText("Error fetching Pokemon: " + throwable.getMessage())
                    .build();
            });
    }

    private CompletableFuture<Content> fetchRichPokemon(PokemonRequest request) {
        String identifier = getPokemonIdentifier(request);
        String url = POKEAPI_BASE_URL + "/pokemon/" + identifier.toLowerCase();

        return fetchJsonFromUrl(url)
            .thenCompose(json -> {
                String name = json.getString("name");
                int id = json.getInteger("id");
                int height = json.getInteger("height");
                int weight = json.getInteger("weight");

                // Get sprite URL
                JsonObject sprites = json.getJsonObject("sprites");
                String spriteUrl = sprites.getString("front_default");

                // Build stats text
                StringBuilder statsText = new StringBuilder();
                statsText.append(String.format("Pokemon: %s (ID: %d)\n", name, id));
                statsText.append(String.format("Height: %d decimeters\n", height));
                statsText.append(String.format("Weight: %d hectograms\n\n", weight));

                // Add stats
                statsText.append("Base Stats:\n");
                json.getJsonArray("stats").forEach(stat -> {
                    JsonObject statObj = (JsonObject) stat;
                    String statName = statObj.getJsonObject("stat").getString("name");
                    int baseStat = statObj.getInteger("base_stat");
                    statsText.append(String.format("- %s: %d\n", statName, baseStat));
                });

                // Add types
                statsText.append("\nTypes:\n");
                json.getJsonArray("types").forEach(type -> {
                    JsonObject typeObj = (JsonObject) type;
                    String typeName = typeObj.getJsonObject("type").getString("name");
                    statsText.append(String.format("- %s\n", typeName));
                });

                Content.Builder contentBuilder = Content.newBuilder();

                // Add text content
                contentBuilder.addContent(ContentItem.newBuilder()
                    .setText(TextContent.newBuilder().setText(statsText.toString()).build())
                    .build());

                // Fetch and add image if available
                if (spriteUrl != null && !spriteUrl.isEmpty()) {
                    return fetchImageAsBase64(spriteUrl)
                        .thenApply(base64Image -> {
                            if (base64Image != null) {
                                contentBuilder.addContent(ContentItem.newBuilder()
                                    .setImage(ImageContent.newBuilder()
                                        .setData(base64Image)
                                        .setMimeType("image/png")
                                        .build())
                                    .build());
                            }
                            return contentBuilder.build();
                        });
                } else {
                    return CompletableFuture.completedFuture(contentBuilder.build());
                }
            })
            .exceptionally(throwable -> {
                return Content.newBuilder()
                    .addContent(ContentItem.newBuilder()
                        .setText(TextContent.newBuilder()
                            .setText("Error fetching Pokemon: " + throwable.getMessage())
                            .build())
                        .build())
                    .build();
            });
    }

    private CompletableFuture<Content> fetchPokemonSpecies(SpeciesRequest request) {
        String identifier = getSpeciesIdentifier(request);
        String url = POKEAPI_BASE_URL + "/pokemon-species/" + identifier.toLowerCase();

        return fetchJsonFromUrl(url)
            .thenApply(json -> {
                String name = json.getString("name");
                int id = json.getInteger("id");

                StringBuilder speciesText = new StringBuilder();
                speciesText.append(String.format("Pokemon Species: %s (ID: %d)\n\n", name, id));

                // Add flavor text
                JsonObject flavorTextEntry = json.getJsonArray("flavor_text_entries")
                    .stream()
                    .map(JsonObject.class::cast)
                    .filter(entry -> "en".equals(entry.getJsonObject("language").getString("name")))
                    .findFirst()
                    .orElse(null);

                if (flavorTextEntry != null) {
                    speciesText.append("Description:\n");
                    speciesText.append(flavorTextEntry.getString("flavor_text").replace("\n", " ")).append("\n\n");
                }

                // Add generation
                JsonObject generation = json.getJsonObject("generation");
                if (generation != null) {
                    speciesText.append("Generation: ").append(generation.getString("name")).append("\n");
                }

                // Add habitat if available
                JsonObject habitat = json.getJsonObject("habitat");
                if (habitat != null) {
                    speciesText.append("Habitat: ").append(habitat.getString("name")).append("\n");
                }

                return Content.newBuilder()
                    .addContent(ContentItem.newBuilder()
                        .setText(TextContent.newBuilder().setText(speciesText.toString()).build())
                        .build())
                    .build();
            })
            .exceptionally(throwable -> {
                return Content.newBuilder()
                    .addContent(ContentItem.newBuilder()
                        .setText(TextContent.newBuilder()
                            .setText("Error fetching Pokemon species: " + throwable.getMessage())
                            .build())
                        .build())
                    .build();
            });
    }

    private CompletableFuture<Content> fetchPokemonType(TypeRequest request) {
        String identifier = getTypeIdentifier(request);
        String url = POKEAPI_BASE_URL + "/type/" + identifier.toLowerCase();

        return fetchJsonFromUrl(url)
            .thenApply(json -> {
                String name = json.getString("name");
                int id = json.getInteger("id");

                StringBuilder typeText = new StringBuilder();
                typeText.append(String.format("Pokemon Type: %s (ID: %d)\n\n", name, id));

                // Add damage relations
                JsonObject damageRelations = json.getJsonObject("damage_relations");

                typeText.append("Damage Relations:\n");

                // Double damage to
                typeText.append("Super effective against:\n");
                damageRelations.getJsonArray("double_damage_to").forEach(type -> {
                    JsonObject typeObj = (JsonObject) type;
                    typeText.append("- ").append(typeObj.getString("name")).append("\n");
                });

                // Half damage to
                typeText.append("\nNot very effective against:\n");
                damageRelations.getJsonArray("half_damage_to").forEach(type -> {
                    JsonObject typeObj = (JsonObject) type;
                    typeText.append("- ").append(typeObj.getString("name")).append("\n");
                });

                // No damage to
                typeText.append("\nNo effect against:\n");
                damageRelations.getJsonArray("no_damage_to").forEach(type -> {
                    JsonObject typeObj = (JsonObject) type;
                    typeText.append("- ").append(typeObj.getString("name")).append("\n");
                });

                return Content.newBuilder()
                    .addContent(ContentItem.newBuilder()
                        .setText(TextContent.newBuilder().setText(typeText.toString()).build())
                        .build())
                    .build();
            })
            .exceptionally(throwable -> {
                return Content.newBuilder()
                    .addContent(ContentItem.newBuilder()
                        .setText(TextContent.newBuilder()
                            .setText("Error fetching Pokemon type: " + throwable.getMessage())
                            .build())
                        .build())
                    .build();
            });
    }

    // Helper methods
    private String getPokemonIdentifier(PokemonRequest request) {
        if (request.hasPokemonId()) {
            return String.valueOf(request.getPokemonId());
        } else if (request.hasPokemonName()) {
            return request.getPokemonName();
        } else {
            throw new IllegalArgumentException("Pokemon identifier is required");
        }
    }

    private String getSpeciesIdentifier(SpeciesRequest request) {
        if (request.hasSpeciesId()) {
            return String.valueOf(request.getSpeciesId());
        } else if (request.hasSpeciesName()) {
            return request.getSpeciesName();
        } else {
            throw new IllegalArgumentException("Species identifier is required");
        }
    }

    private String getTypeIdentifier(TypeRequest request) {
        if (request.hasTypeId()) {
            return String.valueOf(request.getTypeId());
        } else if (request.hasTypeName()) {
            return request.getTypeName();
        } else {
            throw new IllegalArgumentException("Type identifier is required");
        }
    }

    private CompletableFuture<JsonObject> fetchJsonFromUrl(String url) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenApply(body -> {
                try {
                    return new JsonObject(body);
                } catch (Exception e) {
                    throw new RuntimeException("Error parsing JSON response: " + e.getMessage());
                }
            })
            .exceptionally(err -> {
                throw new RuntimeException("Error fetching data from " + url + ": " + err.getMessage());
            });
    }

    private CompletableFuture<String> fetchImageAsBase64(String imageUrl) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(imageUrl))
            .GET()
            .build();

        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
            .thenApply(HttpResponse::body)
            .thenApply(bytes -> Base64.getEncoder().encodeToString(bytes))
            .exceptionally(throwable -> null);
    }
}
