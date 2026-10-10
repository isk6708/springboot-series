const API_URL = window.NIISE_API_URL || "http://localhost:8005/api/v1";

document.addEventListener("DOMContentLoaded", () => {
    initConsole();
});

function initConsole() {
    bindConsoleEvents();
    refreshConsoleAuth();
    searchPublicUsers();
}

function bindConsoleEvents() {
    document.getElementById("login-form").addEventListener("submit", loginConsole);
    document.getElementById("register-form").addEventListener("submit", registerConsole);
    document.getElementById("public-search-form").addEventListener("submit", searchPublicUsers);
    document.getElementById("passport-search-form").addEventListener("submit", loadConsolePassports);
    document.getElementById("passport-form").addEventListener("submit", saveConsolePassport);
    document.getElementById("pengguna-admin-search-form").addEventListener("submit", loadConsolePengguna);
    document.getElementById("pengguna-admin-form").addEventListener("submit", saveConsolePengguna);
    document.getElementById("logout-button").addEventListener("click", logoutConsole);
    document.getElementById("passport-cancel").addEventListener("click", () => clearConsoleForm("passport"));
    document.getElementById("pengguna-admin-cancel").addEventListener("click", () => clearConsoleForm("pengguna-admin"));
    document.querySelectorAll("[data-view]").forEach((button) => {
        button.addEventListener("click", () => showConsoleView(button.dataset.view));
    });
}

function consoleToken() {
    return localStorage.getItem("niise-token");
}

function consoleTokenType() {
    return localStorage.getItem("niise-token-type") || "Bearer";
}

function refreshConsoleAuth(user) {
    const signedIn = Boolean(consoleToken());
    document.getElementById("protected-menu").hidden = !signedIn;
    document.getElementById("protected-content").hidden = !signedIn;
    document.getElementById("login-panel").hidden = signedIn;
    document.getElementById("session-label").textContent = signedIn
        ? `Signed in${user?.email ? ` as ${user.email}` : ""}`
        : "Not signed in";
    if (signedIn) {
        showConsoleView("passports");
        loadConsolePassports();
        loadConsolePengguna();
    }
}

function showConsoleView(view) {
    document.querySelectorAll("[data-panel]").forEach((panel) => {
        panel.hidden = panel.dataset.panel !== view;
    });
    document.querySelectorAll("[data-view]").forEach((button) => {
        button.setAttribute("aria-current", button.dataset.view === view ? "page" : "false");
    });
}

async function consoleRequest(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body !== undefined) headers.set("Content-Type", "application/json");
    if (consoleToken() && (path.startsWith("/passports") || path.startsWith("/pengguna/admin"))) {
        headers.set("Authorization", `${consoleTokenType()} ${consoleToken()}`);
    }
    let response;
    try {
        response = await fetch(`${API_URL}${path}`, { ...options, headers });
    } catch {
        throw new Error("Cannot reach the API. Confirm Spring Boot is running on port 8005.");
    }
    const type = response.headers.get("content-type") || "";
    const data = response.status === 204 ? null : type.includes("application/json")
        ? await response.json()
        : await response.text();
    if (!response.ok) {
        if ((response.status === 401 || response.status === 403) && consoleToken()) {
            localStorage.removeItem("niise-token");
            localStorage.removeItem("niise-token-type");
            refreshConsoleAuth();
        }
        throw new Error(typeof data === "string" ? data : data?.message || `Request failed (${response.status})`);
    }
    return data;
}

async function loginConsole(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = new FormData(form);
    try {
        const result = await consoleRequest("/login", {
            method: "POST",
            body: JSON.stringify({ email: values.get("email"), password: values.get("password") })
        });
        if (!result?.token) throw new Error("Login response did not include a token.");
        localStorage.setItem("niise-token", result.token);
        localStorage.setItem("niise-token-type", result.type || "Bearer");
        form.reset();
        refreshConsoleAuth(result.user);
        showConsoleMessage("login-message", "Signed in successfully.", "success");
    } catch (error) {
        showConsoleMessage("login-message", error.message, "error");
    }
}

async function registerConsole(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = new FormData(form);
    const payload = Object.fromEntries(["nama", "icno", "email", "password"].map((key) => [key, values.get(key)]));
    try {
        await consoleRequest("/pengguna/register", { method: "POST", body: JSON.stringify(payload) });
        form.reset();
        showConsoleMessage("register-message", "Account registered. You can now sign in.", "success");
    } catch (error) {
        showConsoleMessage("register-message", error.message, "error");
    }
}

async function searchPublicUsers(event) {
    if (event) event.preventDefault();
    const query = new URLSearchParams(new FormData(document.getElementById("public-search-form")));
    try {
        const rows = await consoleRequest(`/pengguna${query.size ? `?${query}` : ""}`);
        renderConsoleRows("public-pengguna-results", rows, ["nama", "email", "icno"]);
        showConsoleMessage("public-search-message", `${rows.length} user record(s) found.`, "success");
    } catch (error) {
        showConsoleMessage("public-search-message", error.message, "error");
    }
}

async function loadConsolePassports(event) {
    if (event) event.preventDefault();
    if (!consoleToken()) return;
    const query = new URLSearchParams(new FormData(document.getElementById("passport-search-form")));
    try {
        const rows = await consoleRequest(`/passports/admin${query.size ? `?${query}` : ""}`);
        renderConsoleRows("passport-results", rows, ["id", "fullname", "icno"], passportRowActions);
        showConsoleMessage("passport-message", `${rows.length} passport record(s) found.`, "success");
    } catch (error) {
        showConsoleMessage("passport-message", error.message, "error");
    }
}

async function saveConsolePassport(event) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const id = values.get("id");
    const payload = { fullname: values.get("fullname"), icno: values.get("icno") };
    if (id) payload.id = Number(id);
    try {
        await consoleRequest(id ? "/passports/admin" : "/passports", {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(payload)
        });
        clearConsoleForm("passport");
        showConsoleMessage("passport-message", id ? "Passport updated." : "Passport created.", "success");
        loadConsolePassports();
    } catch (error) {
        showConsoleMessage("passport-message", error.message, "error");
    }
}

async function loadConsolePengguna(event) {
    if (event) event.preventDefault();
    if (!consoleToken()) return;
    const query = new URLSearchParams(new FormData(document.getElementById("pengguna-admin-search-form")));
    try {
        const rows = await consoleRequest(`/pengguna/admin${query.size ? `?${query}` : ""}`);
        renderConsoleRows("pengguna-admin-results", rows, ["id", "nama", "email", "icno"], penggunaRowActions);
        showConsoleMessage("pengguna-admin-message", `${rows.length} user record(s) found.`, "success");
    } catch (error) {
        showConsoleMessage("pengguna-admin-message", error.message, "error");
    }
}

async function saveConsolePengguna(event) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const payload = {
        id: Number(values.get("id")),
        nama: values.get("nama"),
        icno: values.get("icno"),
        email: values.get("email"),
        password: values.get("password")
    };
    try {
        await consoleRequest("/pengguna/admin", { method: "PUT", body: JSON.stringify(payload) });
        clearConsoleForm("pengguna-admin");
        showConsoleMessage("pengguna-admin-message", "User updated.", "success");
        loadConsolePengguna();
    } catch (error) {
        showConsoleMessage("pengguna-admin-message", error.message, "error");
    }
}

function renderConsoleRows(targetId, rows, fields, actionFactory) {
    const body = document.getElementById(targetId);
    body.replaceChildren();
    rows.forEach((row) => {
        const tr = document.createElement("tr");
        fields.forEach((field) => {
            const cell = document.createElement("td");
            cell.textContent = row[field] ?? "";
            tr.appendChild(cell);
        });
        if (actionFactory) {
            const cell = document.createElement("td");
            cell.className = "row-actions";
            cell.append(...actionFactory(row));
            tr.appendChild(cell);
        }
        body.appendChild(tr);
    });
}

function passportRowActions(row) {
    return [
        consoleAction("Edit", () => fillConsoleForm("passport", row)),
        consoleAction("Delete", () => deleteConsoleRecord(`/passports/admin/${row.id}`, "passport"), "button-danger")
    ];
}

function penggunaRowActions(row) {
    return [
        consoleAction("Edit", async () => {
            try {
                const record = await consoleRequest(`/pengguna/admin/${row.id}`);
                fillConsoleForm("pengguna-admin", record);
                showConsoleMessage("pengguna-admin-message", "Set a new password before saving this user.", "success");
            } catch (error) {
                showConsoleMessage("pengguna-admin-message", error.message, "error");
            }
        }),
        consoleAction("Delete", () => deleteConsoleRecord(`/pengguna/admin/${row.id}`, "pengguna-admin"), "button-danger")
    ];
}

function consoleAction(label, action, className = "") {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = label;
    button.className = className;
    button.addEventListener("click", action);
    return button;
}

async function deleteConsoleRecord(path, kind) {
    if (!window.confirm("Permanently delete this record?")) return;
    try {
        await consoleRequest(path, { method: "DELETE" });
        showConsoleMessage(`${kind}-message`, "Record deleted.", "success");
        if (kind === "passport") loadConsolePassports();
        else loadConsolePengguna();
    } catch (error) {
        showConsoleMessage(`${kind}-message`, error.message, "error");
    }
}

function fillConsoleForm(prefix, record) {
    const form = document.getElementById(`${prefix}-form`);
    Object.entries(record).forEach(([key, value]) => {
        const field = form.elements.namedItem(key);
        if (field && key !== "password") field.value = value ?? "";
    });
    form.scrollIntoView({ behavior: "smooth", block: "center" });
}

function clearConsoleForm(prefix) {
    const form = document.getElementById(`${prefix}-form`);
    form.reset();
    form.elements.namedItem("id").value = "";
}

function showConsoleMessage(id, message, kind = "") {
    const element = document.getElementById(id);
    element.textContent = message;
    element.className = `message${kind ? ` message-${kind}` : ""}`;
    element.hidden = !message;
}

function logoutConsole() {
    localStorage.removeItem("niise-token");
    localStorage.removeItem("niise-token-type");
    document.querySelectorAll("[data-panel]").forEach((panel) => { panel.hidden = true; });
    refreshConsoleAuth();
}
