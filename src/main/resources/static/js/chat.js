(function () {
    "use strict";

    const root = document.getElementById("chat-root");
    const currentUserId = Number(root.dataset.currentUserId);
    const peerId = Number(root.dataset.peerId);
    const peerName = root.dataset.peerName;
    const historyUrl = root.dataset.historyUrl;
    const suggestUrl = root.dataset.suggestUrl;
    const wsUrl = root.dataset.wsUrl;

    const chatWindow = document.getElementById("chat-window");
    const sendForm = document.getElementById("send-form");
    const messageInput = document.getElementById("message-input");
    const connectionStatus = document.getElementById("connection-status");

    const suggestBtn = document.getElementById("suggest-btn");
    const suggestionBox = document.getElementById("ai-suggestion-box");
    const suggestionText = document.getElementById("ai-suggestion-text");
    const useSuggestionBtn = document.getElementById("use-suggestion-btn");
    const dismissSuggestionBtn = document.getElementById("dismiss-suggestion-btn");

    let stompClient = null;

    function formatTime(iso) {
        const d = new Date(iso);
        return d.toLocaleString([], { hour: "2-digit", minute: "2-digit", month: "short", day: "numeric" });
    }

    function appendMessage(msg) {
        const mine = msg.senderId === currentUserId;
        const bubble = document.createElement("div");
        bubble.className = "message-bubble " + (mine ? "message-mine" : "message-theirs");

        const text = document.createElement("div");
        text.textContent = msg.content; // textContent evita qualsiasi injection di HTML/script
        bubble.appendChild(text);

        const meta = document.createElement("div");
        meta.className = "message-meta";
        meta.textContent = (mine ? "You" : msg.senderName) + " · " + formatTime(msg.sentAt);
        bubble.appendChild(meta);

        chatWindow.appendChild(bubble);
        chatWindow.scrollTop = chatWindow.scrollHeight;
    }

    // Carica la cronologia esistente via REST quando si apre la pagina della chat.
    function loadHistory() {
        fetch(historyUrl, { credentials: "same-origin" })
            .then((res) => {
                if (!res.ok) throw new Error("Failed to load history");
                return res.json();
            })
            .then((messages) => {
                chatWindow.innerHTML = "";
                messages.forEach(appendMessage);
            })
            .catch((err) => {
                console.error(err);
                chatWindow.innerHTML = '<p class="text-danger">Could not load message history.</p>';
            });
    }

    // Apre la connessione WebSocket (STOMP su SockJS) per ricevere i messaggi in tempo reale.
    function connect() {
        const socket = new SockJS(wsUrl);
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // silenzia i log dettagliati dei frame STOMP in console

        stompClient.connect(
            {},
            () => {
                connectionStatus.textContent = "Connected";
                stompClient.subscribe("/user/queue/messages", (frame) => {
                    const msg = JSON.parse(frame.body);
                    // La coda privata porta i messaggi di TUTTE le conversazioni dell'utente;
                    // mostriamo solo quelli che appartengono alla conversazione aperta ora.
                    const involvesPeer = msg.senderId === peerId || msg.receiverId === peerId;
                    if (involvesPeer) {
                        appendMessage(msg);
                    }
                });
                stompClient.subscribe("/user/queue/errors", (frame) => {
                    const err = JSON.parse(frame.body);
                    connectionStatus.textContent = "⚠️ " + err.error;
                });
            },
            (error) => {
                console.error("WebSocket error", error);
                connectionStatus.textContent = "Disconnected - retrying...";
                setTimeout(connect, 3000);
            }
        );
    }

    sendForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const content = messageInput.value.trim();
        if (!content) return;

        // Se il WebSocket non è connesso (es. si è disconnesso momentaneamente mentre
        // si aspettava il suggerimento IA), NON scartare in silenzio il messaggio: prima
        // il testo restava nell'input senza nessun avviso, dando l'impressione che il
        // messaggio "sparisse" quando poi la pagina veniva ricaricata. Ora lo segnaliamo
        // chiaramente e lasciamo il testo così com'è, pronto per un nuovo tentativo.
        if (!stompClient || !stompClient.connected) {
            connectionStatus.textContent = "⚠️ Not connected - message NOT sent, try again in a moment";
            return;
        }

        stompClient.send("/app/chat.send", {}, JSON.stringify({
            receiverId: peerId,
            content: content
        }));
        messageInput.value = "";
        suggestionBox.style.display = "none";
    });

    // Chiede all'IA un suggerimento di risposta basato sulla conversazione corrente.
    suggestBtn.addEventListener("click", () => {
        suggestBtn.disabled = true;
        suggestBtn.textContent = "Thinking...";
        fetch(suggestUrl, { credentials: "same-origin" })
            .then(async (res) => {
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || "AI suggestion failed");
                return data;
            })
            .then((data) => {
                suggestionText.textContent = data.suggestion;
                suggestionBox.style.display = "block";
            })
            .catch((err) => {
                suggestionText.textContent = "⚠️ " + err.message;
                suggestionBox.style.display = "block";
            })
            .finally(() => {
                suggestBtn.disabled = false;
                suggestBtn.textContent = "✨ Suggest";
            });
    });

    useSuggestionBtn.addEventListener("click", () => {
        messageInput.value = suggestionText.textContent;
        suggestionBox.style.display = "none";
        messageInput.focus();
    });

    dismissSuggestionBtn.addEventListener("click", () => {
        suggestionBox.style.display = "none";
    });

    loadHistory();
    connect();
})();
