# Meteo Public — Running Conditions Rating with Apache Camel on Quarkus

`meteo-public` talks to three public Open-Meteo APIs and tells you whether it's a good day for a run. Send a city name and country code to `POST /rating` and the app resolves the coordinates, grabs the European Air Quality Index and the current weather forecast, then hands back a letter grade from A+ to E with a short verdict.

## Architecture

The app runs on Quarkus 3.x with Java 21 and deploys to OpenShift. Every request goes through an Apache Camel route pipeline that fans out to three external APIs and scores the result through a CDI bean (`MeteoService`).

The orchestrator route `get-running-rating` calls three `direct:` sub-routes in sequence — `geocode`, `airQuality`, and `weather`. Each sub-route hits one API and passes the result forward as an exchange property:

```java
from("direct:getRating")
    .routeId("get-running-rating")
    .setProperty("request", simple("${body}"))
    .to("direct:geocode")
    .to("direct:airQuality")
    .to("direct:weather")
    .bean(meteoService, "buildResponse");
```

`direct:geocode` resolves the city to coordinates, `direct:airQuality` picks up those coordinates and fetches the AQI:

```java
from("direct:geocode")
    .to("https://geocoding-api.open-meteo.com/v1/search?bridgeEndpoint=true")
    .unmarshal(new JacksonDataFormat(GeocodingResponse.class))
    .setProperty("lat", simple("${body.results[0].latitude}"))
    .setProperty("lon", simple("${body.results[0].longitude}"));

from("direct:airQuality")
    .to("https://air-quality-api.open-meteo.com/v1/air-quality?bridgeEndpoint=true")
    .unmarshal(new JacksonDataFormat(AirQualityResponse.class))
    .setProperty("aqi", simple("${body.current.europeanAqi}"));
```

Once you enable `camel-quarkus-opentelemetry2`, every `direct:` route shows up as a named span in the trace. That gives you a clean five-span waterfall per request and immediately tells you which upstream API is eating your latency.

## Dependencies

### REST and Serialization

The REST layer uses `camel-quarkus-rest` with `camel-quarkus-platform-http` as the underlying component. `platform-http` plugs into the Vert.x server Quarkus already runs, so there's no second HTTP listener. For JSON, `camel-quarkus-jackson` handles marshalling inside Camel routes and `quarkus-rest-jackson` covers the Quarkus REST binding.

Outbound HTTP calls go through `camel-quarkus-http`. Route-to-route calls use `camel-quarkus-direct`, and the scoring bean plugs in via `camel-quarkus-bean`.

### API Documentation

`quarkus-smallrye-openapi` combined with `camel-quarkus-openapi-java` exposes the full API spec at `/q/openapi`. The Camel extension contributes the REST DSL routes to the document, so you get an accurate spec that reflects what Camel actually serves — not just what JAX-RS knows about. In the production profile, Swagger UI is on so you can test the endpoint directly from the OpenShift route.

### Observability

Tracing needs two extensions. `quarkus-opentelemetry` wires up the OTLP exporter and W3C Trace Context propagation and auto-instruments inbound HTTP. `camel-quarkus-opentelemetry2` takes that further into the Camel pipeline, turning each `direct:` route into its own named span. Without this second extension, Camel's internals stay invisible in the trace.

`quarkus-micrometer-registry-prometheus` drops a Prometheus scrape endpoint at `/q/metrics`. On OpenShift with user-workload monitoring on, point a `ServiceMonitor` at it and your metrics flow straight into Grafana — no push gateway needed.

### OpenShift Deployment

`quarkus-openshift` generates the `Deployment`, `Service`, and `Route` manifests at build time. No YAML to maintain. The OpenShift route goes live with a single property in the production config.

## Configuration

The base `application.properties` turns on HTTP access logging with response time in milliseconds — handy when you want to cross-reference a slow request in logs with its trace:

```properties
quarkus.http.access-log.enabled=true
quarkus.http.access-log.pattern=%h "%r" %s %b %D ms

%test.quarkus.otel.sdk.disabled=true
```

OTel is off in the test profile, so unit tests don't need a Collector running.

In production (`application-prod.properties`), the OTLP exporter points at the OpenTelemetry Collector running in the cluster's `otel` namespace, and the Camel OTel bridge switches on:

```properties
quarkus.otel.exporter.otlp.endpoint = http://cluster-collector-collector.otel.svc.cluster.local:4317
quarkus.otel.propagators = tracecontext,baggage
camel.opentelemetry2.enabled = true
quarkus.openshift.route.expose = true
```

`tracecontext,baggage` follows the W3C standard, which plays nicely with the OpenShift service mesh if you run Istio on the cluster.
