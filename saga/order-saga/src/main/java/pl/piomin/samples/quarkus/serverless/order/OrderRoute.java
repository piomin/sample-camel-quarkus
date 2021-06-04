package pl.piomin.samples.quarkus.serverless.order;

import lombok.*;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.spi.DataFormat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

// camel-k: trait=knative-service.enabled=true
// camel-k: trait=quarkus.enabled=false
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-jpa
// camel-k: dependency=mvn:io.quarkus:quarkus-jdbc-h2
// camel-k: dependency=mvn:org.projectlombok:lombok:1.18.16

@ApplicationScoped
public class OrderRoute extends RouteBuilder {

    private static long num = 0;

    @Inject
    OrderUpdateProcessor processor;

    @Override
    public void configure() throws Exception {

        from("timer:tick?period=10000")
                .setBody(constant(new Order(null, (int) num%10+1, (int) num%10+1, 100, 1, OrderStatus.NEW)))
//                .to("kafka:test?brokers=my-cluster-kafka-bootstrap.kafka:9092")
                .to("jpa:" + Order.class.getName())
                .log("Order id=${body.id}");

        JacksonDataFormat format = new JacksonDataFormat();
        format.setUnmarshalType(Order.class);

        rest("/orders")
                .post("/confirm")
                .consumes("application/json").type(Order.class)
                .route()
                    .unmarshal(format)
                    .log("Order id=${body.id}, status=${body.status}")
                    .toD("jpa:" + Order.class.getName() + "?query=select o from Order o where o.id= ${body.id}")
                .choice()
                    .when().simple("${body[0].status} == 'NEW'")
                        .setBody(simple("${body[0].updateStatus(OrderStatus.IN_PROGRESS)}"))
                        .to("jpa:" + Order.class.getName() + "?useExecuteUpdate=true")
                        .marshal(format)
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
                    .otherwise()
                        .log("Order status=${body.status}")
                        .process(processor)
                        .to("jpa:" + Order.class.getName() + "?useExecuteUpdate=true")
//                    .to("kafka:test?brokers=my-cluster-kafka-bootstrap.kafka:9092")
                .endChoice()
                .endRest();
    }

}

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
class Order implements Serializable {

    @Id
    @GeneratedValue
    private Long id;
    private Integer customerId;
    private Integer productId;
    private int amount;
    private int productCount;
    @Enumerated
    private OrderStatus status = OrderStatus.NEW;

    public Order updateStatus(OrderStatus status) {
        this.setStatus(status);
        return this;
    }
}

enum OrderStatus {
    NEW, REJECTED, CONFIRMED, IN_PROGRESS;
}

@ApplicationScoped
class OrderUpdateProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Order order = (Order) exchange.getIn().getBody(List.class).get(0);
        order.setStatus(OrderStatus.IN_PROGRESS);
        exchange.getIn().setBody(order);
    }
}