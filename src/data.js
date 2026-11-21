export const categories = ['Electronics', 'Bags', 'Keys', 'Books', 'Bottles', 'Clothing', 'Other'];

export const samples = [
  ['Blue game controller', 'Other', 'found', 'Dorm Commons · Building C'],
  ['Black leather shoulder bag', 'Bags', 'lost', 'Campus Café · South Wing'],
  ['Over-ear wired headphones', 'Electronics', 'found', 'Arts Building · Courtyard'],
  ['Phone with clear case', 'Electronics', 'lost', 'Science Center · Room 204'],
  ['Black canvas backpack', 'Bags', 'found', 'Student Union · East Hall'],
  ['Midnight blue headphones', 'Electronics', 'found', 'Main Library · Level 2'],
].map(([name, category, type, location], index) => ({
  id: 'sample-' + (index + 1), name, category, type, location,
  image: './assets/item-' + (index + 1) + '.png',
  description: 'Contact the Lost & Found Office to verify identifying details and arrange a staffed handoff.',
}));

export const locations = ['Main Library', 'Student Union', 'Science Center', 'Dorm Commons'];

export function readReports() {
  try {
    const saved = JSON.parse(localStorage.getItem('campusfind.reports') || '[]');
    return Array.isArray(saved) ? saved : [];
  } catch {
    return [];
  }
}

export function saveReport(draft) {
  const report = { ...draft, id: crypto.randomUUID() };
  localStorage.setItem('campusfind.reports', JSON.stringify([report, ...readReports()]));
  return report;
}

export function filterReports(reports, { status, category, query }) {
  return reports.filter(report => (
    (status === 'all' || report.type === status)
    && (category === 'All items' || report.category === category)
    && [report.name, report.category, report.location].join(' ').toLowerCase().includes(query.toLowerCase().trim())
  ));
}
