package pl.piomin.services;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

@ApplicationScoped
public class ApiRoute extends RouteBuilder {

    @Override
    public void configure() {

        restConfiguration()
                .component("platform-http")
                .bindingMode(RestBindingMode.json);

        rest("/api")
                .get("/rates")
                .description("Get exchange rates")
                .responseMessage()
                .code(200)
                .message("OK")
                .endResponseMessage()
                .to("direct:getRates");

        from("direct:getRates")
                .log("${body}")
                .setBody(constant("OK"));
    }
}
