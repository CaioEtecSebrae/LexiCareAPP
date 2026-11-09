# LexiCare — redesenho visual

Este projeto mantém a arquitetura Android/Kotlin original e aplica a identidade visual do protótipo navegável enviado.

## Mantido
- Login e cadastro locais com SharedPreferences/Gson
- Dashboard e navegação entre Activities
- Calendário e criação de tarefas
- Detalhes e exclusão de tarefas
- Exercícios e tela de resultado
- Apoio à caligrafia com Canvas
- Anotações
- Reconhecimento de voz
- Text-to-Speech
- Persistência local

## Redesenhado
- Paleta principal do protótipo: fundo creme `#FFF8E7`, azul `#2B6CB0`, verde `#38A169`
- Cards brancos arredondados
- Botões e campos com cantos arredondados
- Cabeçalhos mais limpos
- Dashboard com menu rápido em blocos coloridos
- Telas de exercícios, caligrafia, calendário, anotações e detalhes alinhadas ao protótipo
- Componentes de tarefas/anotações com visual mais próximo do protótipo

## Observação
O projeto foi validado estruturalmente (XMLs e referências de recursos/IDs). O ambiente de execução usado para preparar o pacote não possui a distribuição Gradle 9.1.0 em cache e não tem acesso à internet, portanto o `assembleDebug` não pôde ser executado aqui.

No Android Studio, abra a pasta `LexiCare` e deixe o Gradle sincronizar normalmente.
