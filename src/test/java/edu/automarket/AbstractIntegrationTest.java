package edu.automarket;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.TestPropertySource;

@AutoConfigureWebTestClient
@TestPropertySource(properties = {"app.listing.republishCooldown=2s"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void truncateTables() {
        databaseClient.sql("TRUNCATE TABLE car_listings, users RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .block();
    }
}
