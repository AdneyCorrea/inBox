# Dependências

- Vue 3.5.22: distribuição local de produção com compilador, em `backend/src/main/resources/static/vendor/vue.global.prod.js`. Licença MIT e copyright preservados no cabeçalho. Origem: https://unpkg.com/vue@3.5.22/dist/vue.global.prod.js. Projeto: https://github.com/vuejs/core.
- Spring Boot 3.5.16: dependências resolvidas pelo Maven conforme `backend/pom.xml`. Projeto: https://spring.io/projects/spring-boot. Licença Apache 2.0 para Spring Boot.
- H2: versão gerenciada pelo Spring Boot. Licença MPL 2.0 ou EPL 1.0. Projeto: https://h2database.com.
- Apache Maven 3.9.11: ferramenta local em `backend/scripts/tools/`, fora dos fontes versionáveis. Origem: https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip. Licença Apache 2.0.

O frontend não carrega recursos de CDN durante o uso normal.

- Spring Security: versão gerenciada pelo Spring Boot, licença Apache 2.0. https://spring.io/projects/spring-security
- PostgreSQL JDBC: versão gerenciada pelo Spring Boot, licença BSD-2-Clause. https://jdbc.postgresql.org/
- Eclipse Temurin e Maven: imagens oficiais usadas no Dockerfile para compilação e execução. https://adoptium.net/
