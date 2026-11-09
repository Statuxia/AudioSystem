// Минимальный STOMP-over-WebSocket клиент (без внешних зависимостей).
// Поддерживает ровно то, что нужно для AudioSystem: CONNECT/CONNECTED, SUBSCRIBE, MESSAGE, ERROR.
class MiniStomp {
    constructor(wsUrl) {
        this.wsUrl = wsUrl;
        this.ws = null;
        this.connected = false;
        this.subscriptions = new Map(); // destination -> {id, callback}
        this.nextSubId = 1;
        this.onStatusChange = () => {};
    }

    connect() {
        this.ws = new WebSocket(this.wsUrl);
        this.ws.onopen = () => {
            this._send('CONNECT', { 'accept-version': '1.1,1.2', 'heart-beat': '0,0' });
        };
        this.ws.onclose = () => {
            this.connected = false;
            this.onStatusChange('closed');
        };
        this.ws.onerror = () => {
            this.onStatusChange('error');
        };
        this.ws.onmessage = (event) => this._handleRaw(event.data);
    }

    disconnect() {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this._send('DISCONNECT', {});
            this.ws.close();
        }
        this.connected = false;
        this.subscriptions.clear();
    }

    subscribe(destination, callback) {
        const id = 'sub-' + (this.nextSubId++);
        this.subscriptions.set(destination, { id, callback });
        if (this.connected) {
            this._send('SUBSCRIBE', { id, destination });
        }
        return id;
    }

    unsubscribe(destination) {
        const sub = this.subscriptions.get(destination);
        if (!sub) return;
        if (this.connected) {
            this._send('UNSUBSCRIBE', { id: sub.id });
        }
        this.subscriptions.delete(destination);
    }

    _resubscribeAll() {
        for (const [destination, sub] of this.subscriptions.entries()) {
            this._send('SUBSCRIBE', { id: sub.id, destination });
        }
    }

    _send(command, headers, body = '') {
        const lines = [command];
        for (const [k, v] of Object.entries(headers)) {
            lines.push(`${k}:${v}`);
        }
        lines.push('', body);
        this.ws.send(lines.join('\n') + '\0');
    }

    _handleRaw(raw) {
        raw.split('\0').forEach((chunk) => {
            const text = chunk.replace(/^\n+/, '');
            if (!text) return;
            const frame = this._parseFrame(text);
            this._handleFrame(frame);
        });
    }

    _parseFrame(text) {
        const lines = text.split('\n');
        const command = lines[0];
        const headers = {};
        let i = 1;
        for (; i < lines.length; i++) {
            const line = lines[i];
            if (line === '') {
                i++;
                break;
            }
            const idx = line.indexOf(':');
            if (idx > -1) headers[line.substring(0, idx)] = line.substring(idx + 1);
        }
        const body = lines.slice(i).join('\n');
        return { command, headers, body };
    }

    _handleFrame(frame) {
        switch (frame.command) {
            case 'CONNECTED':
                this.connected = true;
                this.onStatusChange('connected');
                this._resubscribeAll();
                break;
            case 'MESSAGE': {
                // STOMP 1.2: MESSAGE связывается с подпиской по заголовку subscription (= id из SUBSCRIBE),
                // а не по destination — при подписке по шаблону destination у сообщения конкретный
                const subscriptionId = frame.headers['subscription'];
                const sub = [...this.subscriptions.values()].find((s) => s.id === subscriptionId);
                if (sub) {
                    let payload = frame.body;
                    try {
                        payload = JSON.parse(frame.body);
                    } catch (e) {
                        // оставляем как строку, если не JSON
                    }
                    sub.callback(payload);
                }
                break;
            }
            case 'ERROR':
                this.onStatusChange('error', frame.body);
                break;
        }
    }
}

// ---- UI ----

const $ = (id) => document.getElementById(id);

// Клиент раздаётся nginx'ом — относительные пути работают сами;
// file://-открытие fallback'ится на локальный стек
function baseUrl() {
    return location.protocol.startsWith('http') ? location.origin : 'http://localhost:8080';
}

function wsUrl() {
    return baseUrl().replace(/^http/, 'ws') + '/ws';
}

const logEl = $('log');

function log(message, kind = '') {
    const entry = document.createElement('div');
    entry.className = 'log-entry' + (kind ? ' log-' + kind : '');
    const time = new Date().toLocaleTimeString();
    entry.innerHTML = `<span class="log-time">${time}</span>${message}`;
    logEl.appendChild(entry);
    logEl.scrollTop = logEl.scrollHeight;
}

// ---- toasts ----

const toastsEl = $('toasts');

function toast(message, kind = 'info', sticky = kind === 'error') {
    const el = document.createElement('div');
    el.className = `toast toast-${kind}`;
    el.textContent = message;

    const close = document.createElement('button');
    close.type = 'button';
    close.className = 'toast-close';
    close.textContent = '×';
    close.setAttribute('aria-label', 'Закрыть');
    close.addEventListener('click', () => el.remove());
    el.appendChild(close);

    toastsEl.appendChild(el);
    if (!sticky) {
        setTimeout(() => el.remove(), 6000);
    }
}

// ---- dropzone ----

const dropzone = $('dropzone');
const fileInput = $('fileInput');
const fileChip = $('fileChip');
let selectedFile = null;

function setSelectedFile(file) {
    selectedFile = file;
    if (file) {
        $('fileChipName').textContent = file.name;
        $('fileChipSize').textContent = formatSize(file.size);
        fileChip.hidden = false;
        dropzone.querySelector('.dropzone-placeholder').hidden = true;
        dropzone.classList.add('has-file');
    } else {
        fileChip.hidden = true;
        dropzone.querySelector('.dropzone-placeholder').hidden = false;
        dropzone.classList.remove('has-file');
        fileInput.value = '';
    }
}

function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' Б';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' КБ';
    return (bytes / 1024 / 1024).toFixed(1) + ' МБ';
}

dropzone.addEventListener('click', (e) => {
    if (!selectedFile) fileInput.click();
});
dropzone.addEventListener('keydown', (e) => {
    if ((e.key === 'Enter' || e.key === ' ') && !selectedFile) {
        e.preventDefault();
        fileInput.click();
    }
});
fileInput.addEventListener('change', () => {
    if (fileInput.files[0]) setSelectedFile(fileInput.files[0]);
});
$('fileChipRemove').addEventListener('click', (e) => {
    e.stopPropagation();
    setSelectedFile(null);
});

['dragenter', 'dragover'].forEach((event) =>
    dropzone.addEventListener(event, (e) => {
        e.preventDefault();
        dropzone.classList.add('dragover');
    })
);
['dragleave', 'drop'].forEach((event) =>
    dropzone.addEventListener(event, (e) => {
        e.preventDefault();
        dropzone.classList.remove('dragover');
    })
);
dropzone.addEventListener('drop', (e) => {
    const file = e.dataTransfer.files[0];
    if (file) setSelectedFile(file);
});

// ---- WebSocket ----

let stomp = null;
let currentJobId = null;
const wsStatusEl = $('wsStatus');

function setWsBadge(text, cls) {
    wsStatusEl.textContent = 'WS: ' + text;
    wsStatusEl.className = 'badge ' + cls;
}

function ensureStompConnected() {
    // CONNECTING: subscriptions are queued and sent by _resubscribeAll after CONNECTED
    if (stomp && (stomp.connected || stomp.ws?.readyState === WebSocket.CONNECTING)) return;
    if (stomp) stomp.disconnect();
    stomp = new MiniStomp(wsUrl());
    setWsBadge('подключение...', 'badge-neutral');
    stomp.onStatusChange = (status, detail) => {
        if (status === 'connected') {
            setWsBadge('подключено', 'badge-done');
            log('WebSocket подключён (' + wsUrl() + ')', 'ok');
        } else if (status === 'closed') {
            setWsBadge('отключено', 'badge-neutral');
            log('WebSocket отключён');
            toast('WebSocket отключён', 'info');
        } else if (status === 'error') {
            setWsBadge('ошибка', 'badge-error');
            log('WebSocket ошибка: ' + (detail || ''), 'error');
            toast('Ошибка WebSocket: ' + (detail || ''), 'error');
        }
    };
    stomp.connect();
}

function subscribeToJob(jobId) {
    ensureStompConnected();
    stomp.subscribe(`/topic/job/${jobId}`, (payload) => {
        log(`job/${payload.jobId ?? jobId} → status=${payload.status}`, payload.status === 'ERROR' ? 'error' : 'ok');
        renderJobState(payload);
    });
    stomp.subscribe(`/topic/job/${jobId}/queue`, (payload) => {
        log(`job/${payload.jobId ?? jobId}/queue → позиция ${payload.queuePosition}`);
        $('queueInfo').textContent = `очередь: ${payload.queuePosition}`;
    });
}

function renderJobState(state) {
    const badge = $('statusBadge');
    badge.textContent = state.status;
    badge.className = 'badge ' + (
        state.status === 'DONE' ? 'badge-done' :
        state.status === 'ERROR' ? 'badge-error' : 'badge-queue'
    );
    if (state.expireAt) {
        $('expireInfo').textContent = 'истекает: ' + new Date(state.expireAt).toLocaleString();
    }
    if (state.status === 'DONE') {
        $('queueInfo').textContent = '';
        addResultCard(state.jobId ?? currentJobId);
        toast('Задача ' + shortId(state.jobId ?? currentJobId) + ' завершена — результат готов к скачиванию', 'ok');
    } else if (state.status === 'ERROR') {
        toast('Задача ' + shortId(state.jobId ?? currentJobId) + ' завершилась с ошибкой', 'error');
    }
}

function resetJobPanel() {
    $('statusBadge').textContent = 'нет задачи';
    $('statusBadge').className = 'badge badge-neutral';
    $('queueInfo').textContent = '';
    $('expireInfo').textContent = '';
}

function shortId(id) {
    return id ? id.slice(0, 8) : '';
}

// ---- результаты ----

// Метаданные задач, созданных в этой сессии: jobId -> {fileName, settings}
// После перезагрузки страницы или при чужом ID карточка опирается только на UUID
const knownJobs = new Map();
const resultsEl = $('results');

function addResultCard(jobId) {
    if (!jobId || resultsEl.querySelector(`[data-job-id="${jobId}"]`)) return;

    const known = knownJobs.get(jobId);
    const title = known ? known.fileName : 'Задача ' + shortId(jobId);
    const sub = known
        ? `${known.settings.format} · ${known.settings.speed}x · ${known.settings.pitchSemitones} st · ${shortId(jobId)}`
        : 'UUID: ' + jobId;

    $('resultsEmpty').hidden = true;

    const card = document.createElement('div');
    card.className = 'result-item';
    card.dataset.jobId = jobId;

    const info = document.createElement('div');
    info.className = 'result-info';
    info.innerHTML = `<div class="result-name"></div><div class="result-sub"></div>`;
    info.querySelector('.result-name').textContent = title;
    info.querySelector('.result-sub').textContent = sub;

    const download = document.createElement('a');
    download.className = 'result-download';
    download.href = `${baseUrl()}/api/v1/job/${jobId}/download`;
    download.target = '_blank';
    download.rel = 'noopener';
    download.textContent = 'Скачать';

    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'result-remove';
    remove.textContent = '×';
    remove.setAttribute('aria-label', 'Убрать из списка');
    remove.addEventListener('click', () => {
        card.remove();
        if (!resultsEl.querySelector('.result-item')) {
            $('resultsEmpty').hidden = false;
        }
    });

    card.append(info, download, remove);
    resultsEl.appendChild(card);
}

$('clearResultsBtn').addEventListener('click', () => {
    resultsEl.querySelectorAll('.result-item').forEach((el) => el.remove());
    $('resultsEmpty').hidden = false;
});

// ---- пресеты ----

async function loadPresets() {
    try {
        const res = await fetch(`${baseUrl()}/api/v1/job/presets`);
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const presets = await res.json();
        const container = $('presetButtons');
        container.innerHTML = '';
        presets.forEach((preset) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.textContent = `${preset.mode} (${preset.speed}x, ${preset.pitchSemitones}st)`;
            btn.addEventListener('click', () => {
                $('speed').value = preset.speed;
                $('pitch').value = preset.pitchSemitones;
            });
            container.appendChild(btn);
        });
        log(`загружено пресетов: ${presets.length}`, 'ok');
    } catch (e) {
        log('не удалось загрузить пресеты: ' + e.message, 'error');
        toast('Не удалось загрузить пресеты', 'error');
    }
}

loadPresets();

// --- Создание задачи ---

$('jobForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const file = selectedFile;
    if (!file) {
        toast('Сначала выберите аудиофайл', 'error');
        return;
    }

    const settings = {
        format: $('format').value,
        speed: parseFloat($('speed').value),
        pitchSemitones: parseFloat($('pitch').value),
    };

    const formData = new FormData();
    formData.append('file', file);
    formData.append('settings', new Blob([JSON.stringify(settings)], { type: 'application/json' }));

    log(`создаю задачу: ${file.name}, ${JSON.stringify(settings)}`);
    try {
        const res = await fetch(`${baseUrl()}/api/v1/job`, { method: 'POST', body: formData });
        const body = await res.json();
        if (!res.ok) {
            log(`ошибка создания задачи (HTTP ${res.status}): ${JSON.stringify(body)}`, 'error');
            toast('Не удалось создать задачу (HTTP ' + res.status + ')', 'error');
            return;
        }
        currentJobId = body.jobId;
        knownJobs.set(currentJobId, { fileName: file.name, settings });
        $('jobIdInput').value = currentJobId;
        resetJobPanel();
        log(`задача создана: ${currentJobId}`, 'ok');
        toast('Задача ' + shortId(currentJobId) + ' создана', 'ok');
        subscribeToJob(currentJobId);
    } catch (e) {
        log('ошибка запроса: ' + e.message, 'error');
        toast('Ошибка запроса: ' + e.message, 'error');
    }
});

// --- Отслеживание существующей задачи по ID ---

$('trackBtn').addEventListener('click', () => {
    const jobId = $('jobIdInput').value.trim();
    if (!jobId) return;
    currentJobId = jobId;
    resetJobPanel();
    log(`подписка на задачу ${jobId}`);
    subscribeToJob(jobId);
});

// --- Force-check ---

$('checkBtn').addEventListener('click', async () => {
    const jobId = $('jobIdInput').value.trim();
    if (!jobId) return;
    currentJobId = jobId;
    try {
        const res = await fetch(`${baseUrl()}/api/v1/job/${jobId}`);
        const body = await res.json();
        if (!res.ok) {
            log(`force-check ошибка (HTTP ${res.status}): ${JSON.stringify(body)}`, 'error');
            toast('Force-check: задача не найдена (HTTP ' + res.status + ')', 'error');
            return;
        }
        log(`force-check ${jobId} → ${body.status}`, 'ok');
        if (body.status === 'DONE') {
            addResultCard(jobId);
        }
        renderJobState(body);
    } catch (e) {
        log('ошибка force-check: ' + e.message, 'error');
        toast('Ошибка force-check: ' + e.message, 'error');
    }
});

// --- журнал ---

$('clearLogBtn').addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    logEl.innerHTML = '';
});
