package com.spendly.spendly;

import com.spendly.spendly.config.DataSeeder;
import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.repository.AppUserRepository;
import com.spendly.spendly.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SpendlyApplicationTests {

	@Autowired
	AppUserRepository userRepository;
	@Autowired
	TransactionRepository transactionRepository;

	@Test
	void contextLoadsAndSeedsTheDemoAccount() {
		AppUser demo = userRepository.findByUsernameIgnoreCase(DataSeeder.DEMO_USERNAME).orElseThrow();
		assertThat(transactionRepository.countByUser(demo)).isGreaterThan(100);
	}

}
