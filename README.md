# Indayá Locações – Vistoria Android

Aplicativo Android offline para gestão de vistorias de veículos alugados.

## Recursos
- PIN de acesso (inicial 1234, alterável no app)
- Cadastro, edição e exclusão de veículos
- Cadastro, edição e exclusão de locatários
- Retirada de veículo com fotos, km, combustível e avarias
- Devolução vinculada à retirada com comparação de dados e geração de manutenção
- Vistoria semanal por veículo, com pendências automáticas por semana
- Checklist completo: pneus, óleo, água/aditivo, luzes, limpadores, vidros/retrovisores, lataria, interior, documentos, estepe/macaco/chave de roda
- Fotos pela câmera do celular
- Assinatura na tela
- Status: Aprovado / Precisa de atenção / Não liberar
- Manutenção e histórico
- Relatórios
- Exportação/importpãão de backup JSON
- Compartilhamento de laudo
- Impressão / Salvar como PDF usando o sistema de impressão do Android
- Funciona offline e salva os dados no próprio aparelho

## Gerar APK
O workflow `.github/workflows/build-apk.yml` compila automaticamente o APK no GitHub Actions.

1. Envie o projeto para um repositório GitHub.
2. Abra **Actions > Build APK**.
3. Execute o workflow ou faça um push na branch `main`.
4. Baixe o artefato `indaya-vistoria-apk`.

O APK gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.
