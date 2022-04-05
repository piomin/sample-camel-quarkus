package pl.piomin.samples.quarkus.serverless.customer;

import com.github.piomin.entity.model.customer.Customer;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.rest.RestBindingMode;

import javax.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

// camel-k: trait=knative-service.enabled=true
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-core
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-jpa
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-jackson
// camel-k: dependency=mvn:io.quarkus:quarkus-jdbc-postgresql
// camel-k: dependency=mvn:org.projectlombok:lombok:1.18.22
// camel-k: dependency=github:piomin/entity-model/1.1

@ApplicationScoped
public class CustomerRoute extends RouteBuilder {

    private static long i = 0;
    private final JacksonDataFormat FORMAT = new JacksonDataFormat(Order.class);

    @Override
    public void configure() throws Exception {
        restConfiguration().bindingMode(RestBindingMode.json);

        rest("/customers")
            .post("/reserve").consumes("application/json").type(Order.class)
            .route()
                .log("Order received: ${body}")
                .setProperty("order", body())
                .setProperty("orderAmount", simple("${body.amount}", Integer.class))
                .setProperty("orderStatus", simple("${body.status}", OrderStatus.class))
                .toD("jpa:" + Customer.class.getName() + "?query=select c from Customer c where c.id = ${body.customerId}")
                .choice()
                    .when().simple("${exchangeProperty.orderStatus} == 'NEW' && ${body[0].getAmountAvailable()} >= ${exchangeProperty.orderAmount}")
                        .setBody(exchange -> {
                            Customer customer = (Customer) exchange.getIn().getBody(List.class).get(0);
                            customer.setAmountReserved(customer.getAmountReserved() + exchange.getProperty("orderAmount", Integer.class));
                            customer.setAmountAvailable(customer.getAmountAvailable() - exchange.getProperty("orderAmount", Integer.class));
                            return customer;
                        }).setProperty("newStatus", constant("IN_PROGRESS")).endChoice()
                    .when().simple("${exchangeProperty.orderStatus} == 'NEW' && ${body[0].getAmountAvailable()} < ${exchangeProperty.orderAmount}")
                        .setBody(exchange -> exchange.getIn().getBody(List.class).get(0))
                        .setProperty("newStatus", constant("REJECTED")).endChoice()
                    .otherwise()
                        .setBody(exchange -> {
                            Customer customer = (Customer) exchange.getIn().getBody(List.class).get(0);
                            customer.setAmountReserved(customer.getAmountReserved() - exchange.getProperty("orderAmount", Integer.class));
                            return customer;
                        })
                .end()
                .log("Current customer: ${body}")
                .to("jpa:" + Customer.class.getName() + "?useExecuteUpdate=true")
                .choice()
                    .when(simple("${exchangeProperty.newStatus} != null"))
                    .setBody(exchange -> {
                        Order o = exchange.getProperty("order", Order.class);
                        o.setStatus(OrderStatus.valueOf(exchange.getProperty("newStatus").toString()));
                        return o;
                    })
                    .log("Reservation sent: ${body}")
                    .marshal(FORMAT)
                    .toD("kafka:reserve-events?brokers=${env.KAFKA_BOOTSTRAP_SERVERS}")
                .end()
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
                .setBody(constant(null))
            .end()
        .endRest();

        Random r = new Random();
        from("timer://runOnce?repeatCount=1&delay=100")
            .loop(10)
                .setBody(exchange -> new Customer(null, "Test"+(++i), r.nextInt(50000), 0))
                .to("jpa:" + Customer.class.getName())
                .log("Add: ${body}")
            .end();
    }

}

@Data
class Order implements Serializable {
    private Long id;
    private Long customerId;
    private int amount;
    private OrderStatus status;
    private Long productId;
    private int productCount;
}

enum OrderStatus {
    NEW, REJECTED, CONFIRMED, IN_PROGRESS;
}
