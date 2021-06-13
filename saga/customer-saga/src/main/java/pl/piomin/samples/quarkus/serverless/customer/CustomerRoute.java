package pl.piomin.samples.quarkus.serverless.customer;

import com.github.piomin.entity.model.customer.Customer;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import java.util.List;
import java.util.Random;

public class CustomerRoute extends RouteBuilder {

    static long i = 0;

    @Override
    public void configure() throws Exception {

        rest("/customers")
            .post("/reserve").consumes("application/json")
            .route()
                .log("Order received: ${body}")
                .unmarshal().json(JsonLibrary.Jackson, Order.class)
                .choice()
                    .when().simple("${body.status.toString()} == 'NEW'")
                        .setBody(exchange -> {
                            Order order = exchange.getIn().getBody(Order.class);
                            order.setStatus(OrderStatus.IN_PROGRESS);
                            return order;
                        })
                        .marshal().json(JsonLibrary.Jackson)
                        .log("Reservation sent: ${body}")
                        .toD("kafka:reserve-events?brokers=${env.KAFKA_BOOTSTRAP_SERVERS}")
                .end()
            .unmarshal().json(JsonLibrary.Jackson, Order.class)
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
            .log("Current customer: ${body}")
            .to("jpa:" + Customer.class.getName() + "?useExecuteUpdate=true")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201)).setBody(constant(null))
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
class Order {
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
