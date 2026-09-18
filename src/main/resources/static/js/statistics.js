(function () {
    "use strict";

    const root = document.getElementById("stats-root");
    const emailUrl = root.dataset.emailUrl;
    const btn = document.getElementById("email-report-btn");
    const status = document.getElementById("email-status");

    function getCsrfToken() {
        // Se i meta tag del CSRF non ci sono (protezione CSRF disabilitata), non inviamo
        // nessun header - il server lo ignorerà comunque.
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        if (!tokenMeta || !headerMeta) return null;
        return { header: headerMeta.content, token: tokenMeta.content };
    }

    btn.addEventListener("click", () => {
        btn.disabled = true;
        status.textContent = "Sending...";

        const headers = { "Content-Type": "application/json" };
        const csrf = getCsrfToken();
        if (csrf) headers[csrf.header] = csrf.token;

        fetch(emailUrl, { method: "POST", credentials: "same-origin", headers })
            .then(async (res) => {
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || "Could not send the email");
                return data;
            })
            .then((data) => {
                status.textContent = "✅ Report sent to " + data.email;
            })
            .catch((err) => {
                status.textContent = "⚠️ " + err.message;
            })
            .finally(() => {
                btn.disabled = false;
            });
    });
})();
