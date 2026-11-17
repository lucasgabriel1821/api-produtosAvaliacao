package br.edu.pos.apiprodutos.service;

import br.edu.pos.apiprodutos.model.Produto;
import br.edu.pos.apiprodutos.repository.ProdutoRepository;
import java.util.List;
import java.util.NoSuchElementException;

public class ProdutoService {
    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    public Produto cadastrar(String nome, double preco) {
        return cadastrar(nome, preco, 0);
    }

    public Produto cadastrar(String nome, double preco, int quantidadeEstoque) {
        validarProduto(nome, preco);
        validarQuantidadeEstoqueInicial(quantidadeEstoque);
        Produto produto = new Produto(null, nome, preco, quantidadeEstoque);
        repository.salvar(produto);
        return produto;
    }

    public List<Produto> listar() {
        return repository.listarTodos();
    }

    public Produto buscarPorId(Long id) {
        validarId(id);
        return repository.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));
    }

    public Produto atualizar(Long id, String nome, double preco) {
        validarProduto(nome, preco);
        Produto produto = buscarPorId(id);
        produto.setNome(nome);
        produto.setPreco(preco);
        repository.salvar(produto);
        return produto;
    }

    public void excluir(Long id) {
        buscarPorId(id);
        repository.excluir(id);
    }

    public Produto registrarEntrada(Long id, int quantidade) {
        validarMovimentacao(quantidade);
        if (!repository.adicionarEstoque(id, quantidade)) {
            throw new NoSuchElementException("Produto não encontrado: " + id);
        }
        return buscarPorId(id);
    }

    public Produto registrarSaida(Long id, int quantidade) {
        validarMovimentacao(quantidade);
        if (!repository.retirarEstoque(id, quantidade)) {
            // Diferencia produto inexistente de saldo insuficiente.
            buscarPorId(id);
            throw new IllegalArgumentException("Estoque insuficiente para realizar a saída.");
        }
        return buscarPorId(id);
    }

    public List<Produto> listarComEstoqueAbaixoOuIgual(int limite) {
        if (limite < 0) {
            throw new IllegalArgumentException("O limite de estoque não pode ser negativo.");
        }
        return repository.listarComEstoqueAbaixoOuIgual(limite);
    }

    private void validarProduto(String nome, double preco) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        if (!Double.isFinite(preco) || preco <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }
    }

    private void validarQuantidadeEstoqueInicial(int quantidade) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("O estoque inicial não pode ser negativo.");
        }
    }

    private void validarMovimentacao(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade da movimentação deve ser maior que zero.");
        }
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("O ID do produto deve ser maior que zero.");
        }
    }
}
