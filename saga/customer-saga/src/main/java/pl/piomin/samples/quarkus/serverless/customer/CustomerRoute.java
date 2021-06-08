package pl.piomin.samples.quarkus.serverless.customer;

import lombok.*;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class CustomerRoute extends RouteBuilder {

    static long i = 0;

    @Override
    public void configure() throws Exception {

        JacksonDataFormat format = new JacksonDataFormat();
        format.setUnmarshalType(Order.class);

        rest("/customers")
            .post("/reserve").consumes("application/json").type(Order.class)
            .route()
            .unmarshal(format)
            .choice()
                .when().simple("${body.status.toString()} == 'NEW'")
                    .setBody(exchange -> {
                        Order order = exchange.getIn().getBody(Order.class);
                        order.setStatus(OrderStatus.IN_PROGRESS);
                        return order;
                    })
                    .toD("kafka:order-events?brokers=${env.KAFKA_BOOTSTRAP_SERVERS}")
            .end()
            .setProperty("orderAmount", simple("${body.amount}", Integer.class))
            .setProperty("orderStatus", simple("${body.status}", OrderStatus.class))
            .toD("jpa:" + Customer.class.getName() + "?query=select c from Customer c where c.id= ${body.customerId}")
                .choice()
                    .when().simple("${exchangeProperty.orderStatus} == 'IN_PROGRESS'")
                        .setBody(exchange -> {
                            Customer customer = (Customer) exchange.getIn().getBody(List.class).get(0);
                            customer.setAmountReserved(customer.getAmountReserved() + exchange.getProperty("orderAmount", Integer.class));
                            customer.setAmountAvailable(customer.getAmountAvailable() - exchange.getProperty("orderAmount", Integer.class));
                            return customer;
                        })
                    .otherwise()
                        .setBody(exchange -> {
                            Customer customer = (Customer) exchange.getIn().getBody(List.class).get(0);
                            customer.setAmountReserved(customer.getAmountReserved() - exchange.getProperty("orderAmount", Integer.class));
                            return customer;
                        })
                .end()
            .marshal(format)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
class Customer implements Serializable {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private int amountAvailable;
    private int amountReserved;
}

@Data
class Order {
    private Long id;
    private Long customerId;
    private int amount;
    private OrderStatus status;
}

enum OrderStatus {
    NEW, REJECTED, CONFIRMED, IN_PROGRESS;
}
