# FastEasy — Sistema de Gestão de Supermercado

Aplicação **desktop** para gestão de supermercado, construída com **Java 21 + JavaFX + Spring Boot + PostgreSQL**.
Este repositório contém a **Parte 1** (fundação do projeto), a **Parte 2** (cadastro de **Produtos**, **Fornecedores** e **Categorias**), a base de **controle de estoque** e a **Parte 4** (frente de caixa — **PDV** — e histórico de vendas).

---

## Sumário

- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Configuração do banco de dados](#configuração-do-banco-de-dados)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Como executar](#como-executar)
- [Executar os testes](#executar-os-testes)
- [Regras de negócio](#regras-de-negócio)
- [Migrações Flyway](#migrações-flyway)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Escopo e limitações](#escopo-e-limitações)

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Build | Maven |
| Interface | JavaFX 21 (FXML + CSS) |
| Backend / DI | Spring Boot 3.3.4 |
| Persistência | Spring Data JPA / Hibernate |
| Banco | PostgreSQL |
| Migrações | Flyway |
| Testes | JUnit 5 + Mockito + H2 |

Valores monetários usam **`BigDecimal`** (nunca `float`/`double`).

---

## Arquitetura

O código está organizado em camadas, dentro de `com.fasteasy`:

```
com.fasteasy
├── application      → Ponto de entrada (JavaFX), logging e exceções de aplicação
├── config           → Configuração Spring Boot (component/entity/repository scan)
├── domain           → Entidades JPA (Product, Supplier, Category, Sale, SaleItem, …) e regras puras (CarrinhoVenda, Dinheiro)
├── repository       → Repositórios Spring Data JPA
├── service          → Regras de negócio (ProductService, SupplierService, CategoryService, StockService, SaleService)
├── validator        → Validadores puros (CPF/CNPJ e código de barras EAN)
└── presentation     → JavaFX: controladores e views (formulários, tabelas, PDV e histórico)
```

A UI **não acessa o banco diretamente**: todos os formulários e tabelas conversam apenas com os **serviços de aplicação**.

---

## Pré-requisitos

- **JDK 21**
- **Maven 3.9+**
- **PostgreSQL 14+** em execução

---

## Configuração do banco de dados

Crie o banco e o usuário:

```sql
CREATE DATABASE fasteasy;
CREATE USER fasteasy WITH ENCRYPTED PASSWORD 'troque_esta_senha';
GRANT ALL PRIVILEGES ON DATABASE fasteasy TO fasteasy;
```

As tabelas são criadas automaticamente pelo **Flyway** na primeira execução.

---

## Variáveis de ambiente

Copie o arquivo de exemplo e ajuste os valores:

```bash
cp .env.example .env
```

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `fasteasy` | Nome do banco |
| `DB_USER` | `fasteasy` | Usuário |
| `DB_PASSWORD` | *(vazio)* | Senha |
| `LOG_DIR` | `logs` | Diretório dos arquivos de log |

> **Importante:** nunca comite o arquivo `.env` com credenciais reais.

---

## Como executar

Defina as variáveis de ambiente e execute pela raiz do projeto (`fasteasy/`):

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=fasteasy
export DB_USER=fasteasy
export DB_PASSWORD=sua_senha

mvn clean javafx:run
```

O plugin `javafx-maven-plugin` resolve automaticamente os módulos nativos do JavaFX para o sistema operacional em uso — este é o modo recomendado de execução.

Na primeira execução, o Flyway aplica as migrações que criam as tabelas de categorias, fornecedores, produtos, estoque e vendas.

A janela principal possui um **menu lateral verde** com as seções **Produtos**, **Categorias**, **Fornecedores**, **PDV** e **Vendas**, uma **área principal** com os formulários/tabelas e uma **barra de status**.

### Usando o PDV (frente de caixa)

1. Em **Produtos**, cadastre o item e use a ação **Entrada** para lançar a quantidade inicial em estoque (integração com o `StockService`).
2. Abra **PDV**. O campo de **código de barras** já vem com o foco. Digite/leia o código e pressione **Enter** para localizar.
3. O leitor USB de código de barras funciona como teclado — nenhuma integração de hardware é necessária.
4. Se preferir, use **F2** (buscar por nome/SKU) para abrir o seletor de produtos.
5. Ajuste a quantidade pelos botões **+ / −**, edite a quantidade manualmente ou remova itens (**Delete** ou botão). Itens repetidos apenas incrementam a quantidade, respeitando o estoque.
6. Confira o resumo (itens, subtotal, descontos e total) e pressione **F4** (ou **Pagamento**).
7. No diálogo de pagamento, informe a forma (**Dinheiro**, **PIX**, **Débito**, **Crédito**), o valor aplicado e, em dinheiro, o valor recebido (o troco é calculado). É possível **combinar** formas: a soma aplicada deve ser exatamente igual ao total.
8. Confirme a venda. A baixa de estoque e o registro da venda são **atômicos** (`SaleService`); em caso de erro, nada é gravado.
9. O **comprovante** (não fiscal) é exibido e pode ser impresso. Se a impressão falhar, a venda permanece registrada e o comprovante pode ser reaberto em **Vendas**.

Atalhos: **F2** buscar produto, **F4** pagamento, **Delete** remover item selecionado, **Esc** cancelar/fechar.

O campo de código de barras é limpo e o foco retorna a ele após cada item adicionado; códigos desconhecidos exibem uma mensagem clara sem travar a tela.

### Empacotamento

Para gerar o JAR executável (fat jar) com o Spring Boot:

```bash
mvn clean package
```

> Para executar o JAR diretamente (`java -jar target/fasteasy.jar`) em outro computador, os módulos nativos do JavaFX daquele sistema operacional precisam estar disponíveis no classpath. Em ambientes de desenvolvimento, prefira `mvn javafx:run`.


---

## Executar os testes

Os testes usam **H2 em memória** e **não** exigem PostgreSQL:

```bash
mvn test
```

Cobertura de testes:

- `BarcodeValidatorTest` — EAN-8, EAN-13 e códigos internos
- `DocumentoValidatorTest` — CPF e CNPJ (dígito verificador, com/sem máscara)
- `CategoryServiceTest` — cadastro, duplicidade, edição e desativação
- `SupplierServiceTest` — cadastro, documento inválido, e-mail, duplicidade, desativação
- `ProductServiceTest` — todas as regras de negócio de produto
- `CarrinhoVendaTest` — carrinho: inclusão, repetição, quantidade, remoção, estoque, descontos e trava de finalização duplicada
- `StockServiceTest` — entradas, saídas, ajustes, recusa de estoque insuficiente e movimentação `SALE_EXIT`
- `SaleServiceTest` — finalização, validação de pagamentos (troco, combinado, soma) e revalidações
- `VendaIntegracaoTest` — integração com H2: baixa de estoque, rollback, valores históricos, duplicidade e **concorrência no último item**
- `PersistenciaProdutoTest` — persistência e relacionamentos (JPA + H2)

---

## Regras de negócio

### Produtos

1. **Nome obrigatório.**
2. **Preço de venda maior que zero.**
3. **Custo de aquisição não negativo** (pode ser nulo).
4. **Estoque mínimo não negativo.**
5. **Estoque máximo, quando informado, não pode ser menor que o mínimo.**
6. **SKU único** quando informado.
7. **Código de barras único** quando informado, com validação de **dígito verificador EAN-8/EAN-13**. Códigos **internos** válidos não são rejeitados.
8. Produtos **inativos não aparecem** como opção em operações de venda (`listarAtivosParaVenda`).
9. **Sem exclusão física**: produtos são **desativados** (exclusão lógica), preservando histórico.
10. **Margem bruta** calculada de forma segura (retorna `null` quando o custo é zero/nulo).

### Categorias e Fornecedores

- Nome/razão social **obrigatórios** e **únicos**.
- **CPF/CNPJ** e **e-mail** validados quando informados.
- **Exclusão lógica** via status `ATIVO`/`INATIVO`.

### Estoque

- Toda entrada, saída ou ajuste passa pelo `StockService` e gera uma **movimentação** (`ENTRADA`, `SAIDA`, `AJUSTE` ou `SALE_EXIT`).
- O estoque **nunca fica negativo**: saídas maiores que o saldo disponível são recusadas.
- As operações usam quantidade em `BigDecimal`.
- Na finalização da venda, a leitura do produto acontece com **bloqueio pessimista** (`SELECT … FOR UPDATE`), evitando venda duplicada do último item sob concorrência.

### Vendas (PDV)

- O carrinho (`CarrinhoVenda`) valida: produto **existente e ativo**, **preço não negativo**, **quantidade > 0** e **≤ estoque**; recalcula subtotal, descontos e total a cada alteração.
- A **finalização** é feita exclusivamente pelo `SaleService`, em uma única transação: revalida produtos, preço, status e estoque; grava a venda, os itens (com **nome, código e preço históricos**) e os pagamentos; dá baixa no estoque; e marca a venda como `COMPLETED`. Qualquer falha **reverte tudo**.
- **Carrinhos em andamento não são persistidos**; a venda só existe após a confirmação.
- **Pagamentos**: a soma dos valores aplicados deve ser **exatamente igual** ao total. Em dinheiro, o valor recebido pode ser maior, gerando **troco** (o troco é calculado apenas sobre o dinheiro). PIX, débito e crédito registram apenas a forma e o valor.
- **Trava de duplicidade**: o carrinho possui um guarda (`AtomicBoolean`) que impede finalizar a mesma venda duas vezes; o serviço também rejeita uma segunda tentativa.
- **Alteração manual de preço/desconto** exige autorização; como não há módulo de permissões, a edição de preço/desconto fica **desabilitada** no PDV.
- Vendas concluídas **não são editadas** silenciosamente. O histórico permite consultar por número, período e status, ver detalhes e reimprimir o comprovante.

### Integridade no banco de dados

Além da validação em Java, a migração define restrições no banco:

- `CHECK (preco_venda > 0)`
- `CHECK (custo_aquisicao IS NULL OR custo_aquisicao >= 0)`
- `CHECK (estoque_minimo >= 0)`
- `CHECK (estoque_maximo IS NULL OR estoque_maximo >= estoque_minimo)`
- `CHECK (status IN ('ATIVO','INATIVO'))`
- Índices únicos parciais para `sku` e `codigo_barras` (quando não nulos)
- Chaves estrangeiras de produto → categoria e produto → fornecedor principal
- Estoque: `CHECK (estoque_atual >= 0)` e restrições de quantidade nos movimentos
- Vendas: número único, `CHECK` de valores não negativos, integridade venda ↔ itens e venda ↔ pagamentos, e FK de movimentação de estoque → venda (`SALE_EXIT`)

---

## Migrações Flyway

As migrações ficam em `src/main/resources/db/migration`:

- `V1.0.1__create_product_tables.sql` — tabelas de categorias, fornecedores e produtos, índices e restrições.
- `V1.0.2__create_stock_tables.sql` — coluna de estoque atual e tabela de movimentações.
- `V1.0.3__create_sales_tables.sql` — tabelas de vendas, itens e pagamentos, além do vínculo da movimentação `SALE_EXIT` com a venda.

O Flyway roda automaticamente na inicialização da aplicação (configurável em `application.properties`). As migrações são **incrementais**: migrações já aplicadas não são alteradas.

---

## Estrutura do projeto

```
fasteasy/
├── .env.example
├── pom.xml
└── src
    ├── main
    │   ├── java/com/fasteasy/...       (código da aplicação)
    │   └── resources
    │       ├── application.properties
    │       ├── logback-spring.xml
    │       ├── css/fasteasy.css
    │       ├── view/main-view.fxml
    │       └── db/migration/*.sql
    └── test
        ├── java/com/fasteasy/...       (testes JUnit + Mockito)
        └── resources/application.properties
```

---

## Logs e tratamento de erros

- Logging configurado via **SLF4J + Logback** (`logback-spring.xml`), com saída no **console** e em arquivo rotativo (`logs/fasteasy.log`).
- Um **aspecto** (`ServiceLoggingAspect`) registra execuções e falhas dos serviços de aplicação.
- Falhas de validação de negócio usam mensagens em **Português (pt-BR)**, exibidas ao usuário via diálogos JavaFX.

---

## Escopo e limitações

**Incluído:** fundação do projeto (Parte 1); cadastro de produtos, fornecedores e categorias (Parte 2); base de controle de estoque; e frente de caixa (PDV) com finalização de venda, baixa de estoque, comprovante e histórico de vendas (Parte 4).

**Fora de escopo (não implementado nesta etapa):** fechamento/abertura de caixa, compras de fornecedores, relatórios gerenciais completos, integração com gateways de pagamento (cartão/PIX online), documentos fiscais e coleta de dados sensíveis de cartão. **Cancelamento de venda com estorno de estoque** não está disponível na interface nesta etapa (o modelo de dados já contempla o status `CANCELLED`, mas a operação fica para uma etapa futura, de forma explícita e auditável). O sistema foi projetado para que esses módulos sejam adicionados sem quebrar o que já existe.
