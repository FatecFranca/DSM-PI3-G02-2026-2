package PI.Pizzaria.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import PI.Pizzaria.dto.ViaCepResponse;
import PI.Pizzaria.model.Endereco;

@Service
public class ViaCepService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Endereco consultarCep(String cep) {

        cep = cep.replaceAll("\\D", "");

        if (!cep.matches("\\d{8}")) {
            throw new IllegalArgumentException("CEP inválido.");
        }

        String url = "https://viacep.com.br/ws/" + cep + "/json/";

        try {
            ViaCepResponse resposta = restTemplate.getForObject(
                    url,
                    ViaCepResponse.class
            );

            if (resposta == null || resposta.isErro()) {
                throw new IllegalArgumentException("CEP não encontrado.");
            }

            Endereco endereco = new Endereco();

            endereco.setCep(resposta.getCep());
            endereco.setRua(resposta.getLogradouro());
            endereco.setBairro(resposta.getBairro());
            endereco.setCidade(resposta.getLocalidade());
            endereco.setUf(resposta.getUf());

            return endereco;

        } catch (RestClientException e) {
            throw new RuntimeException(
                    "Serviço ViaCEP indisponível.",
                    e
            );
        }
    }
}