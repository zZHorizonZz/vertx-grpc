package io.vertx.jrpc.mcp.sample;

import com.google.protobuf.Descriptors;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.Service;
import io.vertx.jrpc.mcp.sample.weather.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class WeatherServiceImpl implements Service {

  public static final ServiceName WEATHER_SERVICE_NAME = ServiceName.create(WeatherServiceGrpc.SERVICE_NAME);

  // Message encoders and decoders
  public static GrpcMessageDecoder<AlertsRequest> ALERTS_REQUEST_DECODER = GrpcMessageDecoder.decoder(AlertsRequest.newBuilder());
  public static GrpcMessageEncoder<AlertsResponse> ALERTS_RESPONSE_ENCODER = GrpcMessageEncoder.encoder();
  public static GrpcMessageDecoder<ForecastRequest> FORECAST_REQUEST_DECODER = GrpcMessageDecoder.decoder(ForecastRequest.newBuilder());
  public static GrpcMessageEncoder<ForecastResponse> FORECAST_RESPONSE_ENCODER = GrpcMessageEncoder.encoder();

  private static final HttpClient HTTP = HttpClient.newHttpClient();

  @Override
  public ServiceName name() {
    return WEATHER_SERVICE_NAME;
  }

  @Override
  public Descriptors.ServiceDescriptor descriptor() {
    return WeatherServiceOuterClass.getDescriptor().findServiceByName("WeatherService");
  }

  @Override
  public void bind(GrpcServer server) {
    server.callHandler(new GetAlertsMethod(), request -> request.handler(requestBody -> {
      String location = requestBody.getLocation();
      geocode(location).thenCompose(coords -> {
        if (coords == null)
          return CompletableFuture.completedFuture(new ArrayList<>());
        return fetchAlerts(coords[0], coords[1], location);
      }).whenComplete((alerts, err) -> {
        List<WeatherAlert> safeAlerts = alerts != null ? alerts : new ArrayList<>();
        AlertsResponse response = AlertsResponse.newBuilder()
          .addAllAlerts(safeAlerts)
          .setLocation(location)
          .setTimestamp(Instant.now().getEpochSecond())
          .build();
        request.response().end(response);
      });
    }));

    server.callHandler(new GetForecastMethod(), request -> {
      request.handler(requestBody -> {
        String location = requestBody.getLocation();
        int days = requestBody.getDays() > 0 ? requestBody.getDays() : 7;
        geocode(location).thenCompose(coords -> {
          if (coords == null)
            return CompletableFuture.completedFuture(new ArrayList<WeatherCondition>());
          return fetchForecast(coords[0], coords[1], days);
        }).whenComplete((forecast, err) -> {
          List<WeatherCondition> safeForecast = forecast != null ? forecast : new ArrayList<>();
          ForecastResponse response = ForecastResponse.newBuilder()
            .addAllForecast(safeForecast)
            .setLocation(location)
            .setTimestamp(Instant.now().getEpochSecond())
            .build();
          request.response().end(response);
        });
      });
    });
  }

  private static class GetAlertsMethod implements ServiceMethod<AlertsRequest, AlertsResponse> {

    @Override
    public ServiceName serviceName() {
      return WEATHER_SERVICE_NAME;
    }

    @Override
    public String methodName() {
      return "GetAlerts";
    }

    @Override
    public GrpcMessageEncoder<AlertsResponse> encoder() {
      return ALERTS_RESPONSE_ENCODER;
    }

    @Override
    public GrpcMessageDecoder<AlertsRequest> decoder() {
      return ALERTS_REQUEST_DECODER;
    }
  }

  private static class GetForecastMethod implements ServiceMethod<ForecastRequest, ForecastResponse> {

    @Override
    public ServiceName serviceName() {
      return WEATHER_SERVICE_NAME;
    }

    @Override
    public String methodName() {
      return "GetForecast";
    }

    @Override
    public GrpcMessageEncoder<ForecastResponse> encoder() {
      return FORECAST_RESPONSE_ENCODER;
    }

    @Override
    public GrpcMessageDecoder<ForecastRequest> decoder() {
      return FORECAST_REQUEST_DECODER;
    }
  }

  // ---- Open-Meteo integration ----
  private CompletableFuture<double[]> geocode(String location) {
    try {
      String encoded = URLEncoder.encode(location, StandardCharsets.UTF_8);
      String url = "https://geocoding-api.open-meteo.com/v1/search?count=1&name=" + encoded;
      HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
      return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
        .thenApply(HttpResponse::body)
        .thenApply(body -> {
          JsonObject json = new JsonObject(body);
          JsonArray results = json.getJsonArray("results");
          if (results == null || results.isEmpty())
            return null;
          JsonObject first = results.getJsonObject(0);
          double lat = first.getDouble("latitude");
          double lon = first.getDouble("longitude");
          return new double[] { lat, lon };
        })
        .exceptionally(err -> null);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(null);
    }
  }

  private CompletableFuture<List<WeatherCondition>> fetchForecast(double lat, double lon, int days) {
    // Use daily metrics; timezone auto ensures local dates
    String daily = String.join(",",
      "temperature_2m_max",
      "temperature_2m_min",
      "precipitation_probability_max",
      "wind_speed_10m_max",
      "wind_direction_10m_dominant",
      "relative_humidity_2m_mean"
    );
    String url = String.format(Locale.ROOT,
      "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&daily=%s&timezone=auto&forecast_days=%d",
      lat, lon, daily, Math.max(1, Math.min(16, days))
    );
    HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
    return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
      .thenApply(HttpResponse::body)
      .thenApply(this::parseForecast)
      .exceptionally(err -> new ArrayList<>());
  }

  private List<WeatherCondition> parseForecast(String body) {
    List<WeatherCondition> list = new ArrayList<>();
    JsonObject json = new JsonObject(body);
    JsonObject daily = json.getJsonObject("daily");
    if (daily == null)
      return list;
    JsonArray dates = daily.getJsonArray("time");
    JsonArray tMax = daily.getJsonArray("temperature_2m_max");
    JsonArray tMin = daily.getJsonArray("temperature_2m_min");
    JsonArray precipProb = daily.getJsonArray("precipitation_probability_max");
    JsonArray windSpeed = daily.getJsonArray("wind_speed_10m_max");
    JsonArray windDir = daily.getJsonArray("wind_direction_10m_dominant");
    JsonArray humidity = daily.getJsonArray("relative_humidity_2m_mean");

    int count = dates != null ? dates.size() : 0;
    for (int i = 0; i < count; i++) {
      String date = dates.getString(i);
      double hi = tMax != null && i < tMax.size() && tMax.getValue(i) != null ? toDouble(tMax.getValue(i)) : 0d;
      double lo = tMin != null && i < tMin.size() && tMin.getValue(i) != null ? toDouble(tMin.getValue(i)) : 0d;
      double precip = precipProb != null && i < precipProb.size() && precipProb.getValue(i) != null ? toDouble(precipProb.getValue(i)) : 0d;
      double ws = windSpeed != null && i < windSpeed.size() && windSpeed.getValue(i) != null ? toDouble(windSpeed.getValue(i)) : 0d;
      String wd = windDir != null && i < windDir.size() && windDir.getValue(i) != null ? degToCompass(toDouble(windDir.getValue(i))) : "";
      double hum = humidity != null && i < humidity.size() && humidity.getValue(i) != null ? toDouble(humidity.getValue(i)) : 0d;

      WeatherCondition condition = WeatherCondition.newBuilder()
        .setDate(date)
        .setTemperatureHigh(hi)
        .setTemperatureLow(lo)
        .setCondition(precip > 50 ? "rainy" : (hi > 25 ? "sunny" : "cloudy"))
        .setHumidity(hum)
        .setWindSpeed(ws)
        .setWindDirection(wd)
        .setPrecipitationChance(precip)
        .build();
      list.add(condition);
    }
    return list;
  }

  private CompletableFuture<List<WeatherAlert>> fetchAlerts(double lat, double lon, String areaName) {
    String url = String.format(Locale.ROOT,
      "https://api.open-meteo.com/v1/warnings?latitude=%f&longitude=%f&timezone=auto",
      lat, lon);
    HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
    return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
      .thenApply(HttpResponse::body)
      .thenApply(body -> parseAlerts(body, areaName))
      .exceptionally(err -> new ArrayList<>());
  }

  private List<WeatherAlert> parseAlerts(String body, String areaName) {
    List<WeatherAlert> list = new ArrayList<>();
    JsonObject json = new JsonObject(body);
    JsonArray warnings = json.getJsonArray("warnings");
    if (warnings == null)
      return list;
    long now = Instant.now().getEpochSecond();
    for (int i = 0; i < warnings.size(); i++) {
      JsonObject w = warnings.getJsonObject(i);
      String id = w.getString("id", "");
      String event = w.getString("event", "Weather Warning");
      String severity = w.getString("severity", "");
      String description = w.getString("headline", w.getString("description", event));
      long start = toEpoch(w.getString("effective"));
      long end = toEpoch(w.getString("expires"));
      if (start == 0)
        start = now;
      if (end == 0)
        end = now + 6 * 3600;
      WeatherAlert alert = WeatherAlert.newBuilder()
        .setId(id.isEmpty() ? ("ALERT_" + i) : id)
        .setTitle(event)
        .setDescription(description)
        .setSeverity(severity.isEmpty() ? "moderate" : severity.toLowerCase(Locale.ROOT))
        .setArea(areaName)
        .setStartTime(start)
        .setEndTime(end)
        .build();
      list.add(alert);
    }
    return list;
  }

  private static String degToCompass(double deg) {
    String[] dirs = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW" };
    int idx = (int) Math.round(((deg % 360) / 22.5));
    return dirs[idx % 16];
  }

  private static double toDouble(Object o) {
    if (o instanceof Number)
      return ((Number) o).doubleValue();
    try {
      return Double.parseDouble(String.valueOf(o));
    } catch (Exception e) {
      return 0d;
    }
  }

  private static long toEpoch(String iso) {
    try {
      return Instant.parse(iso).getEpochSecond();
    } catch (Exception e) {
      return 0L;
    }
  }
}
