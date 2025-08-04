package io.vertx.jrpc.mcp.sample;

import com.google.protobuf.Descriptors;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.Service;
import io.vertx.jrpc.mcp.sample.weather.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeatherServiceImpl implements Service {

  public static final ServiceName WEATHER_SERVICE_NAME = ServiceName.create(WeatherServiceGrpc.SERVICE_NAME);

  // Message encoders and decoders
  public static GrpcMessageDecoder<AlertsRequest> ALERTS_REQUEST_DECODER = GrpcMessageDecoder.decoder(AlertsRequest.newBuilder());
  public static GrpcMessageEncoder<AlertsResponse> ALERTS_RESPONSE_ENCODER = GrpcMessageEncoder.encoder();
  public static GrpcMessageDecoder<ForecastRequest> FORECAST_REQUEST_DECODER = GrpcMessageDecoder.decoder(ForecastRequest.newBuilder());
  public static GrpcMessageEncoder<ForecastResponse> FORECAST_RESPONSE_ENCODER = GrpcMessageEncoder.encoder();

  private final Random random = new Random();

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
    server.callHandler(new GetAlertsMethod(), request -> {
      request.handler(requestBody -> {
        List<WeatherAlert> alerts = generateMockAlerts(requestBody.getLocation(), requestBody.getSeverityLevel());
        AlertsResponse response = AlertsResponse.newBuilder()
          .addAllAlerts(alerts)
          .setLocation(requestBody.getLocation())
          .setTimestamp(Instant.now().getEpochSecond())
          .build();
        request.response().end(response);
      });
    });

    server.callHandler(new GetForecastMethod(), request -> {
      request.handler(requestBody -> {
        int days = requestBody.getDays() > 0 ? requestBody.getDays() : 7;
        List<WeatherCondition> forecast = generateMockForecast(requestBody.getLocation(), days);
        ForecastResponse response = ForecastResponse.newBuilder()
          .addAllForecast(forecast)
          .setLocation(requestBody.getLocation())
          .setTimestamp(Instant.now().getEpochSecond())
          .build();
        request.response().end(response);
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

  private List<WeatherAlert> generateMockAlerts(String location, String severityLevel) {
    List<WeatherAlert> alerts = new ArrayList<>();

    // Generate 0-3 random alerts
    int alertCount = random.nextInt(4);

    String[] alertTypes = {"Thunderstorm Warning", "Heavy Rain Alert", "Wind Advisory", "Temperature Warning"};
    String[] severities = {"minor", "moderate", "severe", "extreme"};

    for (int i = 0; i < alertCount; i++) {
      String alertType = alertTypes[random.nextInt(alertTypes.length)];
      String severity = severityLevel.isEmpty() ? severities[random.nextInt(severities.length)] : severityLevel;

      long startTime = Instant.now().getEpochSecond();
      long endTime = startTime + (3600 * (6 + random.nextInt(18))); // 6-24 hours duration

      WeatherAlert alert = WeatherAlert.newBuilder()
        .setId("ALERT_" + System.currentTimeMillis() + "_" + i)
        .setTitle(alertType + " for " + location)
        .setDescription("A " + severity + " " + alertType.toLowerCase() + " is expected in " + location + " area.")
        .setSeverity(severity)
        .setArea(location)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .build();

      alerts.add(alert);
    }

    return alerts;
  }

  private List<WeatherCondition> generateMockForecast(String location, int days) {
    List<WeatherCondition> forecast = new ArrayList<>();

    String[] conditions = {"sunny", "partly cloudy", "cloudy", "rainy", "snowy", "stormy"};
    String[] windDirections = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    LocalDate currentDate = LocalDate.now();

    for (int i = 0; i < days; i++) {
      LocalDate forecastDate = currentDate.plusDays(i);

      // Generate realistic weather data
      double baseTemp = 15 + random.nextGaussian() * 10; // Base around 15°C with variation
      double tempHigh = baseTemp + random.nextDouble() * 8;
      double tempLow = baseTemp - random.nextDouble() * 8;

      WeatherCondition condition = WeatherCondition.newBuilder()
        .setDate(forecastDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .setTemperatureHigh(Math.round(tempHigh * 10.0) / 10.0)
        .setTemperatureLow(Math.round(tempLow * 10.0) / 10.0)
        .setCondition(conditions[random.nextInt(conditions.length)])
        .setHumidity(30 + random.nextDouble() * 60) // 30-90%
        .setWindSpeed(5 + random.nextDouble() * 25) // 5-30 km/h
        .setWindDirection(windDirections[random.nextInt(windDirections.length)])
        .setPrecipitationChance(random.nextDouble() * 100) // 0-100%
        .build();

      forecast.add(condition);
    }

    return forecast;
  }
}
