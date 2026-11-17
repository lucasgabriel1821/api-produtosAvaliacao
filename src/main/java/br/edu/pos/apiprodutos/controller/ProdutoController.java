package br.edu.pos.apiprodutos.controller;

import br.edu.pos.apiprodutos.model.Produto;
import br.edu.pos.apiprodutos.service.ProdutoService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import java.util.NoSuchElementException;

public class ProdutoController {
    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    public void registrarRotas(Javalin app) {

        // GET
        app.get("/produtos", this::listarProdutos);
        app.get("/produtos/estoque-baixo", this::listarEstoqueBaixo);
        app.get("/produtos/{id}", this::buscarProduto);

        // POST
        app.post("/produtos", this::cadastrarProduto);
        app.post("/produtos/{id}/estoque/entrada", this::registrarEntrada);
        app.post("/produtos/{id}/estoque/saida", this::registrarSaida);

        // PUT
        app.put("/produtos/{id}", this::atualizarProduto);

        // DELETE
        app.delete("/produtos/{id}", this::excluirProduto);

        app.exception(NoSuchElementException.class, (erro, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new MensagemErro(erro.getMessage()));
        });

        app.exception(IllegalArgumentException.class, (erro, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new MensagemErro(erro.getMessage()));
        });
    }

    @OpenApi(path = "/produtos", methods = {HttpMethod.GET}, summary = "Listar todos os produtos",
            tags = {"Produtos"}, responses = {@OpenApiResponse(status = "200",
            content = {@OpenApiContent(from = Produto[].class)})})
    public void listarProdutos(Context ctx) {
        ctx.json(service.listar());
    }

    @OpenApi(path = "/produtos/{id}", methods = {HttpMethod.GET}, summary = "Buscar produto por ID",
            tags = {"Produtos"}, pathParams = {@OpenApiParam(name = "id", type = Long.class,
            description = "Identificador do produto", required = true)},
            responses = {@OpenApiResponse(status = "200", content = {@OpenApiContent(from = Produto.class)}),
                    @OpenApiResponse(status = "404")})
    public void buscarProduto(Context ctx) {
        ctx.json(service.buscarPorId(parseId(ctx)));
    }

    @OpenApi(path = "/produtos", methods = {HttpMethod.POST}, summary = "Cadastrar um novo produto",
            tags = {"Produtos"}, requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = Produto.class)}),
            responses = {@OpenApiResponse(status = "201", content = {@OpenApiContent(from = Produto.class)}),
                    @OpenApiResponse(status = "400")})
    public void cadastrarProduto(Context ctx) {
        Produto dados = ctx.bodyAsClass(Produto.class);
        Produto produto = service.cadastrar(dados.getNome(), dados.getPreco(), dados.getQuantidadeEstoque());
        ctx.status(HttpStatus.CREATED);
        ctx.json(produto);
    }

    @OpenApi(path = "/produtos/{id}", methods = {HttpMethod.PUT}, summary = "Atualizar um produto existente",
            tags = {"Produtos"}, pathParams = {@OpenApiParam(name = "id", type = Long.class,
            description = "Identificador do produto", required = true)},
            requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = Produto.class)}),
            responses = {@OpenApiResponse(status = "200", content = {@OpenApiContent(from = Produto.class)}),
                    @OpenApiResponse(status = "400"), @OpenApiResponse(status = "404")})
    public void atualizarProduto(Context ctx) {
        Long id = parseId(ctx);
        Produto dados = ctx.bodyAsClass(Produto.class);
        // O estoque não é alterado pelo PUT; movimentações usam os endpoints próprios.
        ctx.json(service.atualizar(id, dados.getNome(), dados.getPreco()));
    }

    @OpenApi(path = "/produtos/{id}", methods = {HttpMethod.DELETE}, summary = "Excluir um produto",
            tags = {"Produtos"}, pathParams = {@OpenApiParam(name = "id", type = Long.class,
            description = "Identificador do produto", required = true)},
            responses = {@OpenApiResponse(status = "204"), @OpenApiResponse(status = "404")})
    public void excluirProduto(Context ctx) {
        service.excluir(parseId(ctx));
        ctx.status(HttpStatus.NO_CONTENT);
    }

    @OpenApi(path = "/produtos/{id}/estoque/entrada", methods = {HttpMethod.POST},
            summary = "Registrar entrada de unidades no estoque", tags = {"Estoque"},
            pathParams = {@OpenApiParam(name = "id", type = Long.class, description = "ID do produto", required = true)},
            requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = MovimentacaoEstoque.class)}),
            responses = {@OpenApiResponse(status = "200", content = {@OpenApiContent(from = Produto.class)}),
                    @OpenApiResponse(status = "400"), @OpenApiResponse(status = "404")})
    public void registrarEntrada(Context ctx) {
        MovimentacaoEstoque dados = ctx.bodyAsClass(MovimentacaoEstoque.class);
        ctx.json(service.registrarEntrada(parseId(ctx), dados.quantidade()));
    }

    @OpenApi(path = "/produtos/{id}/estoque/saida", methods = {HttpMethod.POST},
            summary = "Registrar saída de unidades do estoque", tags = {"Estoque"},
            pathParams = {@OpenApiParam(name = "id", type = Long.class, description = "ID do produto", required = true)},
            requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = MovimentacaoEstoque.class)}),
            responses = {@OpenApiResponse(status = "200", content = {@OpenApiContent(from = Produto.class)}),
                    @OpenApiResponse(status = "400"), @OpenApiResponse(status = "404")})
    public void registrarSaida(Context ctx) {
        MovimentacaoEstoque dados = ctx.bodyAsClass(MovimentacaoEstoque.class);
        ctx.json(service.registrarSaida(parseId(ctx), dados.quantidade()));
    }

    @OpenApi(path = "/produtos/estoque-baixo", methods = {HttpMethod.GET},
            summary = "Listar produtos com estoque abaixo ou igual ao limite", tags = {"Estoque"},
            queryParams = {@OpenApiParam(name = "limite", type = Integer.class,
                    description = "Limite máximo de unidades em estoque", required = true)},
            responses = {@OpenApiResponse(status = "200", content = {@OpenApiContent(from = Produto[].class)}),
                    @OpenApiResponse(status = "400")})
    public void listarEstoqueBaixo(Context ctx) {
        String valorLimite = ctx.queryParam("limite");
        if (valorLimite == null) valorLimite = ctx.queryParam("limit");
        if (valorLimite == null || valorLimite.isBlank()) {
            throw new IllegalArgumentException("Informe o parâmetro 'limite'.");
        }
        try {
            ctx.json(service.listarComEstoqueAbaixoOuIgual(Integer.parseInt(valorLimite)));
        } catch (NumberFormatException erro) {
            throw new IllegalArgumentException("O limite deve ser um número inteiro.");
        }
    }

    private Long parseId(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException erro) {
            throw new IllegalArgumentException("O ID deve ser um número inteiro válido.");
        }
    }

    public record MovimentacaoEstoque(int quantidade) {
    }

    public record MensagemErro(String mensagem) {
    }
}
