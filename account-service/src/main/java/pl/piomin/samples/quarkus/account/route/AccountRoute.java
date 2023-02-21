package pl.piomin.samples.quarkus.account.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-jackson
// camel-k: dependency=mvn:org.projectlombok:lombok:1.18.16

@ApplicationScoped
public class AccountRoute extends RouteBuilder {

	AccountService accountService = new AccountService();

	@Override
	public void configure() throws Exception {
		restConfiguration().bindingMode(RestBindingMode.json);

		rest("/accounts")
				.get("/{id}").to("direct:findById")
				.get("/customer/{customerId}").to("direct:findByCustomerId")
				.get().to("direct:findAll")
				.post()
					.consumes("application/json").type(Account.class)
					.to("direct:add");

		from("direct:findById").bean(accountService, "findById(${header.id})");
		from("direct:findByCustomerId").bean(accountService, "findByCustomerId(${header.customerId})");
		from("direct:findAll").bean(accountService, "findAll");
		from("direct:add").bean(accountService, "add(${body})");
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Account {
		private Integer id;
		private String number;
		private int amount;
		private Integer customerId;
	}

	public class AccountService {

		private List<Account> accounts = new ArrayList<>();

		AccountService() {
			accounts.add(new Account(1, "1234567890", 5000, 1));
			accounts.add(new Account(2, "1234567891", 12000, 1));
			accounts.add(new Account(3, "1234567892", 30000, 2));
		}

		public Account findById(Integer id) {
			return accounts.stream()
					.filter(it -> it.getId().equals(id))
					.findFirst().get();
		}

		public List<Account> findByCustomerId(Integer customerId) {
			return accounts.stream()
					.filter(it -> it.getCustomerId().equals(customerId))
					.collect(Collectors.toList());
		}

		public List<Account> findAll() {
			return accounts;
		}

		public Account add(Account account) {
			account.setId(accounts.size() + 1);
			accounts.add(account);
			return account;
		}

	}
}
