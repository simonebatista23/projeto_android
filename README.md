# 📱 Aplicativo Android — Sistema de Chamados (Java)

Este é o aplicativo Android desenvolvido em **Java**, que se conecta à API de Chamados Internos. Ele permite que o usuário faça login, abra novos chamados e visualize todos os chamados que registrou. O app foi criado para fornecer uma interface simples e funcional, totalmente integrada à API.

---

## 🚀 Tecnologias Utilizadas
- **Linguagem:** Java  
- **Plataforma:** Android (Android Studio)  
- **Layout:** XML  
- **Requisições HTTP:** Retrofit / HttpURLConnection  
- **Autenticação:** JWT  
- **Navegação:** Activities e Intents  

---

## ⚙️ Funcionalidades do Aplicativo

| Funcionalidade | Descrição |
|----------------|-----------|
| **Login** | Autentica o usuário na API e armazena o token JWT localmente. |
| **Abrir Chamado** | Usuário pode registrar um novo chamado informando os dados necessários. |
| **Listagem de Chamados** | Exibe todos os chamados criados pelo usuário logado. |
| **Integração com API** | Todas as ações são realizadas através de requisições HTTP para a API de Chamados Internos. |

---

## ▶️ Como Executar o Projeto

1. Abra o projeto no **Android Studio**  
2. Configure e execute em um dispositivo físico ou emulador  
3. Certifique-se de que a API está rodando corretamente  
4. O app consumirá automaticamente os endpoints necessários

---

## 📡 Comunicação com a API
O app realiza chamadas HTTP autenticadas para:

- Login  
- Abertura de chamados  
- Listagem dos chamados do usuário  

---

## 📝 Observação
O projeto foi desenvolvido com foco em simplicidade, integração com API e navegação fluida entre as telas principais do sistema.
