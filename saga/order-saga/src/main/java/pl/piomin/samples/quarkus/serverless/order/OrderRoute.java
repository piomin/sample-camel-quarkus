package pl.piomin.samples.quarkus.serverless.order;

import com.github.piomin.entity.model.order.Order;
import com.github.piomin.entity.model.order.OrderStatus;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

// camel-k: trait=knative-service.enabled=true
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-jpa
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-jackson
// camel-k: dependency=mvn:io.quarkus:quarkus-jdbc-h2
// camel-k: dependency=mvn:org.projectlombok:lombok:1.18.16
// camel-k: dependency=github:piomin/entity-model/1.0-SNAPSHOT

@ApplicationScoped
public class OrderRoute extends RouteBuilder {

    private static long num = 0;

    @Override
    public void configure() throws Exception {

        from("timer:tick?period=10000")
            .setBody(exchange -> new Order(null, (int) ++num%10+1, (int) num%10+1, 100, 1, OrderStatus.NEW))
            .to("jpa:" + Order.class.getName())
            .marshal().json(JsonLibrary.Jackson)
            .log("New Order: ${body}")
            .toD("kafka:order-events?brokers=${env.KAFKA_BOOTSTRAP_SERVERS}");

        JacksonDataFormat format = new JacksonDataFormat();
        format.setUnmarshalType(Order.class);

        rest("/orders")
            .post("/confirm").consumes("application/json")//.type(Order.class)
            .route()
                .unmarshal().json(JsonLibrary.Jackson, Order.class)
                .toD("jpa:" + Order.class.getName() + "?query=select o from Order o where o.id= ${body.id}")
                .log("Found Order: ${body[0]}")
                .choice()
                    .when().simple("${body[0].status.toString()} == 'NEW'")
                        .setBody(exchange -> updateOrderStatus(exchange, OrderStatus.IN_PROGRESS))
                    .otherwise()
                        .setBody(exchange -> updateOrderStatus(exchange, OrderStatus.CONFIRMED))
                        .marshal().json(JsonLibrary.Jackson)
                        .log("Order confirmed: ${body}")
                        .toD("kafka:order-events?brokers=${env.KAFKA_BOOTSTRAP_SERVERS}")
                .end()
                .unmarshal().json(JsonLibrary.Jackson, Order.class)
                .to("jpa:" + Order.class.getName() + "?useExecuteUpdate=true")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201)).setBody(constant(null))
        .endRest();
    }

    private Order updateOrderStatus(Exchange exchange, OrderStatus status) {
        Order order = (Order) exchange.getIn().getBody(List.class).get(0);
        order.setStatus(status);
        return order;
    }

}