package pl.piomin.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import pl.piomin.services.model.Person;
import pl.piomin.services.service.PersonService;

@ApplicationScoped
public class ApiRoute extends RouteBuilder {

    @Inject
    PersonService personService;

    @Override
    public void configure() {

        restConfiguration()
                .component("platform-http")
                .bindingMode(RestBindingMode.json);

        rest("/persons")
                .get()
                    .description("Get all persons")
                    .to("direct:getPersons")
                .get("/{id}")
                    .description("Get person by id")
                    .to("direct:getPersonById")
                .post()
                    .description("Create person")
                    .type(Person.class)
                    .to("direct:createPerson")
                .put("/{id}")
                    .description("Update person")
                    .type(Person.class)
                    .to("direct:updatePerson")
                .delete("/{id}")
                    .description("Delete person")
                    .to("direct:deletePerson");

        from("direct:getPersons")
                .bean(personService, "findAll");

        from("direct:getPersonById")
                .bean(personService, "findById");

        from("direct:createPerson")
                .bean(personService, "create");

        from("direct:updatePerson")
                .bean(personService, "update");

        from("direct:deletePerson")
                .bean(personService, "delete");
    }
}
