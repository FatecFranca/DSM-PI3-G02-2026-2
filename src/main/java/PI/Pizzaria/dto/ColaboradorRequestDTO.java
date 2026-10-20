package PI.Pizzaria.dto;

import PI.Pizzaria.model.Colaborador;

public class ColaboradorRequestDTO {

    private String nome;
    private String matricula;
    private String email;
    private String senha;
    private Colaborador.Perfil perfil;

    public String getNome() {
        return nome;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public Colaborador.Perfil getPerfil() {
        return perfil;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public void setPerfil(Colaborador.Perfil perfil) {
        this.perfil = perfil;
    }

    public Colaborador toColaborador() {
        Colaborador colaborador = new Colaborador();

        colaborador.setNome(nome);
        colaborador.setMatricula(matricula);
        colaborador.setEmail(email);
        colaborador.setSenha(senha);
        colaborador.setPerfil(perfil);

        return colaborador;
    }

}
