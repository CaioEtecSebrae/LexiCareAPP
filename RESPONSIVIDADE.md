# LexiCare — responsividade

## Como funciona
| Necessidade | Solução |
|---|---|
| Tablet / TV / desktop / janela larga: ocupar a tela inteira | `MaxWidthLinearLayout` sem limite prático (`@dimen/content_max_width` = 4000dp). Só Login/Cadastro usam coluna central (`app:larguraMaxima="@dimen/form_max_width"`) |
| Textos maiores em telas grandes e TV | `@dimen/ts_NN` em `values`, `values-sw600dp`, `values-sw840dp`, `values-television` |
| Menu do Dashboard: 2 colunas no celular, 4 em tela larga | `GridLayout` + `@integer/menu_colunas` (`values-w600dp`) |
| Celular deitado / tela larga | `layout-land/` para Calendário e Caligrafia (dois painéis) |
| Fonte grande (acessibilidade) / telas baixas | alturas fixas viraram `minHeight`; telas com rolagem |
| Barras do sistema, notch, teclado | `BaseActivity` (edge-to-edge + insets). Cabeçalhos usam `@+id/headerTopo` |
| TV / teclado / mouse | anel de foco (`foco_anel.xml`), manifest com `touchscreen`/`microphone` opcionais, `LEANBACK_LAUNCHER` e banner |
| Janela redimensionável (ChromeOS, multi-janela) | `resizeableActivity=true`; Exercícios e Caligrafia guardam o estado ao recriar |

## Ao criar uma tela nova
1. Herde de `BaseActivity` (não de `AppCompatActivity`).
2. Corpo da tela: `MaxWidthLinearLayout` com `android:layout_gravity="center_horizontal"`.
3. Cabeçalho colorido: `FrameLayout` com `@+id/headerTopo`.
4. Use `@dimen/ts_NN` para textos e `minHeight` (não `layout_height` fixo) em botões e campos.
5. Teste: emulador pequeno (320dp), Pixel, tablet, TV e "Resizable (Experimental)" em paisagem.

## Campos de texto longos (Cadastro)
Nome, série e e-mail não têm limite de caracteres. O XML do campo é `textMultiLine` e `CampoTexto.quebraAutomatica(...)`
tira a rolagem horizontal, troca o Enter por "Próximo" e bloqueia quebras de linha digitadas. Assim o texto quebra e
o campo cresce para baixo; a borda acompanha. Use o mesmo padrão em qualquer campo novo que possa receber texto longo.

## Notificações
`Notificacoes.kt` concentra canais, agendamento e utilidades. Lembrete diário: `LembreteDiarioReceiver` (1 por dia, só se
a pessoa ainda não praticou). Horário em `Notificacoes.HORA_LEMBRETE`. Tarefas do calendário: `LembreteReceiver`.
