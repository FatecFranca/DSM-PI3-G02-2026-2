package PI.Pizzaria.dto;

public class LoginResponse {
    private String matricula;
    private String perfil;

    public String getMatricula(String matricula){
        return matricula;
    }
    public String getPerfil(String perfil){
        return perfil;
    }

    public LoginResponse(String matricula, String perfil){
        this.matricula = matricula;
        this.perfil = perfil;
    }
}
