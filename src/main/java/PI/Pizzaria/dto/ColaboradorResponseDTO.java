package PI.Pizzaria.dto;

import PI.Pizzaria.model.Colaborador;

public class ColaboradorResponseDTO {

    private String id;
    private String nome;
    private String matricula;
    private String email;
    private Colaborador.Perfil perfil;

    public ColaboradorResponseDTO(Colaborador colaborador) {
        this.id = colaborador.getId();
        this.nome = colaborador.getNome();
        this.matricula = colaborador.getMatricula();
        this.email = colaborador.getEmail();
        this.perfil = colaborador.getPerfil();
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getEmail() {
        return email;
    }

    public Colaborador.Perfil getPerfil() {
        return perfil;
    }

}
