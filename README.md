# FastEasy — Sistema de Gestão de Supermercado

Repositório do sistema desktop **FastEasy**. O código-fonte da aplicação Maven está em [`fasteasy/`](fasteasy/).

## Estrutura

```
FastEasy-sistemaMKT/
└── fasteasy/                 → projeto Maven (Java 21 + JavaFX + Spring Boot + PostgreSQL)
    ├── README.md             → documentação completa
    ├── pom.xml
    ├── .env.example
    └── src/
        ├── main/
        │   ├── java/         → código da aplicação
        │   └── resources/    → configuração, FXML, CSS e migrações Flyway
        └── test/             → testes JUnit 5 / Mockito / H2
```

## Documentação

Consulte **[fasteasy/README.md](fasteasy/README.md)** para:

- tecnologias e arquitetura em camadas;
- pré-requisitos e configuração do PostgreSQL;
- variáveis de ambiente (`.env.example`);
- como executar (`mvn javafx:run`) e como rodar os testes;
- regras de negócio do cadastro de produtos, fornecedores e categorias.

## Sincronizar com o GitHub

O repositório remoto é `https://github.com/victorGAPsilva/FastEasy-sistemaMKT`.

```bash
./scripts/sync-github.sh "sua mensagem de commit"
```

O script adiciona as alterações, cria o commit (se houver mudanças) e envia para o GitHub no branch `main`.
