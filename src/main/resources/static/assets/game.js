const elements = {
  form: document.querySelector('#playerForm'),
  playerName: document.querySelector('#playerName'),
  startButton: document.querySelector('#startButton'),
  board: document.querySelector('#board'),
  score: document.querySelector('#scoreValue'),
  time: document.querySelector('#timeValue'),
  hits: document.querySelector('#hitsValue'),
  misses: document.querySelector('#missesValue'),
  status: document.querySelector('#statusText'),
  session: document.querySelector('#sessionText'),
  leaderboard: document.querySelector('#leaderboard')
};

const HIT_REACTION_MILLIS = 340;

let gameConfig = null;
let sessionId = null;
let startedAt = 0;
let running = false;
let finishing = false;
let timers = [];
let activeEvents = new Map();
let hitAudioPool = [];
let hitAudioIndex = 0;
let audioContext = null;
let audioUnlocked = false;
let configuredHitSound = '';

async function init() {
  await loadConfig();
  await loadLeaderboard();
  elements.form.addEventListener('submit', handleStart);
}

async function loadConfig() {
  const response = await fetch('/api/game/config');
  gameConfig = await response.json();
  elements.time.textContent = gameConfig.durationSeconds.toFixed(1);
  prepareHitAudio(gameConfig.assets.hitSound);
  renderBoard(gameConfig.rows, gameConfig.assets);
}

function renderBoard(rows, assets) {
  elements.board.innerHTML = '';
  let holeIndex = 0;
  const moleAsset = assets.mole;
  const moleHitAsset = assets.moleHit || assets.mole;

  rows.forEach((count, rowIndex) => {
    const row = document.createElement('div');
    row.className = 'hole-row';
    row.style.gridTemplateColumns = `repeat(${count}, minmax(0, 82px))`;
    row.setAttribute('aria-label', `${rowIndex + 1}번째 줄`);

    for (let column = 0; column < count; column += 1) {
      const hole = document.createElement('button');
      hole.className = 'hole-cell';
      hole.type = 'button';
      hole.dataset.holeIndex = String(holeIndex);
      hole.setAttribute('aria-label', `${holeIndex + 1}번 구멍`);
      hole.innerHTML = `
        <span class="hole-bowl"></span>
        <img class="mole-img mole-img-up" src="${moleAsset}" alt="">
        <img class="mole-img mole-img-hit" src="${moleHitAsset}" alt="">
      `;
      hole.addEventListener('click', () => handleHoleClick(hole));
      row.appendChild(hole);
      holeIndex += 1;
    }

    elements.board.appendChild(row);
  });
}

async function handleStart(event) {
  event.preventDefault();
  if (running || finishing) {
    return;
  }

  const playerName = elements.playerName.value.trim();
  if (!playerName) {
    elements.status.textContent = '플레이어 이름을 입력하세요.';
    elements.playerName.focus();
    return;
  }

  resetGame();
  setControls(false);
  elements.status.textContent = '게임을 준비하는 중입니다.';

  try {
    const response = await fetch('/api/games', { method: 'POST' });
    if (!response.ok) {
      throw new Error('게임 세션 생성 실패');
    }

    const payload = await response.json();
    sessionId = payload.sessionId;
    gameConfig = payload.config;
    unlockAudio();
    prepareHitAudio(gameConfig.assets.hitSound);
    renderBoard(gameConfig.rows, gameConfig.assets);
    startSchedule(payload.schedule);
  } catch (error) {
    elements.status.textContent = '게임을 시작할 수 없습니다. 서버 상태를 확인하세요.';
    setControls(true);
  }
}

function startSchedule(schedule) {
  running = true;
  finishing = false;
  startedAt = performance.now();
  elements.status.textContent = '두더지가 올라오면 클릭하세요.';
  elements.session.textContent = 'Playing';

  schedule.forEach((event) => {
    const timer = window.setTimeout(() => showMole(event), event.appearAtMillis);
    timers.push(timer);
  });

  timers.push(window.setInterval(updateClock, 100));
  timers.push(window.setTimeout(finishGame, gameConfig.durationSeconds * 1000 + 250));
}

function showMole(event) {
  if (!running) {
    return;
  }

  const hole = elements.board.querySelector(`[data-hole-index="${event.holeIndex}"]`);
  if (!hole) {
    return;
  }

  hole.classList.remove('is-hit', 'is-pending-hit');
  hole.classList.add('is-active');
  hole.dataset.eventId = String(event.id);

  const hideTimer = window.setTimeout(() => {
    if (hole.dataset.eventId === String(event.id)) {
      hideMole(hole);
      updateMisses(Number(elements.misses.textContent) + 1);
    }
    activeEvents.delete(event.id);
  }, event.visibleMillis);

  activeEvents.set(event.id, { hole, hideTimer });
}

async function handleHoleClick(hole) {
  if (!running || !sessionId) {
    markEmpty(hole);
    return;
  }

  const eventId = Number(hole.dataset.eventId || 0);
  if (!eventId || !hole.classList.contains('is-active')) {
    markEmpty(hole);
    return;
  }
  if (hole.classList.contains('is-pending-hit') || hole.classList.contains('is-hit')) {
    return;
  }

  const active = activeEvents.get(eventId);
  if (active) {
    window.clearTimeout(active.hideTimer);
    activeEvents.delete(eventId);
  }

  hole.classList.add('is-pending-hit');

  try {
    const response = await fetch(`/api/games/${sessionId}/hits/${eventId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ clientHitAtMillis: Math.round(performance.now() - startedAt) })
    });
    const payload = await response.json();
    if (response.ok) {
      applyState(payload);
      if (payload.accepted) {
        playHitSound();
        showHitMole(hole);
      } else {
        hideMole(hole);
      }
      elements.status.textContent = payload.accepted ? 'Hit!' : payload.message;
    } else {
      hideMole(hole);
      elements.status.textContent = payload.message || '점수 반영에 실패했습니다.';
    }
  } catch (error) {
    hideMole(hole);
    elements.status.textContent = '네트워크 오류로 hit가 반영되지 않았습니다.';
  }
}

function hideMole(hole) {
  hole.classList.remove('is-active', 'is-hit', 'is-pending-hit');
  delete hole.dataset.eventId;
}

function showHitMole(hole) {
  hole.classList.remove('is-pending-hit');
  hole.classList.add('is-active', 'is-hit');
  delete hole.dataset.eventId;

  window.setTimeout(() => {
    hideMole(hole);
  }, HIT_REACTION_MILLIS);
}

function markEmpty(hole) {
  hole.classList.add('is-empty-hit');
  window.setTimeout(() => hole.classList.remove('is-empty-hit'), 180);
}

function updateClock() {
  if (!running) {
    return;
  }

  const elapsed = performance.now() - startedAt;
  const remaining = Math.max(0, gameConfig.durationSeconds * 1000 - elapsed);
  elements.time.textContent = (remaining / 1000).toFixed(1);
}

async function finishGame() {
  if (!running || finishing || !sessionId) {
    return;
  }

  finishing = true;
  running = false;
  clearTimers();
  clearActiveMoles();
  updateClock();
  elements.time.textContent = '0.0';
  elements.status.textContent = '점수를 저장하는 중입니다.';
  elements.session.textContent = 'Saving';

  try {
    const response = await fetch(`/api/games/${sessionId}/finish`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerName: elements.playerName.value.trim() })
    });
    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload.message || '점수 저장 실패');
    }

    applyState(payload.finalState);
    if (payload.leaderboardUpdated) {
      elements.status.textContent = `${payload.entry.playerName}님 새 최고 점수 ${payload.entry.score}점 저장 완료`;
    } else {
      elements.status.textContent = `${payload.entry.playerName}님 기존 최고 ${payload.entry.score}점 유지`;
    }
    elements.session.textContent = 'Finished';
    await loadLeaderboard();
  } catch (error) {
    elements.status.textContent = '점수를 저장하지 못했습니다. 다시 시작해 주세요.';
    elements.session.textContent = 'Error';
  } finally {
    sessionId = null;
    finishing = false;
    setControls(true);
  }
}

async function loadLeaderboard() {
  try {
    const response = await fetch('/api/leaderboard?limit=10');
    const rows = await response.json();
    renderLeaderboard(rows);
  } catch (error) {
    elements.leaderboard.innerHTML = '<div class="empty-state">리더보드를 불러오지 못했습니다.</div>';
  }
}

function renderLeaderboard(rows) {
  if (!rows.length) {
    elements.leaderboard.innerHTML = '<div class="empty-state">아직 기록이 없습니다.</div>';
    return;
  }

  elements.leaderboard.innerHTML = rows.map((row, index) => `
    <div class="leaderboard-row">
      <div class="rank-badge">${index + 1}</div>
      <div>
        <div class="player-name">${escapeHtml(row.playerName)}</div>
        <div class="player-meta">${row.hits} hits · ${row.misses} misses</div>
      </div>
      <div class="score-pill">${row.score}</div>
    </div>
  `).join('');
}

function applyState(state) {
  elements.score.textContent = String(state.score);
  elements.hits.textContent = String(state.hits);
  updateMisses(state.misses);
}

function updateMisses(value) {
  elements.misses.textContent = String(Math.max(0, value));
}

function prepareHitAudio(assetPath) {
  if (!assetPath || configuredHitSound === assetPath) {
    return;
  }

  configuredHitSound = assetPath;
  hitAudioIndex = 0;
  hitAudioPool = Array.from({ length: 4 }, () => {
    const audio = new Audio(assetPath);
    audio.preload = 'auto';
    audio.volume = 0.7;
    return audio;
  });
}

function unlockAudio() {
  if (audioUnlocked) {
    return;
  }

  audioUnlocked = true;
  const AudioContextClass = window.AudioContext || window.webkitAudioContext;
  if (AudioContextClass && !audioContext) {
    audioContext = new AudioContextClass();
  }
  if (audioContext && audioContext.state === 'suspended') {
    audioContext.resume().catch(() => {});
  }
}

function playHitSound() {
  const audio = hitAudioPool[hitAudioIndex % Math.max(hitAudioPool.length, 1)];
  hitAudioIndex += 1;

  if (audio) {
    audio.currentTime = 0;
    audio.play().catch(() => playSyntheticHitSound());
    return;
  }

  playSyntheticHitSound();
}

function playSyntheticHitSound() {
  const AudioContextClass = window.AudioContext || window.webkitAudioContext;
  if (!AudioContextClass) {
    return;
  }

  if (!audioContext) {
    audioContext = new AudioContextClass();
  }
  if (audioContext.state === 'suspended') {
    audioContext.resume().catch(() => {});
  }

  const now = audioContext.currentTime;
  const oscillator = audioContext.createOscillator();
  const gain = audioContext.createGain();

  oscillator.type = 'triangle';
  oscillator.frequency.setValueAtTime(190, now);
  oscillator.frequency.exponentialRampToValueAtTime(70, now + 0.08);
  gain.gain.setValueAtTime(0.18, now);
  gain.gain.exponentialRampToValueAtTime(0.001, now + 0.1);

  oscillator.connect(gain);
  gain.connect(audioContext.destination);
  oscillator.start(now);
  oscillator.stop(now + 0.11);
}

function resetGame() {
  clearTimers();
  clearActiveMoles();
  sessionId = null;
  startedAt = 0;
  running = false;
  finishing = false;
  elements.score.textContent = '0';
  elements.hits.textContent = '0';
  elements.misses.textContent = '0';
  elements.time.textContent = gameConfig ? gameConfig.durationSeconds.toFixed(1) : '30.0';
  elements.session.textContent = 'Ready';
}

function clearTimers() {
  timers.forEach((timer) => {
    window.clearTimeout(timer);
    window.clearInterval(timer);
  });
  timers = [];
}

function clearActiveMoles() {
  activeEvents.forEach(({ hole, hideTimer }) => {
    window.clearTimeout(hideTimer);
    hideMole(hole);
  });
  activeEvents.clear();
}

function setControls(enabled) {
  elements.startButton.disabled = !enabled;
  elements.playerName.disabled = !enabled;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

init();
