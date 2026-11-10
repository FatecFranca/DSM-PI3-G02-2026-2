package PI.Pizzaria.dto;

public class LoginResponse {
    private final String matricula;
    private final String perfil;
    private final String nome;
    private final String token;

    public LoginResponse(String matricula, String perfil, String nome, String token) {
        this.matricula = matricula;
        this.perfil = perfil;
        this.nome = nome;
        this.token = token;
    }

    public String getMatricula() { return matricula; }
    public String getPerfil() { return perfil; }
    public String getNome() { return nome; }
    public String getToken() { return token; }
}
