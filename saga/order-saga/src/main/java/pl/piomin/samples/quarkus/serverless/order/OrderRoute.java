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
// camel-k: dependency=github:piomin/entity-model/1.1

@ApplicationScoped
public class OrderRoute extends RouteBuilder {

    private static long num = 0;
    private Random r = new Random();
    private final JacksonDataFormat FORMAT = new JacksonDataFormat(Order.class);

    @Override
    public void configure() throws Exception {
        restConfiguration().bindingMode(RestBindingMode.json);

        from("timer:tick?period=10000")
            .setBody(exchange -> createOrder())
            .to("jpa:" + Order.class.getName())
            .marshal(FORMAT)
            .log("New Order: ${body}")
            .toD("kafka:order-events?brokers=${env.KAFKA_BOOTSTRAP_SERVERS}");


        rest("/orders")
            .post("/confirm").consumes("application/json").type(Order.class)
            .route()
                .toD("jpa:" + Order.class.getName() + "?query=select o from Order o where o.id = ${body.id}")
                .log("Found order: ${body[0]}")
                .choice()
                    .when().simple("${body[0].status.toString()} == 'NEW'")
                        .setBody(exchange -> updateOrderStatus(exchange, OrderStatus.IN_PROGRESS))
                        .endChoice()
                    .otherwise()
                        .setBody(exchange -> updateOrderStatus(exchange, OrderStatus.CONFIRMED))
                        .marshal(FORMAT)
                        .log("Order confirmed: ${body}")
                        .toD("kafka:order-events?brokers=${env.KAFKA_BOOTSTRAP_SERVERS}")
                        .unmarshal(FORMAT)
                .end()
                .to("jpa:" + Order.class.getName() + "?useExecuteUpdate=true")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
                .setBody(constant(null))
            .end()
        .endRest();
    }

    private Order updateOrderStatus(Exchange exchange, OrderStatus status) {
        Order order = (Order) exchange.getIn().getBody(List.class).get(0);
        order.setStatus(status);
        return order;
    }

    private Order createOrder() {
        return new Order(null,
                r.nextInt(10) + 1,
                r.nextInt(10) + 1,
                100,
                1,
                OrderStatus.NEW);
    }
}