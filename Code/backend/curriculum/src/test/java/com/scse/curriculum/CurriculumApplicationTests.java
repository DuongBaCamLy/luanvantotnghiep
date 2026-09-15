package com.scse.curriculum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CurriculumApplicationTests {

    @Autowired private DataSource dataSource;
    @Autowired private Environment environment;

    @Test
    void actualConnectionUsesOnlyDisposableTestDatabase() throws Exception {
        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("create");
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getCatalog()).isEqualTo("curriculum_iu_test");
            assertThat(connection.getMetaData().getURL())
                    .startsWith("jdbc:mysql://localhost:3306/curriculum_iu_test?");
            System.out.println("VERIFIED TEST CONNECTION: catalog=" + connection.getCatalog()
                    + ", url=" + connection.getMetaData().getURL());
        }
    }

	@Test
	void contextLoads() {
	}

}
