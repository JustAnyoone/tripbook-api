# TripBook

![CI](https://github.com/SEU_USUARIO/NOME_DO_REPO/actions/workflows/ci.yml/badge.svg?branch=portfolio)

API REST para gerenciamento de viagens, passageiros e destinos, desenvolvida com Spring Boot, Spring Data JPA e MySQL.

Projeto acadêmico do curso de Programação Orientada a Objetos (CEUB).

## Funcionalidades

- CRUD completo de passageiros, destinos e viagens
- Cada viagem liga um passageiro a um destino
- Regras de negócio: a data de volta não pode ser anterior à data de ida, e o passageiro e o destino informados precisam existir
- Validação dos dados de entrada, com mensagens de erro claras
- Tratamento global de exceções, com respostas de erro em formato padronizado
- Documentação interativa da API com Swagger UI

## Tecnologias

- Java 17
- Spring Boot 4 (Web, Data JPA, Validation)
- Hibernate / JPA
- MySQL 8
- springdoc-openapi (Swagger UI)
- Maven

## Arquitetura

O projeto é organizado em camadas, dentro do pacote `com.tt.tripbook`:

| Pacote       | Responsabilidade                                          |
|--------------|-----------------------------------------------------------|
| `controller` | Endpoints HTTP e documentação da API                      |
| `service`    | Regras de negócio                                         |
| `repository` | Acesso a dados com Spring Data JPA                        |
| `model`      | Entidades JPA e relacionamentos                           |
| `exception`  | Exceções personalizadas e tratamento global de erros      |

Fluxo de uma requisição:

```
Cliente -> Controller -> Service -> Repository -> Hibernate/JPA -> MySQL
```

## Modelo de dados

| Entidade     | Atributos principais                                   | Relacionamentos                                  |
|--------------|--------------------------------------------------------|--------------------------------------------------|
| `Passageiro` | nome, cpf (único), email, dataNascimento               | Um passageiro possui várias viagens              |
| `Destino`    | cidade, pais, descricao                                | Um destino é usado em várias viagens             |
| `Viagem`     | dataIda, dataVolta, valor, status                      | Pertence a um passageiro e a um destino          |

Detalhes de modelagem:

- A `Viagem` guarda as chaves estrangeiras `passageiro_id` e `destino_id` (`@ManyToOne` + `@JoinColumn`).
- Remover um passageiro remove também as viagens dele (`cascade` e `orphanRemoval`), pois a viagem não existe sem o passageiro.
- Um destino que possui viagens associadas não pode ser removido (erro 409).
- O campo `status` aceita apenas `PENDENTE`, `CONFIRMADA` ou `CANCELADA`.
- O valor da viagem usa `BigDecimal`, adequado para dinheiro.

## Como executar

### Pré-requisitos

- Java 17 ou superior
- MySQL 8 em execução na porta 3306

### Passo a passo

1. Clone o repositório:

```bash
git clone https://github.com/JustAnyoone/tripbook-api.git
cd tripbook-api
```

2. Defina a senha do MySQL. A aplicação lê a variável de ambiente `DB_PASSWORD`; se ela não existir, usa `root` como valor padrão.

```powershell
# Windows (PowerShell)
setx DB_PASSWORD "sua_senha"
```

```bash
# Linux / macOS
export DB_PASSWORD="sua_senha"
```

Depois de usar o `setx`, feche e abra o terminal (ou o VS Code) para a variável valer.

3. Execute a aplicação:

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell):

```powershell
.\mvnw spring-boot:run
```

O banco `tripbook` e as tabelas são criados automaticamente na primeira execução (`createDatabaseIfNotExist=true` e `ddl-auto=update`).

### Configuração

As configurações ficam em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tripbook?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:root}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

Se o seu MySQL usa outro usuário ou porta, ajuste essas linhas.

## Documentação da API (Swagger)

Com a aplicação em execução, acesse:

http://localhost:8080/swagger-ui/index.html

Por lá é possível ver todos os endpoints, os formatos de entrada e saída e testar as requisições direto no navegador. A especificação em JSON está em `http://localhost:8080/v3/api-docs`.

## Endpoints

| Recurso      | Rota base          | Operações                                 |
|--------------|--------------------|-------------------------------------------|
| Passageiros  | `/api/passageiros` | GET, GET por id, POST, PUT, DELETE        |
| Destinos     | `/api/destinos`    | GET, GET por id, POST, PUT, DELETE        |
| Viagens      | `/api/viagens`     | GET, GET por id, POST, PUT, DELETE        |

Consultas adicionais:

| Rota                                | Descrição                          |
|-------------------------------------|------------------------------------|
| `GET /api/viagens/passageiro/{id}`  | Lista as viagens de um passageiro  |
| `GET /api/viagens/destino/{id}`     | Lista as viagens para um destino   |

### Exemplos

Criar um destino (`POST /api/destinos`):

```json
{
  "cidade": "Lisboa",
  "pais": "Portugal",
  "descricao": "Capital portuguesa"
}
```

Criar um passageiro (`POST /api/passageiros`):

```json
{
  "nome": "Ana Souza",
  "cpf": "12345678901",
  "email": "ana@email.com",
  "dataNascimento": "1998-05-20"
}
```

Criar uma viagem (`POST /api/viagens`). Informe apenas o `id` do passageiro e do destino:

```json
{
  "dataIda": "2026-12-20",
  "dataVolta": "2026-12-30",
  "valor": 3500.50,
  "status": "CONFIRMADA",
  "passageiro": { "id": 1 },
  "destino": { "id": 1 }
}
```

## Tratamento de erros

Todos os erros seguem o mesmo formato:

```json
{
  "status": 404,
  "mensagem": "Destino não encontrado: id 999",
  "timestamp": "2026-10-09T21:00:00"
}
```

| Status | Quando acontece                                                                |
|--------|--------------------------------------------------------------------------------|
| 201    | Registro criado                                                                |
| 204    | Registro removido                                                              |
| 400    | Dados inválidos, status inexistente ou data de volta anterior à data de ida    |
| 404    | Registro, passageiro ou destino não encontrado                                 |
| 409    | CPF já cadastrado, ou tentativa de remover um destino que possui viagens       |

## Estrutura do projeto

```
src/main/java/com/tt/tripbook
  controller/   DestinoController, PassageiroController, ViagemController
  service/      DestinoService, PassageiroService, ViagemService
  repository/   DestinoRepository, PassageiroRepository, ViagemRepository
  model/        Destino, Passageiro, Viagem
  exception/    ApiError, ConflitoException, RecursoNaoEncontradoException,
                RegraDeNegocioException, GlobalExceptionHandler
src/main/resources
  application.properties
```

## Possíveis melhorias

- DTOs de entrada e saída, para não expor as entidades diretamente
- Paginação e filtros nas listagens
- `enum` para o status da viagem
- Testes automatizados (JUnit e Mockito)
- Autenticação e autorização

## Autor

Porttilho, estudante do CEUB.