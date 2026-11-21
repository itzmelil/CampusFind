const app = document.querySelector('#app');
const state = { status: 'all', category: 'All items', query: '', draft: null };
let renderVersion = 0;

async function api(path, fields) {
  const options = fields ? { method: 'POST', body: new URLSearchParams(fields) } : {};
  const response = await fetch(path, options);
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || 'CampusFind could not complete this request.');
  return result;
}

function showError(message) {
  const parent = app.querySelector('main') || app;
  let notice = parent.querySelector('.request-error');
  if (!notice) {
    notice = document.createElement('p');
    notice.className = 'request-error surface';
    notice.setAttribute('role', 'alert');
    parent.prepend(notice);
  }
  notice.textContent = message;
}
const escape = value => String(value ?? '').replace(/[&<>"']/g, char => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
})[char]);
const icon = (index, extra = '') => `<img class="icon ${extra}" src="./assets/icon-${index}.png" alt="">`;
const route = () => location.hash.slice(1).split('/');
const url = (scope, panel, arg = '') => '#' + [scope, panel, arg].filter(Boolean).join('/');
const link = (label, destination, css = '') => `<a class="${css}" href="${destination}">${label}</a>`;
const eyebrow = value => `<p class="eyebrow">${value}</p>`;
const intro = (tag, title, description) => `<header class="intro">${eyebrow(tag)}<h1>${title}</h1><p>${description}</p></header>`;
const group = (...buttons) => `<div class="actions">${buttons.join('')}</div>`;
const cta = (label, scope, panel, arg, secondary = false) => link(label, url(scope, panel, arg), 'button ' + (secondary ? 'secondary' : 'primary'));
const safeCard = scope => `<section class="safe-card">${icon(29)}<h3>Return it safely</h3><p>Arrange pickup or drop-off at a staffed campus location.</p>${link('View locations ' + icon(30), url(scope, 'safety'))}</section>`;
const navOffice = [['home', 'Overview', 1], ['board', 'Browse items', 2], ['report', 'Report item', 3], ['alerts', 'My alerts', 4], ['safety', 'Safety & privacy', 5]];
const navStudent = [['home', 'Student home', 1], ['board', 'Campus board', 2], ['mine', 'My reports', 3], ['alerts', 'Alerts', 4], ['safety', 'Safety', 5]];

function sidebar(scope, panel) {
  const student = scope === 'student';
  const activePanel = panel === 'preview' || panel === 'report' ? student ? 'mine' : 'report' : panel;
  const links = (student ? navStudent : navOffice).map(([key, title, glyph]) =>
    `<a href="${url(scope, key)}" ${key === activePanel ? 'aria-current="page"' : ''}>${icon(glyph)}${title}</a>`).join('');
  return `<aside class="sidebar">
    <a class="brand" href="${url(scope, 'home')}"><span class="brand-icon">${icon(0)}</span><span>CampusFind<small>${student ? 'Student access' : 'UM Matina'}</small></span></a>
    <nav aria-label="${student ? 'Student' : 'Office'} navigation">${links}</nav>
    <div class="sidebar-bottom">
      <section class="handoff"><strong>${icon(6)} Safe handoffs</strong><p>Confirm a staffed handoff point with campus personnel before returning an item.</p>${link('View locations ' + icon(7), url(scope, 'safety'))}</section>
      ${student ? '<button class="account" data-action="logout">' + icon(8) + ' Log out</button>' : link(icon(8) + ' Student sign in', url('office', 'login'), 'account')}
    </div>
  </aside>`;
}

function activity(scope, reports, own) {
  const student = scope === 'student';
  const stats = student
    ? [[own.length, 'My reports', 'mine'], [0, 'Pending claims', 'mine'], [0, 'Unread alerts', 'alerts'], [0, 'Active handoffs', 'mine']]
    : [[reports.length, 'Active reports', 'board'], [1, 'Items returned', 'board']];
  return `<aside class="activity" aria-label="Activity">
    ${student ? '' : `<div class="rail-top"><a class="bell" href="${url(scope, 'alerts')}" aria-label="View alerts">${icon(24)}</a>${link('Student sign in', url('office', 'login'))}</div>`}
    ${eyebrow(student ? 'Student workspace' : 'Live overview')}<h2>${student ? 'Your activity' : 'Campus pulse'}</h2>
    <div class="stats">${stats.map(([number, label, target], index) => link(`${student ? '' : '<span class="stat-icon">' + icon(26 + index) + '</span>'}<strong>${number}</strong><small>${label}</small>`, url(scope, target), 'stat')).join('')}</div>
    ${student ? '' : `<div class="subhead"><h3>Latest reports</h3>${link('View all →', url(scope, 'board'))}</div>
      <div class="latest">${reports.slice(0, 3).map(report => link(`${photo(report)}<span><strong>${escape(report.name)}</strong><small>${escape(report.location)}</small></span><b>${icon(28)}</b>`, url(scope, 'item', report.id))).join('')}</div>`}
    ${safeCard(scope)}
  </aside>`;
}

const photo = report => report.image ? `<img class="photo" src="${report.image}" alt="">` : '<span class="photo placeholder">+</span>';
const reportCard = (scope, report) => `<a class="item" href="${url(scope, 'item', report.id)}">
  <span class="item-photo">${photo(report)}<span class="badge ${report.type}">${report.type}</span></span>
  <span class="item-copy">${eyebrow(escape(report.category))}<strong>${escape(report.name)}</strong><small>${icon(22)} ${escape(report.location)}</small><small>${icon(23)} ${report.id.startsWith('sample-') ? '4h ago' : 'Your report'}</small></span>
  <span aria-hidden="true">${icon(21)}</span>
</a>`;

function board(scope, { categories, visible }, home = false) {
  const filter = (kind, value, label, index) => `<button class="chip" type="button" data-filter="${kind}" data-value="${value}" aria-pressed="${state[kind] === value}">${index == null ? '' : icon(index)}${label}</button>`;
  return `${home ? '' : intro('Campus board', scope === 'student' ? 'Campus board' : 'Browse items', 'Explore lost and found reports by category or status.')}
    <section class="board" aria-label="Item listings">
      ${eyebrow('Campus activity')}<div class="subhead"><h2>${home ? 'Recently reported' : 'Latest reports'}</h2>
      <div class="segmented">${['all', 'found', 'lost'].map(value => filter('status', value, value)).join('')}</div></div>
      <div class="filters">${['All items', ...categories].map((value, index) => filter('category', value, value, 13 + index)).join('')}</div>
      ${state.query ? `<p class="query">Results for “${escape(state.query)}” <button data-action="clear">Clear search</button></p>` : ''}
      <div class="items">${visible.length ? visible.map(report => reportCard(scope, report)).join('') : '<p class="surface">No items match your search.</p>'}</div>
    </section>`;
}

function home(scope, boardData) {
  const student = scope === 'student';
  return `<section class="hero">
    <div class="hero-copy"><span class="hero-tag">${icon(12)} ${student ? 'Student workspace' : 'Community powered'}</span>
    <h1>${student ? 'Welcome, LYLE<br>Find and return items.' : 'Lost something?<br>Your campus is looking.'}</h1>
    <p>${student ? 'Report, claim, and recover campus items through the Lost & Found Office.' : 'Report missing items and connect with verified finders at the University of Mindanao, Matina campus in Davao City.'}</p>
    ${group(cta('I lost something', scope, 'report', 'lost', true), link('I found something', url(scope, 'report', 'found'), 'button glass'))}</div>
    <div class="hero-art" aria-hidden="true"><img src="./assets/item-1.png" alt=""><img src="./assets/item-2.png" alt=""><span>${icon(11)}</span></div>
  </section>${student ? `<section class="tools"><h2>Student tools</h2><div class="tiles">${[
    ['board', 'Campus board', 'Browse lost and found items on campus.'], ['mine', 'My reports', 'Track your submissions, claims, and handoffs.'],
    ['alerts', 'Alerts', 'Check for updates about your reports and claims.'], ['safety', 'Safety', 'Find staffed handoff locations and privacy tips.'],
  ].map(([target, title, detail]) => link(`<strong>${title}</strong><b>→</b><small>${detail}</small>`, url(scope, target))).join('')}</div></section>` : board(scope, boardData, true)}`;
}

function report(scope, type, categories) {
  const found = type === 'found';
  const draft = state.draft || {};
  return intro('Campus lost & found', 'Report an item', 'Share enough detail for the Lost & Found Office to identify the item.') +
    `<form class="surface report-form" id="report-form">
      <h2>What happened?</h2>${group(cta('I lost an item', scope, 'report', 'lost', found), cta('I found an item', scope, 'report', 'found', !found))}
      <input name="type" type="hidden" value="${type}">
      <label>Item name<input name="name" required maxlength="100" value="${escape(draft.name)}" placeholder="${found ? 'e.g. Blue game controller' : 'e.g. Black canvas backpack'}"></label>
      <div class="form-grid"><label>Category<select name="category" required><option value="">Select a category</option>${categories.map(value => `<option ${value === draft.category ? 'selected' : ''}>${value}</option>`).join('')}</select></label>
      <label>${found ? 'Date found' : 'Date last seen'}<input type="date" name="date" required value="${escape(draft.date)}"></label></div>
      <label>${found ? 'Found at' : 'Last seen at'}<input name="location" required maxlength="120" value="${escape(draft.location)}" placeholder="Building, room, or campus location"></label>
      <label>Description<textarea name="description" required maxlength="500" placeholder="Add identifying details you can share with the office">${escape(draft.description)}</textarea></label>
      ${group('<button class="button primary" type="submit">Preview report</button>', cta('Cancel', scope, scope === 'student' ? 'mine' : 'home', '', true))}
    </form><p class="fineprint">Keep names, student IDs, and other personal information out of your report.</p>`;
}

function preview(scope, type) {
  if (!state.draft || state.draft.type !== type) {
    return intro('Report preview', 'Start a report', 'Fill in the report form to preview your item.') + cta('Open report form', scope, 'report', type);
  }
  return intro('Report preview', `Preview ${type} report`, 'Review how the item will appear on the campus board.') +
    itemSummary(state.draft, group(cta('Back to report', scope, 'report', type, true), '<button class="button primary" data-action="save">Submit report</button>'));
}

function itemSummary(report, actions) {
  return `<article class="surface item-summary">
    ${report.image ? photo(report) : ''}${eyebrow(escape(report.type) + ' · ' + escape(report.category))}
    <h2>${escape(report.name)}</h2>
    <dl><dt>Campus location</dt><dd>${escape(report.location)}</dd>
    ${report.date ? '<dt>Date</dt><dd>' + escape(report.date) + '</dd>' : ''}
    <dt>Description</dt><dd>${escape(report.description)}</dd></dl>${actions}</article>`;
}

function safety(locations) {
  return intro('Safe handoffs', 'Safety & privacy', 'Use staffed locations and protect your personal details when reporting or returning items.') +
    `<section class="surface guidance"><h3>Report privately</h3><p>Keep student IDs, addresses, and contact details out of public descriptions.</p>
    <h3>Verify ownership</h3><p>Use identifying details to verify ownership with the Lost & Found Office.</p>
    <h3>Meet at a staffed location</h3><p>Confirm pickup with the office before bringing or collecting an item.</p></section>
    <section class="surface"><h2>Staffed handoff locations</h2><p>Illustrative locations at Matina campus. Ask University of Mindanao staff to confirm approved handoff points and hours.</p>
    <ul class="location-list">${locations.map(location => `<li>${location}</li>`).join('')}</ul></section>`;
}

function panelContent(scope, panel, arg, boardData) {
  const { reports, own, categories, locations } = boardData;
  const type = arg === 'found' ? 'found' : 'lost';
  switch (panel) {
    case 'board': return board(scope, boardData);
    case 'report': return report(scope, type, categories);
    case 'preview': return preview(scope, type);
    case 'safety': return safety(locations);
    case 'login': return intro('University of Mindanao · Matina', 'Student sign in', 'Open your student workspace to manage reports and alerts.') +
      `<section class="surface"><h2>Student workspace</h2><p>Explore the student flow using a demo profile.</p>
      ${group(cta('Open Student demo', 'student', 'home'), cta('Back to overview', 'office', 'home', '', true))}</section>`;
    case 'alerts': return intro('Student updates', scope === 'student' ? 'Alerts' : 'My alerts', 'Check match suggestions, claim decisions, and staffed handoff updates.') +
      `<section class="surface"><h2>You’re all caught up</h2><p>Updates from the Lost & Found Office will appear here when there’s activity on your reports or claims.</p>
      ${group(cta('View my reports', scope, scope === 'student' ? 'mine' : 'login'), cta('Browse items', scope, 'board', '', true))}</section>`;
    case 'mine': return intro('Student workspace', 'My reports & claims', 'Track your reports, ownership claims, and staffed handoffs.') +
      (own.length ? `<div class="items">${own.map(entry => reportCard(scope, entry)).join('')}</div>${cta('New report', scope, 'report', 'lost')}` :
      `<section class="surface"><h2>No reports or claims yet</h2><p>Your reports will appear here as soon as you create one.</p>${group(cta('New report', scope, 'report', 'lost'), cta('Browse campus board', scope, 'board', '', true))}</section>`);
    case 'item': {
      const item = reports.find(entry => entry.id === arg);
      return item ? intro('Campus board', 'Item details', 'Review the report and find a safe handoff location.') +
        itemSummary(item, group(cta('View handoff locations', scope, 'safety'), cta('Back to campus board', scope, 'board', '', true))) :
        intro('Campus board', 'Item unavailable', 'Return to the campus board to find available reports.') + cta('Browse items', scope, 'board');
    }
    default: return home(scope, boardData);
  }
}

async function render() {
  const version = ++renderVersion;
  const [area, page, arg] = route();
  const scope = area === 'student' ? 'student' : 'office';
  const panel = [...navOffice, ...navStudent].some(([key]) => key === page) || ['login', 'preview', 'item'].includes(page) ? page : 'home';
  let boardData;
  try {
    const filters = new URLSearchParams({ query: state.query, status: state.status, category: state.category });
    boardData = await api('/api/board?' + filters);
  } catch (error) {
    if (version === renderVersion) showError(error.message);
    return;
  }
  if (version !== renderVersion) return;
  app.innerHTML = `<div class="shell">${sidebar(scope, panel)}<main id="main">
    <header class="toolbar"><form id="search-form" role="search">${icon(9)}<input name="query" type="search" aria-label="Search campus reports" placeholder="Search item, category or campus location" value="${escape(state.query)}"></form>
    ${cta(icon(10) + ' Report item', scope, 'report', 'lost')}</header>
    ${panelContent(scope, panel, arg, boardData)}</main>${activity(scope, boardData.reports, boardData.own)}</div>`;
}

window.addEventListener('hashchange', () => {
  render();
  window.scrollTo(0, 0);
});

app.addEventListener('submit', async event => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  const [area] = route();
  const scope = area === 'student' ? 'student' : 'office';
  if (event.target.id === 'search-form') {
    state.query = values.query;
    if (location.hash === url(scope, 'board')) render();
    else location.hash = url(scope, 'board');
  }
  if (event.target.id === 'report-form') {
    const submit = event.submitter;
    if (submit) submit.disabled = true;
    try {
      state.draft = await api('/api/preview', values);
      if (event.target.isConnected) location.hash = url(scope, 'preview', values.type);
    } catch (error) {
      if (event.target.isConnected) showError(error.message);
    } finally {
      if (submit) submit.disabled = false;
    }
  }
});

app.addEventListener('click', async event => {
  const target = event.target.closest('button');
  if (!target) return;
  if (target.dataset.filter) {
    state[target.dataset.filter] = target.dataset.value;
    await render();
    app.querySelector(`[data-filter="${target.dataset.filter}"][data-value="${target.dataset.value}"]`)?.focus();
  }
  if (target.dataset.action === 'clear') {
    state.query = '';
    render();
  }
  if (target.dataset.action === 'logout') {
    state.draft = null;
    location.hash = url('office', 'home');
  }
  if (target.dataset.action === 'save' && state.draft) {
    target.disabled = true;
    try {
      const scope = route()[0] === 'student' ? 'student' : 'office';
      await api('/api/reports', state.draft);
      state.draft = null;
      state.status = 'all';
      state.category = 'All items';
      state.query = '';
      location.hash = url(scope, scope === 'student' ? 'mine' : 'board');
    } catch (error) {
      showError(error.message);
    } finally {
      target.disabled = false;
    }
  }
});

render();
