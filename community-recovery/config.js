/* config.js — the one place this kit is told where it lives.
   Another recovery organisation changes `beach` to its own place, and `place` to its own name. */
window.CR = {
  place: 'Community Recovery',
  // the place on the beach: every block these pages read and write stands here
  beach: 'https://beach.happyseaurchin.com/w/community-recovery',
  // the families a person has a notebook in
  families: ['constitution', 'agreement', 'standards'],
  // which page opens which block (the front door reads its doors from the lighthouse block)
  pages: {
    'spine:agreement': 'join.html',
    'spine:constitution': 'constitution.html',
    'spine:standards': 'standards.html',
    'pool:community-recovery': '#room'
  },
  // readings that stand elsewhere on the beach and are shown beside the ones made here
  also: {
    standards: { beach: 'https://beach.happyseaurchin.com', family: 'community-recovery-standards' }
  },
  // where these pages are published, printed on a person's card
  home: 'https://phenomemental.github.io/happyseaurchin-beach/community-recovery/'
};
