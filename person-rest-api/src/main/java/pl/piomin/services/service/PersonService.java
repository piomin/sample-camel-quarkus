package pl.piomin.services.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.apache.camel.Header;
import pl.piomin.services.model.Person;

import java.util.List;

@ApplicationScoped
public class PersonService {

    @Transactional
    public List<Person> findAll() {
        return Person.listAll();
    }

    @Transactional
    public Person findById(@Header("id") Long id) {
        return Person.findById(id);
    }

    @Transactional
    public Person create(Person person) {
        person.persist();
        return person;
    }

    @Transactional
    public Person update(@Header("id") Long id, Person person) {
        Person existing = Person.findById(id);
        if (existing == null) {
            return null;
        }
        existing.firstName = person.firstName;
        existing.lastName = person.lastName;
        existing.email = person.email;
        existing.age = person.age;
        existing.gender = person.gender;
        return existing;
    }

    @Transactional
    public void delete(@Header("id") Long id) {
        Person.deleteById(id);
    }
}