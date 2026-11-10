package PI.Pizzaria.dto;

public class RedefinicaoAdminRequest {
    private String colaboradorId;
    private String novaSenha;

    public String getColaboradorId() { return colaboradorId; }
    public void setColaboradorId(String colaboradorId) { this.colaboradorId = colaboradorId; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
}
