```yaml
// camel-k: trait=knative-service.enabled=true
```

```java
        from("timer:tick?period=10000")
            .setBody(exchange -> createOrder())
            .to("jpa:" + Order.class.getName())
            .marshal(FORMAT)
            .log("New Order: ${body}")
            .toD("kafka:order-events?brokers=${env.KAFKA_BOOTSTRAP_SERVERS}");
```

```java
                .log("Reservation received: ${body}")
                .setProperty("order", body())
                .toD("jpa:" + Order.class.getName() + "?query=select o from Order o where o.id = ${body.id}")
                .log("Found order: ${body[0]}")
```

```java
                        .marshal(FORMAT)
                        .toD("kafka:order-events?brokers=${env.KAFKA_BOOTSTRAP_SERVERS}")
                        .unmarshal(FORMAT)
```

```shell
$ kamel run --name order-saga src/main/java/pl/piomin/samples/quarkus/serverless/order/OrderRoute.java --build-property file:src/main/resources/quarkus.properties --dev
```

```shell
curl https://order-saga-camel.apps.cluster-7h2ls.7h2ls.sandbox1025.opentlc.com/orders/confirm -H "Content-Type:application/json" -d "{\"id\":1,\"status\":\"IN_PROGRESS\"}" -v
```