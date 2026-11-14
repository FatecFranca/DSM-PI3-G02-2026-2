package PI.Pizzaria;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.pedido.migracao-habilitada=false")
class PizzariaApplicationTests {

	@Test
	void contextLoads() {
	}

}
