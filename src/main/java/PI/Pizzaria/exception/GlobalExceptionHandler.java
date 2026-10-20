package PI.Pizzaria.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice   
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity <String> tratarProdutoNãoEncontrado(
        ProductNotFoundException exception){

            return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(exception.getMessage());
        }
        
 @ExceptionHandler(ColaboradorNotFoundException.class)
    public ResponseEntity<String> tratarColaboradorNãoEncontrado(
            ColaboradorNotFoundException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exception.getMessage());
    }

}
