package br.edu.pos.apiprodutos.repository;

import br.edu.pos.apiprodutos.model.Produto;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoRepositoryMemoria implements ProdutoRepository {
    private final List<Produto> produtos = new ArrayList<>();
    private long proximoId = 1;

    @Override
    public void salvar(Produto produto) {
        if (produto.getId() == null) produto.setId(proximoId++);
        produtos.removeIf(p -> p.getId().equals(produto.getId()));
        produtos.add(produto);
    }

    @Override
    public List<Produto> listarTodos() {
        return new ArrayList<>(produtos);
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        return produtos.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    @Override
    public void excluir(Long id) {
        produtos.removeIf(p -> p.getId().equals(id));
    }

    @Override
    public boolean adicionarEstoque(Long id, int quantidade) {
        Optional<Produto> encontrado = buscarPorId(id);
        encontrado.ifPresent(p -> p.setQuantidadeEstoque(p.getQuantidadeEstoque() + quantidade));
        return encontrado.isPresent();
    }

    @Override
    public boolean retirarEstoque(Long id, int quantidade) {
        Optional<Produto> encontrado = buscarPorId(id);
        if (encontrado.isEmpty() || encontrado.get().getQuantidadeEstoque() < quantidade) return false;
        encontrado.get().setQuantidadeEstoque(encontrado.get().getQuantidadeEstoque() - quantidade);
        return true;
    }

    @Override
    public List<Produto> listarComEstoqueAbaixoOuIgual(int limite) {
        return produtos.stream().filter(p -> p.getQuantidadeEstoque() <= limite).toList();
    }
}
