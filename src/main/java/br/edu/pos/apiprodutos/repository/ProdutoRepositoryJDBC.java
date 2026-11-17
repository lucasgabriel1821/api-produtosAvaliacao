package br.edu.pos.apiprodutos.repository;

import br.edu.pos.apiprodutos.config.Database;
import br.edu.pos.apiprodutos.model.Produto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoRepositoryJDBC implements ProdutoRepository {

    @Override
    public void salvar(Produto produto) {
        if (produto.getId() == null) inserir(produto);
        else atualizar(produto);
    }

    private void inserir(Produto produto) {
        String sql = "INSERT INTO produtos (nome, preco, quantidade_estoque) VALUES (?, ?, ?)";
        try (Connection conexao = Database.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            comando.setString(1, produto.getNome());
            comando.setDouble(2, produto.getPreco());
            comando.setInt(3, produto.getQuantidadeEstoque());
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (chaves.next()) produto.setId(chaves.getLong(1));
                else throw new SQLException("O banco não retornou o ID do produto");
            }
        } catch (SQLException erro) {
            throw new RuntimeException("Erro ao inserir produto", erro);
        }
    }

    private void atualizar(Produto produto) {
        String sql = "UPDATE produtos SET nome = ?, preco = ?, quantidade_estoque = ? WHERE id = ?";
        try (Connection conexao = Database.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, produto.getNome());
            comando.setDouble(2, produto.getPreco());
            comando.setInt(3, produto.getQuantidadeEstoque());
            comando.setLong(4, produto.getId());
            comando.executeUpdate();
        } catch (SQLException erro) {
            throw new RuntimeException("Erro ao atualizar produto", erro);
        }
    }

    @Override
    public List<Produto> listarTodos() {
        String sql = "SELECT id, nome, preco, quantidade_estoque FROM produtos ORDER BY id";
        List<Produto> produtos = new ArrayList<>();
        try (Connection conexao = Database.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) produtos.add(converterProduto(resultado));
        } catch (SQLException erro) {
            throw new RuntimeException("Erro ao listar produtos", erro);
        }
        return produtos;
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        String sql = "SELECT id, nome, preco, quantidade_estoque FROM produtos WHERE id = ?";
        try (Connection conexao = Database.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, id);
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) return Optional.of(converterProduto(resultado));
            }
        } catch (SQLException erro) {
            throw new RuntimeException("Erro ao buscar produto", erro);
        }
        return Optional.empty();
    }

    @Override
    public void excluir(Long id) {
        try (Connection conexao = Database.conectar();
             PreparedStatement comando = conexao.prepareStatement("DELETE FROM produtos WHERE id = ?")) {
            comando.setLong(1, id);
            comando.executeUpdate();
        } catch (SQLException erro) {
            throw new RuntimeException("Erro ao excluir produto", erro);
        }
    }

    @Override
    public boolean adicionarEstoque(Long id, int quantidade) {
        String sql = "UPDATE produtos SET quantidade_estoque = quantidade_estoque + ? WHERE id = ?";
        return executarMovimentacao(sql, id, quantidade);
    }

    @Override
    public boolean retirarEstoque(Long id, int quantidade) {
        //impede saldo negativo mesmo com requisições concorrentes. (Pegadinha do professor kkk)
        String sql = "UPDATE produtos SET quantidade_estoque = quantidade_estoque - ? " +
                "WHERE id = ? AND quantidade_estoque >= ?";
        try (Connection conexao = Database.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, quantidade);
            comando.setLong(2, id);
            comando.setInt(3, quantidade);
            return comando.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException("Erro ao retirar estoque", erro);
        }
    }

    private boolean executarMovimentacao(String sql, Long id, int quantidade) {
        try (Connection conexao = Database.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, quantidade);
            comando.setLong(2, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException("Erro ao movimentar estoque", erro);
        }
    }

    @Override
    public List<Produto> listarComEstoqueAbaixoOuIgual(int limite) {
        String sql = "SELECT id, nome, preco, quantidade_estoque FROM produtos " +
                "WHERE quantidade_estoque <= ? ORDER BY quantidade_estoque, id";
        List<Produto> produtos = new ArrayList<>();
        try (Connection conexao = Database.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, limite);
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) produtos.add(converterProduto(resultado));
            }
        } catch (SQLException erro) {
            throw new RuntimeException("Erro ao consultar estoque baixo", erro);
        }
        return produtos;
    }

    private Produto converterProduto(ResultSet resultado) throws SQLException {
        return new Produto(
                resultado.getLong("id"),
                resultado.getString("nome"),
                resultado.getDouble("preco"),
                resultado.getInt("quantidade_estoque")
        );
    }
}
