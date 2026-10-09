# FastEasy — Sistema de Gestão de Supermercado

Aplicação **desktop** para gestão de supermercado, construída com **Java 21 + JavaFX + Spring Boot + PostgreSQL**.
Este repositório contém a **Parte 1** (fundação do projeto) e a **Parte 2** (módulo de cadastro de **Produtos**, **Fornecedores** e **Categorias**).

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
├── domain           → Entidades JPA (Product, Supplier, Category, UnidadeMedida)
├── repository       → Repositórios Spring Data JPA
├── service          → Regras de negócio (ProductService, SupplierService, CategoryService)
├── validator        → Validadores puros (CPF/CNPJ e código de barras EAN)
└── presentation     → JavaFX: controladores e views (formulários e tabelas)
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

Na primeira execução, o Flyway aplica a migração que cria as tabelas de categorias, fornecedores e produtos.

A janela principal possui um **menu lateral verde** com as seções **Produtos**, **Categorias** e **Fornecedores**, uma **área principal** com os formulários/tabelas e uma **barra de status**.

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

### Integridade no banco de dados

Além da validação em Java, a migração define restrições no banco:

- `CHECK (preco_venda > 0)`
- `CHECK (custo_aquisicao IS NULL OR custo_aquisicao >= 0)`
- `CHECK (estoque_minimo >= 0)`
- `CHECK (estoque_maximo IS NULL OR estoque_maximo >= estoque_minimo)`
- `CHECK (status IN ('ATIVO','INATIVO'))`
- Índices únicos parciais para `sku` e `codigo_barras` (quando não nulos)
- Chaves estrangeiras de produto → categoria e produto → fornecedor principal

---

## Migrações Flyway

As migrações ficam em `src/main/resources/db/migration`:

- `V1.0.1__create_product_tables.sql` — tabelas de categorias, fornecedores e produtos, índices e restrições.

O Flyway roda automaticamente na inicialização da aplicação (configurável em `application.properties`).

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

**Incluído:** fundação do projeto (Parte 1) e cadastro de produtos, fornecedores e categorias (Parte 2).

**Fora de escopo (não implementado):** PDV, caixa, controle de estoque completo e módulos de vendas. A quantidade em estoque será controlada por um módulo futuro. O sistema foi projetado para que esses módulos sejam adicionados sem quebrar o que já existe.
