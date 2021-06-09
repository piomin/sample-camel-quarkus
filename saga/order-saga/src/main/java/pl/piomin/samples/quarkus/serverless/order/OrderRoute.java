package pl.piomin.samples.quarkus.serverless.order;

import com.github.piomin.entity.model.order.Order;
import com.github.piomin.entity.model.order.OrderStatus;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

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
                .setBody(constant(new Order(null, (int) ++num%10+1, (int) num%10+1, 100, 1, OrderStatus.NEW)))
//                .to("kafka:test?brokers=my-cluster-kafka-bootstrap.kafka:9092")
                .to("jpa:" + Order.class.getName())
                .log("Order id=${body.id}");

        JacksonDataFormat format = new JacksonDataFormat();
        format.setUnmarshalType(Order.class);

        rest("/orders")
                .post("/confirm").consumes("application/json").type(Order.class)
                .route()
                    .unmarshal(format)
//                .unmarshal().json(JsonLibrary.Jackson, Data.class)
                    .toD("jpa:" + Order.class.getName() + "?query=select o from Order o where o.id= ${body.id}")
                    .log("Status: ${body[0].status.toString()}")
                    .choice()
                        .when().simple("${body[0].status.toString()} == 'NEW'")
                            .setBody(exchange -> updateOrderStatus(exchange, OrderStatus.IN_PROGRESS))
                        .otherwise()
                            .setBody(exchange -> updateOrderStatus(exchange, OrderStatus.CONFIRMED))
    //                    .to("kafka:order-events?brokers=my-cluster-kafka-bootstrap.kafka:9092")
                    .end()
                .log("Order: ${body}")
                    .to("jpa:" + Order.class.getName() + "?useExecuteUpdate=true")
                    .marshal(format)
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
                .endRest();
    }

    private Order updateOrderStatus(Exchange exchange, OrderStatus status) {
        Order order = (Order) exchange.getIn().getBody(List.class).get(0);
        order.setStatus(status);
        return order;
    }

}