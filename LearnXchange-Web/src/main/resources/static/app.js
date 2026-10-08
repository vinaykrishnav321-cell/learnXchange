(() => {
  'use strict';

  const SLOTS = ['Weekday morning', 'Weekday afternoon', 'Weekday evening',
                 'Weekend morning', 'Weekend afternoon', 'Weekend evening'];
  const LEVELS = ['Beginner', 'Basic', 'Comfortable', 'Advanced', 'Expert'];
  const state = { me: null, view: null, catalog: null, matches: [], adminTab: 'users', reports: [] };

  const $ = (sel, root = document) => root.querySelector(sel);
  const app = $('#app');
  const modal = $('#modal');
  const modalBody = $('#modal-body');
  const toastEl = $('#toast');

  const esc = (v) => String(v ?? '').replace(/[&<>"']/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  const ICON_SWAP = '<svg viewBox="0 0 32 32" aria-hidden="true"><path d="M7 12h17m0 0-5-5m5 5-5 5M25 21H8m0 0 5-5m-5 5 5 5" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>';
  const BRAND_MARK = '<svg viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="9" fill="#3347ff"/><path d="M8 12h14m0 0-4-4m4 4-4 4M24 20H10m0 0 4-4m-4 4 4 4" stroke="#fff" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>';

  /* ---------------- helpers ---------------- */
  async function api(method, url, body) {
    const res = await fetch(url, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
      body: body !== undefined ? JSON.stringify(body) : undefined,
      credentials: 'same-origin'
    });
    let data = null;
    try { data = await res.json(); } catch (e) { /* empty body */ }
    if (!res.ok) {
      const err = new Error((data && data.error) || 'Something went wrong. Please try again.');
      err.status = res.status;
      throw err;
    }
    return data;
  }

  let toastTimer;
  function toast(message, kind = 'info') {
    toastEl.textContent = message;
    toastEl.className = 'toast show ' + kind;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { toastEl.className = 'toast'; }, 4200);
  }

  async function safe(fn) {
    try {
      return await fn();
    } catch (e) {
      if (e.status === 401 && state.me) {
        state.me = null;
        closeModal();
        renderAuth();
        toast('Your session ended. Please sign in again.', 'error');
      } else {
        toast(e.message, 'error');
      }
      return undefined;
    }
  }

  const initials = (name) => String(name || '?').trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join('').toUpperCase();
  const avatar = (id, name, size = '') => `<span class="avatar ${size}" style="--hue:${(id * 47) % 360}" aria-hidden="true">${esc(initials(name))}</span>`;
  const ratingText = (u) => (u.ratingCount ? `${Number(u.avgRating).toFixed(1)} out of 5, ${u.ratingCount} review${u.ratingCount === 1 ? '' : 's'}` : 'New member');
  const chips = (list, cls, empty) => (list && list.length
    ? list.map((s) => `<span class="chip ${cls}">${esc(s)}</span>`).join('')
    : `<span class="chip none">${esc(empty)}</span>`);
  const dots = (n) => `<span class="dots" role="img" aria-label="Level ${n} of 5, ${LEVELS[n - 1]}">${[1, 2, 3, 4, 5].map((i) => `<i class="${i <= n ? 'on' : ''}"></i>`).join('')}</span>`;
  const pill = (status) => `<span class="pill ${esc(String(status).toLowerCase())}">${esc(String(status).charAt(0) + String(status).slice(1).toLowerCase())}</span>`;
  const fmtDateTime = (iso) => new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
  const pct = (x) => Math.round(x * 100);
  const linkOrText = (v) => (/^https?:\/\//i.test(v)
    ? `<a href="${esc(v)}" target="_blank" rel="noopener noreferrer">${esc(v)}</a>` : esc(v));

  function openModal(html) {
    modalBody.innerHTML = `<div class="modal-in">${html}</div>`;
    if (!modal.open) modal.showModal();
  }
  function closeModal() { if (modal.open) modal.close(); }
  modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });

  async function ensureCatalog(force = false) {
    if (!state.catalog || force) state.catalog = await api('GET', '/api/skills');
    return state.catalog;
  }

  /* ---------------- auth screen ---------------- */
  function renderAuth(tab = 'login') {
    state.view = null;
    app.innerHTML = `
      <div class="auth">
        <section class="auth-hero">
          <div class="brand">${BRAND_MARK}<span>LearnXchange</span></div>
          <h1>Trade what you know for what you want to learn.</h1>
          <p class="sub">No courses, no fees. Tell us the skills you can teach and the ones you want, and we'll find people whose answers are the mirror image of yours.</p>
          <article class="ticket demo" style="grid-template-columns:1fr auto;" aria-label="Example match">
            <div class="ticket-who">${avatar(5, 'Maya Rao')}<div><h3>Maya Rao</h3><p class="muted">Design, 3rd year</p></div></div>
            <div class="ring" style="--p:92" role="img" aria-label="92 percent match"><span>92%</span></div>
            <div class="ticket-swap" style="grid-column:1/-1;">
              <div class="side get"><span class="side-label">You get</span><div class="chips"><span class="chip">Photoshop</span></div></div>
              <div class="swap-glyph">${ICON_SWAP}</div>
              <div class="side give"><span class="side-label">You give</span><div class="chips"><span class="chip">Java</span></div></div>
            </div>
          </article>
        </section>
        <section class="auth-panel">
          <div class="auth-card">
            <div class="tabs" role="tablist">
              <button role="tab" data-action="auth-tab" data-tab="login" aria-selected="${tab === 'login'}">Sign in</button>
              <button role="tab" data-action="auth-tab" data-tab="register" aria-selected="${tab === 'register'}">Create account</button>
            </div>
            ${tab === 'login' ? `
            <form data-form="login" novalidate>
              <div class="field"><label for="l-email">Email</label><input id="l-email" name="email" type="email" autocomplete="username" required></div>
              <div class="field"><label for="l-pass">Password</label><input id="l-pass" name="password" type="password" autocomplete="current-password" required></div>
              <button class="btn primary" type="submit" style="width:100%">Sign in</button>
            </form>` : `
            <form data-form="register" novalidate>
              <div class="field"><label for="r-name">Full name</label><input id="r-name" name="name" type="text" autocomplete="name" required></div>
              <div class="field"><label for="r-email">Email</label><input id="r-email" name="email" type="email" autocomplete="email" required></div>
              <div class="field"><label for="r-dept">Department <span class="muted">(optional)</span></label><input id="r-dept" name="department" type="text"></div>
              <div class="field"><label for="r-pass">Password</label><input id="r-pass" name="password" type="password" autocomplete="new-password" required>
                <p class="hint">At least 8 characters, with a letter and a digit.</p></div>
              <div class="field"><label for="r-conf">Confirm password</label><input id="r-conf" name="confirm" type="password" autocomplete="new-password" required></div>
              <button class="btn primary" type="submit" style="width:100%">Create account</button>
            </form>`}
          </div>
        </section>
      </div>`;
    const first = $('input');
    if (first) first.focus();
  }

  /* ---------------- shell & routing ---------------- */
  function enterApp(view) {
    const me = state.me;
    const isAdmin = me.role === 'ADMIN';
    const nav = isAdmin
      ? [['admin', 'Admin']]
      : [['matches', 'Matches'], ['profile', 'My profile'], ['requests', 'Requests'], ['sessions', 'Sessions']];
    app.innerHTML = `
      <header class="topbar"><div class="topbar-in">
        <div class="brand">${BRAND_MARK}<span>LearnXchange</span></div>
        <nav class="nav" aria-label="Main">
          ${nav.map(([v, label]) => `<button data-action="nav" data-view="${v}">${label}${v === 'requests' ? ' <span id="req-badge" class="badge" hidden>0</span>' : ''}</button>`).join('')}
        </nav>
        <div class="me">${avatar(me.id, me.name, 'sm')}<span class="name">${esc(me.name)}</span>
          <button class="btn small" data-action="logout">Sign out</button></div>
      </div></header>
      <main id="view" tabindex="-1"></main>`;
    go(view || (isAdmin ? 'admin' : 'matches'));
  }

  const views = {
    matches: viewMatches, profile: viewProfile, requests: viewRequests,
    sessions: viewSessions, admin: viewAdmin
  };

  async function go(view) {
    state.view = view;
    document.querySelectorAll('.nav button').forEach((b) => {
      if (b.dataset.view === view) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
    });
    await safe(() => views[view]());
    window.scrollTo(0, 0);
    refreshBadge();
  }

  async function refreshBadge() {
    const badge = $('#req-badge');
    if (!state.me || !badge) return;
    try {
      const r = await api('GET', '/api/requests/pending-count');
      badge.textContent = r.count;
      badge.hidden = r.count === 0;
    } catch (e) { /* cosmetic */ }
  }
  setInterval(refreshBadge, 60000);

  /* ---------------- matches ---------------- */
  async function viewMatches() {
    const catalog = await ensureCatalog();
    const categories = [...new Set(catalog.map((s) => s.category))].sort();
    $('#view').innerHTML = `
      <div class="page-head"><h1>People worth swapping with</h1>
        <p class="sub">Ranked by how well your skills fit in both directions, plus proficiency, shared free time and past reviews.</p></div>
      <form class="filters" data-form="filters">
        <input type="text" name="skill" placeholder="Skill they teach, e.g. Photoshop" aria-label="Skill they teach">
        <select name="category" aria-label="Category"><option value="">All categories</option>${categories.map((c) => `<option>${esc(c)}</option>`).join('')}</select>
        <select name="slot" aria-label="Availability"><option value="">Any availability</option>${SLOTS.map((s) => `<option>${esc(s)}</option>`).join('')}</select>
        <button class="btn primary" type="submit">Search</button>
        <button class="btn" type="button" data-action="reset-filters">Reset</button>
      </form>
      <div id="match-list" class="match-list" aria-live="polite"><p class="muted">Finding matches...</p></div>`;
    await loadMatches();
  }

  async function loadMatches() {
    const form = $('form[data-form=filters]');
    const fd = new FormData(form);
    const qs = new URLSearchParams();
    ['skill', 'category', 'slot'].forEach((k) => { const v = String(fd.get(k) || '').trim(); if (v) qs.set(k, v); });
    state.matches = await api('GET', '/api/matches' + (qs.toString() ? '?' + qs : ''));
    const list = $('#match-list');
    if (!state.matches.length) {
      list.innerHTML = `<div class="empty"><h3>No matches yet</h3>
        <p class="muted">Matches appear when someone teaches what you want to learn, or wants to learn what you teach. Add both kinds of skills to your profile.</p>
        <button class="btn primary" data-action="nav" data-view="profile">Add your skills</button></div>`;
      return;
    }
    list.innerHTML = state.matches.map(ticketHtml).join('');
  }

  function ticketHtml(m) {
    const u = m.user;
    return `
      <article class="ticket">
        <div class="ticket-who">${avatar(u.id, u.name)}
          <div><h3>${esc(u.name)}</h3><p class="muted">${esc(u.department || 'No department')}</p>
          <p class="muted">${esc(ratingText(u))}</p></div></div>
        <div class="ticket-swap">
          <div class="side get"><span class="side-label">You get</span><div class="chips">${chips(m.theyTeachMe, '', 'Nothing you listed')}</div></div>
          <div class="swap-glyph">${ICON_SWAP}</div>
          <div class="side give"><span class="side-label">You give</span><div class="chips">${chips(m.iTeachThem, '', 'Nothing they listed')}</div></div>
        </div>
        <div class="ring" style="--p:${m.percentage}" role="img" aria-label="${m.percentage} percent match"><span>${Math.round(m.percentage)}%</span></div>
        <div class="ticket-actions">
          <button class="btn primary small" data-action="request-swap" data-id="${u.id}">Request swap</button>
          <button class="btn small" data-action="view-user" data-id="${u.id}">View profile</button>
        </div>
      </article>`;
  }

  async function openUserModal(id) {
    const p = await api('GET', '/api/users/' + id);
    const m = state.matches.find((x) => x.user.id === id);
    const teach = p.skills.filter((s) => s.type === 'TEACH');
    const learn = p.skills.filter((s) => s.type === 'LEARN');
    const skillLine = (s) => `<div class="skill-row"><div class="grow"><strong>${esc(s.skillName)}</strong> <span class="muted">${esc(s.category)}</span></div>${dots(s.proficiency)}</div>`;
    const bar = (label, v) => `<div class="bar"><span>${label}</span><div class="bar-track"><i style="width:${pct(v)}%"></i></div><b>${pct(v)}%</b></div>`;
    openModal(`
      <div class="modal-head">${avatar(p.user.id, p.user.name, 'lg')}
        <div class="grow"><h2 id="modal-title">${esc(p.user.name)}</h2>
          <p class="muted">${esc(p.user.department || 'No department')}</p><p class="muted">${esc(ratingText(p.user))}</p></div>
        <button class="x" data-action="close-modal" aria-label="Close">&times;</button></div>
      ${p.user.bio ? `<p>${esc(p.user.bio)}</p>` : '<p class="muted">No bio yet.</p>'}
      <div><h3>Free time</h3><div class="chips" style="margin-top:8px">${chips(p.user.availability, '', 'Not set')}</div></div>
      <div class="grid-2" style="gap:14px">
        <div><h3>Can teach</h3>${teach.length ? teach.map(skillLine).join('') : '<p class="muted">Nothing listed</p>'}</div>
        <div><h3>Wants to learn</h3>${learn.length ? learn.map(skillLine).join('') : '<p class="muted">Nothing listed</p>'}</div>
      </div>
      ${m ? `<div class="stack" style="gap:8px"><h3>Why ${Math.round(m.percentage)}%</h3>
        ${bar('Skill fit', m.skillScore)}${bar('Reverse fit', m.reverseScore)}${bar('Proficiency', m.proficiencyScore)}${bar('Availability', m.availabilityScore)}${bar('Rating', m.ratingScore)}</div>` : ''}
      <div><h3>Reviews</h3>${p.reviews.length ? p.reviews.map((r) => `<div class="review"><strong>${r.score}/5</strong> from ${esc(r.raterName)} <span class="muted">on ${esc(fmtDateTime(r.createdAt))}</span>${r.feedback ? `<br>${esc(r.feedback)}` : ''}</div>`).join('') : '<p class="muted">No reviews yet.</p>'}</div>
      <div class="modal-actions">
        <button class="btn danger" data-action="report-user" data-id="${p.user.id}" data-name="${esc(p.user.name)}">Report</button>
        <button class="btn primary" data-action="request-swap" data-id="${p.user.id}">Request swap</button>
      </div>`);
  }

  async function openSwapModal(userId) {
    const [p, mine] = await Promise.all([api('GET', '/api/users/' + userId), api('GET', '/api/profile')]);
    const myTeach = mine.skills.filter((s) => s.type === 'TEACH');
    const theirTeach = p.skills.filter((s) => s.type === 'TEACH');
    if (!myTeach.length) {
      toast('Add a skill you can teach to your profile first.', 'error');
      closeModal();
      go('profile');
      return;
    }
    if (!theirTeach.length) { toast(`${p.user.name} hasn't listed a skill to teach yet.`, 'error'); return; }
    const m = state.matches.find((x) => x.user.id === userId);
    const pick = (list, names) => { const hit = list.find((s) => names && names.includes(s.skillName)); return hit ? hit.skillId : list[0].skillId; };
    const offerSel = pick(myTeach, m && m.iTeachThem);
    const wantSel = pick(theirTeach, m && m.theyTeachMe);
    const opts = (list, sel) => list.map((s) => `<option value="${s.skillId}" ${s.skillId === sel ? 'selected' : ''}>${esc(s.skillName)} (level ${s.proficiency})</option>`).join('');
    openModal(`
      <div class="modal-head"><div class="grow"><h2 id="modal-title">Request a swap with ${esc(p.user.name)}</h2></div>
        <button class="x" data-action="close-modal" aria-label="Close">&times;</button></div>
      <form data-form="swap" data-receiver="${userId}">
        <div class="row">
          <div class="field"><label for="s-offer">You teach</label><select id="s-offer" name="offered">${opts(myTeach, offerSel)}</select></div>
          <div class="field"><label for="s-want">You learn</label><select id="s-want" name="requested">${opts(theirTeach, wantSel)}</select></div>
        </div>
        <div class="field"><label for="s-msg">Message</label><textarea id="s-msg" name="message" maxlength="500" required placeholder="Say hi and mention what you hope to learn."></textarea></div>
        <div class="modal-actions"><button type="button" class="btn" data-action="close-modal">Cancel</button><button type="submit" class="btn primary">Send request</button></div>
      </form>`);
  }

  function openReportModal(id, name) {
    openModal(`
      <div class="modal-head"><div class="grow"><h2 id="modal-title">Report ${esc(name)}</h2></div>
        <button class="x" data-action="close-modal" aria-label="Close">&times;</button></div>
      <form data-form="report" data-user="${id}">
        <div class="field"><label for="rp-reason">What happened?</label><textarea id="rp-reason" name="reason" maxlength="500" required></textarea>
          <p class="hint">An administrator will review this report.</p></div>
        <div class="modal-actions"><button type="button" class="btn" data-action="close-modal">Cancel</button><button type="submit" class="btn danger">Send report</button></div>
      </form>`);
  }

  /* ---------------- profile ---------------- */
  async function viewProfile() {
    const [p, catalog] = await Promise.all([api('GET', '/api/profile'), ensureCatalog()]);
    state.me = p.user;
    const byCat = {};
    catalog.forEach((s) => { (byCat[s.category] = byCat[s.category] || []).push(s); });
    const options = Object.keys(byCat).sort().map((c) =>
      `<optgroup label="${esc(c)}">${byCat[c].map((s) => `<option value="${s.id}">${esc(s.name)}</option>`).join('')}</optgroup>`).join('');
    $('#view').innerHTML = `
      <div class="page-head"><h1>My profile</h1><p class="sub">Matches are only as good as what you tell us here.</p></div>
      <form class="card" data-form="profile">
        <h2>About you</h2>
        <p class="muted" style="margin-bottom:14px">Signed in as ${esc(p.user.email)}${p.user.ratingCount ? ` &middot; rated ${esc(ratingText(p.user))}` : ''}</p>
        <div class="row">
          <div class="field"><label for="p-name">Name</label><input id="p-name" name="name" type="text" value="${esc(p.user.name)}" required></div>
          <div class="field"><label for="p-dept">Department</label><input id="p-dept" name="department" type="text" value="${esc(p.user.department)}"></div>
        </div>
        <div class="field"><label for="p-bio">Bio</label><textarea id="p-bio" name="bio" maxlength="500">${esc(p.user.bio)}</textarea></div>
        <div class="field"><label>When are you free?</label>
          <div class="slot-toggles">${SLOTS.map((s) => `<label><input type="checkbox" name="slot" value="${esc(s)}" ${p.user.availability.includes(s) ? 'checked' : ''}><span>${esc(s)}</span></label>`).join('')}</div></div>
        <button class="btn primary" type="submit">Save profile</button>
      </form>
      <div class="card">
        <h2>Add a skill</h2>
        <form data-form="addskill">
          <div class="row">
            <div class="field"><label for="k-skill">Skill</label><select id="k-skill" name="skillId" required><option value="">Choose a skill</option>${options}</select></div>
            <div class="field"><label for="k-type">I...</label><select id="k-type" name="type"><option value="TEACH">can teach this</option><option value="LEARN">want to learn this</option></select></div>
            <div class="field"><label for="k-level">Level: <output id="k-out">Comfortable</output></label><input id="k-level" name="level" type="range" min="1" max="5" value="3"></div>
          </div>
          <button class="btn primary" type="submit">Add skill</button>
          <p class="hint">Adding a skill you already have updates its level.</p>
        </form>
      </div>
      <div id="skill-lists"></div>`;
    paintSkills(p.skills);
  }

  function paintSkills(skills) {
    const row = (s) => `<div class="skill-row"><div class="grow"><strong>${esc(s.skillName)}</strong> <span class="muted">${esc(s.category)}</span></div>${dots(s.proficiency)}
      <button class="btn small danger" data-action="remove-skill" data-id="${s.id}" aria-label="Remove ${esc(s.skillName)}">Remove</button></div>`;
    const teach = skills.filter((s) => s.type === 'TEACH');
    const learn = skills.filter((s) => s.type === 'LEARN');
    $('#skill-lists').innerHTML = `
      <div class="grid-2">
        <div class="card"><h2>I can teach</h2>${teach.length ? teach.map(row).join('') : '<p class="muted">Nothing yet. What are you good at?</p>'}</div>
        <div class="card"><h2>I want to learn</h2>${learn.length ? learn.map(row).join('') : '<p class="muted">Nothing yet. What would you like to pick up?</p>'}</div>
      </div>`;
  }

  /* ---------------- requests ---------------- */
  async function viewRequests() {
    const [inc, out] = await Promise.all([api('GET', '/api/requests/incoming'), api('GET', '/api/requests/outgoing')]);
    const card = (r, incoming) => {
      const otherId = incoming ? r.senderId : r.receiverId;
      const otherName = incoming ? r.senderName : r.receiverName;
      const get = incoming ? r.offeredSkillName : r.requestedSkillName;
      const give = incoming ? r.requestedSkillName : r.offeredSkillName;
      let actions = '';
      if (incoming && r.status === 'PENDING') {
        actions = `<button class="btn primary small" data-action="accept-request" data-id="${r.id}">Accept</button>
                   <button class="btn small" data-action="reject-request" data-id="${r.id}">Decline</button>`;
      } else if (!incoming && r.status === 'PENDING') {
        actions = `<button class="btn small" data-action="cancel-request" data-id="${r.id}">Cancel request</button>`;
      } else if (r.status === 'ACCEPTED') {
        actions = `<button class="btn primary small" data-action="schedule" data-id="${r.id}" data-label="${esc(r.offeredSkillName + ' for ' + r.requestedSkillName)}">Schedule a session</button>`;
      }
      return `<article class="item"><div class="item-main">
          <div class="item-top">${avatar(otherId, otherName, 'sm')}<strong>${esc(otherName)}</strong><span class="muted">${esc(fmtDateTime(r.createdAt))}</span>${pill(r.status)}</div>
          <div class="pair"><div class="side get"><span class="side-label">You get</span><div class="chips"><span class="chip">${esc(get)}</span></div></div>
            <div class="side give"><span class="side-label">You give</span><div class="chips"><span class="chip">${esc(give)}</span></div></div></div>
          <blockquote>${esc(r.message)}</blockquote></div>
          <div class="item-actions">${actions}</div></article>`;
    };
    $('#view').innerHTML = `
      <div class="page-head"><h1>Requests</h1><p class="sub">Accept a swap to schedule your first session.</p></div>
      <div class="section-title"><h2>Received</h2></div>
      <div class="stack">${inc.length ? inc.map((r) => card(r, true)).join('') : '<div class="empty"><p class="muted">No requests received yet.</p></div>'}</div>
      <div class="section-title"><h2>Sent</h2></div>
      <div class="stack">${out.length ? out.map((r) => card(r, false)).join('') : '<div class="empty"><p class="muted">You haven\'t sent any requests. Find someone on the Matches page.</p></div>'}</div>`;
  }

  function openScheduleModal(requestId, label) {
    const today = new Date().toISOString().slice(0, 10);
    openModal(`
      <div class="modal-head"><div class="grow"><h2 id="modal-title">Schedule a session</h2><p class="muted">${esc(label)}</p></div>
        <button class="x" data-action="close-modal" aria-label="Close">&times;</button></div>
      <form data-form="schedule" data-request="${requestId}">
        <div class="row">
          <div class="field"><label for="sc-date">Date</label><input id="sc-date" name="date" type="date" min="${today}" required></div>
          <div class="field"><label for="sc-time">Time</label><input id="sc-time" name="time" type="time" value="18:00" required></div>
        </div>
        <div class="field"><label for="sc-mode">Where</label><select id="sc-mode" name="mode"><option value="ONLINE">Online</option><option value="OFFLINE">In person</option></select></div>
        <div class="field"><label for="sc-where">Meeting link or place</label><input id="sc-where" name="where" type="text" maxlength="255" placeholder="https://meet.google.com/... or Library, room 2" required></div>
        <div class="modal-actions"><button type="button" class="btn" data-action="close-modal">Cancel</button><button type="submit" class="btn primary">Schedule session</button></div>
      </form>`);
  }

  /* ---------------- sessions ---------------- */
  async function viewSessions() {
    const list = await api('GET', '/api/sessions');
    const me = state.me.id;
    const card = (s) => {
      const partnerId = s.userAId === me ? s.userBId : s.userAId;
      const partner = s.userAId === me ? s.userBName : s.userAName;
      const d = new Date(s.date + 'T00:00');
      let actions = '';
      if (s.status === 'SCHEDULED') {
        actions = `<button class="btn primary small" data-action="complete-session" data-id="${s.id}">Mark completed</button>
                   <button class="btn small" data-action="cancel-session" data-id="${s.id}">Cancel</button>`;
      } else if (s.status === 'COMPLETED') {
        actions = `<button class="btn primary small" data-action="rate-session" data-id="${s.id}" data-name="${esc(partner)}">Rate ${esc(partner.split(' ')[0])}</button>`;
      }
      return `<article class="item"><div style="display:flex;gap:14px;flex:1 1 320px;min-width:0">
          <div class="datebox"><b>${d.getDate()}</b><span>${esc(d.toLocaleDateString(undefined, { month: 'short' }))}</span></div>
          <div class="item-main"><div class="item-top">${avatar(partnerId, partner, 'sm')}<strong>${esc(partner)}</strong>${pill(s.status)}</div>
            <p>${esc(s.swapSummary.replace('<->', 'for'))}</p>
            <p class="muted">${esc(String(s.time).slice(0, 5))}, ${s.mode === 'ONLINE' ? 'online' : 'in person'}: ${linkOrText(s.locationOrLink)}</p></div></div>
          <div class="item-actions">${actions}</div></article>`;
    };
    $('#view').innerHTML = `
      <div class="page-head"><h1>Sessions</h1><p class="sub">Mark a session completed when it's done, then rate each other.</p></div>
      <div class="stack">${list.length ? list.map(card).join('') : '<div class="empty"><h3>No sessions yet</h3><p class="muted">Accept a swap request, then schedule a session from the Requests page.</p></div>'}</div>`;
  }

  function openRateModal(sessionId, name) {
    openModal(`
      <div class="modal-head"><div class="grow"><h2 id="modal-title">Rate ${esc(name)}</h2></div>
        <button class="x" data-action="close-modal" aria-label="Close">&times;</button></div>
      <form data-form="rate" data-session="${sessionId}">
        <div class="field"><label>How did it go? (1 is poor, 5 is excellent)</label>
          <div class="scale">${[1, 2, 3, 4, 5].map((n) => `<label><input type="radio" name="score" value="${n}" ${n === 5 ? 'checked' : ''}><span>${n}</span></label>`).join('')}</div></div>
        <div class="field"><label for="rt-fb">Feedback <span class="muted">(optional)</span></label><textarea id="rt-fb" name="feedback" maxlength="500"></textarea></div>
        <div class="modal-actions"><button type="button" class="btn" data-action="close-modal">Cancel</button><button type="submit" class="btn primary">Submit rating</button></div>
      </form>`);
  }

  /* ---------------- admin ---------------- */
  async function viewAdmin() {
    const tabs = [['users', 'Users'], ['skills', 'Skills'], ['reports', 'Reports'], ['stats', 'Statistics']];
    $('#view').innerHTML = `
      <div class="page-head"><h1>Admin panel</h1></div>
      <div class="subtabs" role="tablist">${tabs.map(([k, l]) => `<button role="tab" data-action="admin-tab" data-tab="${k}" aria-selected="${state.adminTab === k}">${l}</button>`).join('')}</div>
      <div id="admin-body"></div>`;
    const body = $('#admin-body');
    if (state.adminTab === 'users') {
      const users = await api('GET', '/api/admin/users');
      body.innerHTML = `<div class="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Department</th><th>Role</th><th>Rating</th><th>Status</th><th></th></tr></thead><tbody>
        ${users.map((u) => `<tr><td>${esc(u.name)}</td><td>${esc(u.email)}</td><td>${esc(u.department)}</td><td>${esc(u.role.toLowerCase())}</td>
          <td>${u.ratingCount ? Number(u.avgRating).toFixed(1) + ' (' + u.ratingCount + ')' : '-'}</td>
          <td>${pill(u.blocked ? 'BLOCKED' : 'ACTIVE')}</td>
          <td>${u.role === 'ADMIN' ? '' : `<button class="btn small ${u.blocked ? '' : 'danger'}" data-action="block-user" data-id="${u.id}" data-blocked="${u.blocked ? 'false' : 'true'}">${u.blocked ? 'Unblock' : 'Block'}</button>`}</td></tr>`).join('')}
        </tbody></table></div>`;
    } else if (state.adminTab === 'skills') {
      const skills = await ensureCatalog(true);
      body.innerHTML = `
        <form class="card" data-form="catalog"><h2>Add a skill to the catalog</h2>
          <div class="row"><div class="field"><label for="c-name">Skill name</label><input id="c-name" name="name" type="text" maxlength="80" required></div>
            <div class="field"><label for="c-cat">Category</label><input id="c-cat" name="category" type="text" maxlength="60" list="cats" placeholder="General">
              <datalist id="cats">${[...new Set(skills.map((s) => s.category))].map((c) => `<option value="${esc(c)}">`).join('')}</datalist></div></div>
          <button class="btn primary" type="submit">Add skill</button></form>
        <div class="table-wrap"><table><thead><tr><th>Skill</th><th>Category</th><th></th></tr></thead><tbody>
        ${skills.map((s) => `<tr><td>${esc(s.name)}</td><td>${esc(s.category)}</td><td><button class="btn small danger" data-action="delete-skill" data-id="${s.id}" data-name="${esc(s.name)}">Delete</button></td></tr>`).join('')}
        </tbody></table></div>`;
    } else if (state.adminTab === 'reports') {
      state.reports = await api('GET', '/api/admin/reports');
      body.innerHTML = state.reports.length ? `<div class="table-wrap"><table><thead><tr><th>Reported user</th><th>Reported by</th><th>Reason</th><th>Filed</th><th>Status</th><th></th></tr></thead><tbody>
        ${state.reports.map((r) => `<tr><td>${esc(r.reportedName)}</td><td>${esc(r.reporterName)}</td><td>${esc(r.reason)}</td><td>${esc(fmtDateTime(r.createdAt))}</td><td>${pill(r.status)}</td>
          <td>${r.status === 'OPEN' ? `<button class="btn small" data-action="resolve-report" data-id="${r.id}">Resolve</button><button class="btn small danger" data-action="block-resolve" data-id="${r.id}">Block and resolve</button>` : ''}</td></tr>`).join('')}
        </tbody></table></div>` : '<div class="empty"><p class="muted">No reports. The community is behaving.</p></div>';
    } else {
      const stats = await api('GET', '/api/admin/stats');
      body.innerHTML = `<div class="stats">${Object.entries(stats).map(([k, v]) => `<div class="stat"><b>${v}</b><span class="muted">${esc(k)}</span></div>`).join('')}</div>`;
    }
  }

  /* ---------------- form handlers ---------------- */
  const forms = {
    async login(form, fd) {
      state.me = await api('POST', '/api/auth/login', { email: fd.get('email'), password: fd.get('password') });
      enterApp();
    },
    async register(form, fd) {
      if (fd.get('password') !== fd.get('confirm')) { toast('Passwords do not match.', 'error'); return; }
      state.me = await api('POST', '/api/auth/register', {
        name: fd.get('name'), email: fd.get('email'), password: fd.get('password'), department: fd.get('department')
      });
      enterApp('profile');
      toast('Welcome! Add the skills you can teach and the ones you want to learn.', 'ok');
    },
    async filters() { await loadMatches(); },
    async profile(form, fd) {
      state.me = await api('PUT', '/api/profile', {
        name: fd.get('name'), department: fd.get('department'), bio: fd.get('bio'), availability: fd.getAll('slot')
      });
      toast('Profile saved.', 'ok');
    },
    async addskill(form, fd) {
      const skills = await api('POST', '/api/profile/skills', {
        skillId: Number(fd.get('skillId')), type: fd.get('type'), proficiency: Number(fd.get('level'))
      });
      paintSkills(skills);
      toast('Skill saved.', 'ok');
    },
    async swap(form, fd) {
      await api('POST', '/api/requests', {
        receiverId: Number(form.dataset.receiver), offeredSkillId: Number(fd.get('offered')),
        requestedSkillId: Number(fd.get('requested')), message: fd.get('message')
      });
      closeModal();
      toast('Swap request sent.', 'ok');
    },
    async report(form, fd) {
      await api('POST', '/api/reports', { userId: Number(form.dataset.user), reason: fd.get('reason') });
      closeModal();
      toast('Report sent. An administrator will review it.', 'ok');
    },
    async schedule(form, fd) {
      await api('POST', '/api/sessions', {
        requestId: Number(form.dataset.request), date: fd.get('date'), time: fd.get('time'),
        mode: fd.get('mode'), where: fd.get('where')
      });
      closeModal();
      toast('Session scheduled.', 'ok');
      go('sessions');
    },
    async rate(form, fd) {
      await api('POST', `/api/sessions/${form.dataset.session}/rate`, { score: Number(fd.get('score')), feedback: fd.get('feedback') });
      closeModal();
      toast('Thanks for your feedback.', 'ok');
    },
    async catalog(form, fd) {
      await api('POST', '/api/admin/skills', { name: fd.get('name'), category: fd.get('category') });
      toast('Skill added.', 'ok');
      viewAdmin();
    }
  };

  document.addEventListener('submit', (e) => {
    const form = e.target.closest('form[data-form]');
    if (!form) return;
    e.preventDefault();
    const handler = forms[form.dataset.form];
    if (!handler) return;
    const btn = form.querySelector('button[type=submit]');
    if (btn) btn.disabled = true;
    safe(() => handler(form, new FormData(form))).finally(() => { if (btn) btn.disabled = false; });
  });

  document.addEventListener('input', (e) => {
    if (e.target.matches && e.target.matches('#k-level')) $('#k-out').textContent = LEVELS[Number(e.target.value) - 1];
  });

  /* ---------------- click actions ---------------- */
  const post = (url) => api('POST', url);
  const actions = {
    'auth-tab': (el) => renderAuth(el.dataset.tab),
    logout: async () => { await safe(() => post('/api/auth/logout')); state.me = null; state.catalog = null; renderAuth(); },
    nav: (el) => { closeModal(); go(el.dataset.view); },
    'close-modal': closeModal,
    'reset-filters': () => { $('form[data-form=filters]').reset(); safe(loadMatches); },
    'view-user': (el) => safe(() => openUserModal(Number(el.dataset.id))),
    'request-swap': (el) => safe(() => openSwapModal(Number(el.dataset.id))),
    'report-user': (el) => openReportModal(Number(el.dataset.id), el.dataset.name),
    'remove-skill': (el) => safe(async () => {
      paintSkills(await api('DELETE', '/api/profile/skills/' + el.dataset.id));
    }),
    'accept-request': (el) => safe(async () => { await post(`/api/requests/${el.dataset.id}/accept`); toast('Accepted. Now schedule a session.', 'ok'); await viewRequests(); refreshBadge(); }),
    'reject-request': (el) => safe(async () => { await post(`/api/requests/${el.dataset.id}/reject`); await viewRequests(); refreshBadge(); }),
    'cancel-request': (el) => { if (confirm('Cancel this request?')) safe(async () => { await post(`/api/requests/${el.dataset.id}/cancel`); await viewRequests(); }); },
    schedule: (el) => openScheduleModal(Number(el.dataset.id), el.dataset.label),
    'complete-session': (el) => safe(async () => { await post(`/api/sessions/${el.dataset.id}/complete`); toast('Marked completed. Rate your partner next.', 'ok'); await viewSessions(); }),
    'cancel-session': (el) => { if (confirm('Cancel this session?')) safe(async () => { await post(`/api/sessions/${el.dataset.id}/cancel`); await viewSessions(); }); },
    'rate-session': (el) => openRateModal(Number(el.dataset.id), el.dataset.name),
    'admin-tab': (el) => { state.adminTab = el.dataset.tab; safe(viewAdmin); },
    'block-user': (el) => safe(async () => {
      await api('POST', `/api/admin/users/${el.dataset.id}/block`, { blocked: el.dataset.blocked === 'true' });
      await viewAdmin();
    }),
    'delete-skill': (el) => {
      if (confirm(`Delete "${el.dataset.name}"? It will be removed from every profile.`)) {
        safe(async () => { await api('DELETE', '/api/admin/skills/' + el.dataset.id); await viewAdmin(); });
      }
    },
    'resolve-report': (el) => safe(async () => { await post(`/api/admin/reports/${el.dataset.id}/resolve`); await viewAdmin(); }),
    'block-resolve': (el) => {
      const r = state.reports.find((x) => x.id === Number(el.dataset.id));
      if (r && confirm(`Block ${r.reportedName} and resolve this report?`)) {
        safe(async () => {
          await api('POST', `/api/admin/users/${r.reportedId}/block`, { blocked: true });
          await post(`/api/admin/reports/${r.id}/resolve`);
          await viewAdmin();
        });
      }
    }
  };

  document.addEventListener('click', (e) => {
    const el = e.target.closest('[data-action]');
    if (!el) return;
    const fn = actions[el.dataset.action];
    if (fn) fn(el);
  });

  /* ---------------- boot ---------------- */
  api('GET', '/api/auth/me')
    .then((me) => { state.me = me; enterApp(); })
    .catch(() => renderAuth());
})();
