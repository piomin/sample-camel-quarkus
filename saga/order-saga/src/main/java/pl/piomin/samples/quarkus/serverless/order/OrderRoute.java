package pl.piomin.samples.quarkus.serverless.order;

import com.github.piomin.entity.model.order.Order;
import com.github.piomin.entity.model.order.OrderStatus;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.rest.RestBindingMode;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Random;

// camel-k: trait=knative-service.enabled=true
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-jpa
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-jackson
// camel-k: dependency=mvn:io.quarkus:quarkus-jdbc-postgresql
// camel-k: dependency=mvn:org.projectlombok:lombok:1.18.22
// camel-k: dependency=github:piomin/entity-model/1.3

@ApplicationScoped
public class OrderRoute extends RouteBuilder {

    private static long NUM = 0;
    private Random RAND = new Random();
    private final JacksonDataFormat FORMAT = new JacksonDataFormat(Order.class);

    @Override
    public void configure() throws Exception {
        restConfiguration().bindingMode(RestBindingMode.json);

        from("timer:tick?period=10000")
                .setBody(exchange -> createOrder())
                .to("jpa:" + Order.class.getName())
                .marshal(FORMAT)
                .log("New order: ${body}")
                .to("kafka:order-events?brokers=my-cluster-kafka-bootstrap.kafka:9092");


        rest("/orders")
            .post("/confirm").type(Order.class)
            .route()
                .filter(exchange -> exchange.getIn().getBody() != null && ((Order) exchange.getIn().getBody()).getId() != null)
                .log("Reservation: ${body}")
                .setProperty("order", body())
                .toD("jpa:" + Order.class.getName() + "?query=select o from Order o where o.id = ${body.id}")
                .log("Found order: ${body[0]}")
                .choice()
                    .when().simple("${body[0].status.toString()} == 'NEW'")
                        .setBody(exchange -> {
                            Order order = (Order) exchange.getIn().getBody(List.class).get(0);
                            order.setStatus(exchange.getProperty("order", Order.class).getStatus());
                            return order;
                        })
                        .endChoice()
                    .otherwise()
                        .setBody(this::updateOrderStatus)
                        .log("Order confirmed: ${body}")
                        .marshal(FORMAT)
                        .to("kafka:order-events?brokers=my-cluster-kafka-bootstrap.kafka:9092")
                        .unmarshal(FORMAT)
                .end()
                .to("jpa:" + Order.class.getName() + "?useExecuteUpdate=true")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
                .setBody(constant(null))
            .end()
        .endRest();
    }

    private Order updateOrderStatus(Exchange exchange) {
        OrderStatus status = exchange.getProperty("order", Order.class).getStatus();
        Order order = (Order) exchange.getIn().getBody(List.class).get(0);
        if (order.getStatus() == OrderStatus.IN_PROGRESS) {
            order.setStatus(status);
        }
        return order;
    }

    private Order createOrder() {
        return new Order(null,
                RAND.nextInt(10) + 1,
                RAND.nextInt(10) + 1,
                100,
                1,
                OrderStatus.NEW, null, null);
    }
}