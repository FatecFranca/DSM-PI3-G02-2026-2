package PI.Pizzaria.dto;

public class LoginRequest {
    private String matricula;
    private String senha;

    public String getMatricula(){
        return matricula;
    }
    
    public String getSenha(){
        return senha;
    }

    public void setMatricula(String matricula){
        this.matricula = matricula;    
    }
    
    public void setSenha(String senha){
        this.senha = senha;    
    }
}
