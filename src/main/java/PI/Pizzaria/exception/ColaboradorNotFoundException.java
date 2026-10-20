package PI.Pizzaria.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ColaboradorNotFoundException extends RuntimeException {

     public ColaboradorNotFoundException(String message) {
        super(message);
    }

}
