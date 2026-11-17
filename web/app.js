'use strict';

/* ---------- Antennes ---------- */

const STATUS_URL = 'https://direct.radiopresence.com/status-json.xsl';
const NO_TITLE = 'En direct';

const STATIONS = [
  { id: 'toulouse', name: 'Radio Présence', city: '', mount: 'radio-presence' },
  { id: 'lourdes', name: 'Radio Présence Lourdes', city: 'LOURDES', mount: 'radio-presence-lourdes' },
  { id: 'figeac', name: 'Radio Présence Figeac', city: 'FIGEAC', mount: 'radio-presence-figeac' },
  { id: 'cahors', name: 'Radio Présence Cahors', city: 'CAHORS', mount: 'radio-presence-lot' },
  { id: 'stgaudens', name: 'Radio Présence St Gaudens', city: 'ST GAUDENS', mount: 'radio-presence-pyrenees' },
];

/**
 * Le serveur annonce par exemple « Titre - Radio Présence » ou « Titre - www.radiopresence.com »,
 * parfois seulement le suffixe : on ne garde que le titre.
 */
const SUFFIX = /(^|\s*-\s*)(www\.radiopresence\.com|radio\s*pr[eé]sence)\s*$/i;

function cleanTitle(raw) {
  if (typeof raw !== 'string') return null;
  let title = raw.trim();
  for (let before = null; before !== title;) {
    before = title;
    title = title.replace(SUFFIX, '').trim();
  }
  title = title.replace(/^-+|-+$/g, '').trim();
  return title || null;
}

/* ---------- Mémoire du navigateur (peut être indisponible : navigation privée…) ---------- */

const store = {
  get(key) { try { return localStorage.getItem(key); } catch { return null; } },
  set(key, value) { try { localStorage.setItem(key, value); } catch { /* sans mémoire, tant pis */ } },
};

/* ---------- État ---------- */

const $ = id => document.getElementById(id);

let station = STATIONS.find(s => s.id === store.get('radiop.station')) || STATIONS[0];
let wanted = false;      // l'auditeur veut entendre la radio
let loading = false;
let title = null;        // émission en cours
let retries = 0;
let retryTimer = null;
let stallTimer = null;
let sleepEndAt = null;   // échéance du minuteur de veille (Date.now()), ou null

const MAX_RETRIES = 6;

const audio = new Audio();
audio.preload = 'none';

/* ---------- Lecture du direct ---------- */

// Chaque connexion repart du direct : l'adresse change pour que rien ne soit relu depuis un cache.
const streamUrl = s => `https://direct.radiopresence.com/${s.mount}?t=${Date.now()}`;

function start() {
  wanted = true;
  retries = 0;
  $('playError').hidden = true;
  connect();
}

function connect() {
  clearTimeout(retryTimer);
  audio.src = streamUrl(station);
  setLoading(true);
  audio.play().catch(error => {
    // Le navigateur refuse de jouer sans geste de l'auditeur : on revient à l'arrêt.
    if (error && error.name === 'NotAllowedError') release();
    // Les autres échecs arrivent aussi par l'événement « error », qui retente.
  });
  render();
}

/** Arrêt complet : la connexion au flux est coupée, pour ne pas consommer de données en pause. */
function release() {
  wanted = false;
  clearTimeout(retryTimer);
  audio.pause();
  audio.removeAttribute('src');
  audio.load();
  setLoading(false);
  render();
}

function toggle() {
  if (wanted) release(); else start();
}

function retryOrGiveUp() {
  if (!wanted) return;
  if (retries < MAX_RETRIES) {
    retries++;
    setLoading(true);
    clearTimeout(retryTimer);
    retryTimer = setTimeout(connect, 3000);
  } else {
    release();
    $('playError').hidden = false;
  }
}

function setLoading(value) {
  loading = value;
  clearTimeout(stallTimer);
  // Un flux qui ne démarre pas au bout de 20 s : on se reconnecte.
  if (value && wanted) stallTimer = setTimeout(retryOrGiveUp, 20000);
  render();
}

audio.addEventListener('playing', () => { retries = 0; setLoading(false); });
audio.addEventListener('waiting', () => { if (wanted) setLoading(true); });
audio.addEventListener('error', () => { if (wanted && audio.getAttribute('src')) retryOrGiveUp(); });
audio.addEventListener('ended', retryOrGiveUp);   // le serveur a fermé le flux
// Pause venue d'ailleurs (casque débranché, appel téléphonique…) : on s'arrête proprement.
audio.addEventListener('pause', () => { if (wanted && audio.paused) release(); });
audio.addEventListener('timeupdate', checkSleep);

function choose(target) {
  closeAll();
  if (target.id === station.id) return;
  station = target;
  store.set('radiop.station', station.id);
  title = null;
  render();
  fetchTitle();
  if (wanted) { retries = 0; connect(); }
}

function step(offset) {
  const index = STATIONS.indexOf(station);
  choose(STATIONS[(index + offset + STATIONS.length) % STATIONS.length]);
}

/* ---------- Titre de l'émission en cours ---------- */

async function fetchTitle() {
  const asked = station;
  try {
    const response = await fetch(STATUS_URL, { cache: 'no-store' });
    const data = await response.json();
    let sources = data && data.icestats && data.icestats.source;
    if (!Array.isArray(sources)) sources = sources ? [sources] : [];
    const source = sources.find(s => String(s.listenurl || '').endsWith('/' + asked.mount));
    if (asked !== station) return;   // l'antenne a changé entre-temps
    const fresh = cleanTitle(source && source.title);
    if (fresh !== title) { title = fresh; render(); }
  } catch {
    // Pas de réseau ou serveur muet : on garde le titre affiché.
  }
}

setInterval(() => { if (wanted || !document.hidden) fetchTitle(); }, 20000);
document.addEventListener('visibilitychange', () => { if (!document.hidden) { fetchTitle(); checkSleep(); } });

/* ---------- Minuteur de veille ---------- */

function setSleep(minutes) {
  sleepEndAt = Date.now() + minutes * 60000;
  renderSleep();
}

function clearSleep() {
  sleepEndAt = null;
  renderSleep();
}

function checkSleep() {
  if (sleepEndAt && Date.now() >= sleepEndAt) {
    sleepEndAt = null;
    release();
  }
  renderSleep();
}

setInterval(checkSleep, 5000);

function renderSleep() {
  const badge = $('timerBadge');
  const moon = document.querySelector('.moon');
  const left = sleepEndAt ? Math.max(1, Math.ceil((sleepEndAt - Date.now()) / 60000)) : null;
  badge.hidden = left === null;
  badge.textContent = left === null ? '' : `${left} min`;
  moon.classList.toggle('active', left !== null);
  moon.setAttribute('aria-label', left === null ? 'Minuteur de veille' : `Minuteur de veille : arrêt dans ${left} min`);
  $('timerSub').textContent = left === null ? 'Arrêter la radio après' : `Arrêt de la radio dans ${left} min`;
  $('btnTimerClear').disabled = left === null;
}

/* ---------- Écran de verrouillage, montre, voiture (Media Session) ---------- */

const media = 'mediaSession' in navigator ? navigator.mediaSession : null;

if (media) {
  const actions = {
    play: start,
    pause: release,
    stop: release,
    previoustrack: () => step(-1),
    nexttrack: () => step(1),
  };
  for (const [action, handler] of Object.entries(actions)) {
    try { media.setActionHandler(action, handler); } catch { /* action inconnue de ce navigateur */ }
  }
}

function renderSession() {
  if (!media) return;
  const art = size => ({ src: new URL(`img/icon-${size}.png`, location.href).href, sizes: `${size}x${size}`, type: 'image/png' });
  try {
    media.metadata = new MediaMetadata({
      title: title || NO_TITLE,
      artist: station.name,
      album: 'radioP',
      artwork: [art(192), art(512)],
    });
    media.playbackState = wanted ? 'playing' : 'paused';
  } catch { /* navigateur ancien */ }
}

/* ---------- Affichage ---------- */

function render() {
  $('stationName').textContent = station.name;
  $('nowTitle').textContent = title || (wanted ? NO_TITLE : ' ');

  const play = $('btnPlay');
  play.classList.toggle('loading', wanted && loading);
  play.setAttribute('aria-label', wanted ? 'Arrêter' : 'Lecture');
  $('playIcon').setAttribute('href', wanted ? '#i-pause' : '#i-play');

  for (const button of document.querySelectorAll('#stationList button')) {
    button.setAttribute('aria-current', String(button.dataset.id === station.id));
  }
  document.title = wanted ? `${title || station.name} · radioP` : 'radioP';
  renderSession();
}

function buildStations() {
  const list = $('stationList');
  for (const s of STATIONS) {
    const item = document.createElement('li');
    const button = document.createElement('button');
    button.dataset.id = s.id;
    button.setAttribute('aria-label', `Écouter ${s.name}`);
    if (s.city) {
      const city = document.createElement('span');
      city.className = 'city';
      city.textContent = s.city;
      button.append(city);
    }
    const logo = document.createElement('img');
    logo.src = 'img/logo.svg';
    logo.alt = '';
    button.append(logo);
    button.addEventListener('click', () => choose(s));
    item.append(button);
    list.append(item);
  }
}

/* ---------- Panneaux, vues et bouton retour ---------- */

const OVERLAYS = ['drawer', 'stations', 'contact', 'timer'];

function hideOverlays() {
  for (const id of OVERLAYS) $(id).hidden = true;
  $('scrim').hidden = true;
}

function showView(name) {
  $('viewRadio').hidden = name !== 'radio';
  $('viewInfos').hidden = name !== 'infos';
}

/** Chaque panneau ouvert ajoute une étape à l'historique : le bouton retour d'Android le referme. */
function openOverlay(id) {
  hideOverlays();
  $(id).hidden = false;
  $('scrim').hidden = false;
  history.pushState({ radiop: id }, '');
  const focus = $(id).querySelector('button, a');
  if (focus) focus.focus({ preventScroll: true });
}

function closeAll() {
  if (history.state && history.state.radiop) history.back();
  else { hideOverlays(); showView('radio'); }
}

function go(view) {
  hideOverlays();
  showView(view);
  if (view === 'radio') {
    if (history.state && history.state.radiop) history.back();
  } else if (history.state && history.state.radiop) {
    history.replaceState({ radiop: view }, '');
  } else {
    history.pushState({ radiop: view }, '');
  }
}

window.addEventListener('popstate', () => {
  hideOverlays();
  showView(history.state && history.state.radiop === 'infos' ? 'infos' : 'radio');
});

document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && (!$('scrim').hidden || !$('viewInfos').hidden)) closeAll();
});

/* ---------- Branchements ---------- */

$('btnPlay').addEventListener('click', toggle);
$('scrim').addEventListener('click', closeAll);
$('btnTimerClear').addEventListener('click', () => { clearSleep(); closeAll(); });

for (const button of document.querySelectorAll('[data-open]')) {
  button.addEventListener('click', () => openOverlay(button.dataset.open));
}
for (const button of document.querySelectorAll('[data-close], [data-back]')) {
  button.addEventListener('click', closeAll);
}
for (const button of document.querySelectorAll('[data-go]')) {
  button.addEventListener('click', () => go(button.dataset.go));
}
for (const button of document.querySelectorAll('[data-minutes]')) {
  button.addEventListener('click', () => { setSleep(Number(button.dataset.minutes)); closeAll(); });
}
// Un lien (site, don, téléphone, courriel) referme le panneau qui le contient.
for (const link of document.querySelectorAll('.drawer a, .dialog a')) {
  link.addEventListener('click', () => setTimeout(closeAll, 0));
}

buildStations();
render();
renderSleep();
fetchTitle();

if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  navigator.serviceWorker.register('sw.js').catch(() => { /* l'appli marche aussi sans */ });
}
