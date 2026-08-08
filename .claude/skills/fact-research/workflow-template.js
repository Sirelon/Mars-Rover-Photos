// Template for the fact-research Workflow. Copy, then:
//  1. Replace EXCLUDE with the current educational_facts_v2 fact texts (see SKILL.md step 1).
//  2. Adjust TOPICS if the user asked for specific areas.
// Models/effort are deliberate (see SKILL.md) — do not remove them.
export const meta = {
  name: 'fact-research',
  description: 'Research, score, and categorize Mars/rover facts with source links',
  phases: [
    { title: 'Research', detail: 'web research per topic, facts with source links', model: 'haiku' },
    { title: 'Score', detail: 'credibility/readability/interest scoring per topic', model: 'haiku' },
    { title: 'Merge', detail: 'dedupe within batch, sectioning, cap per section', model: 'sonnet' },
  ],
}

const EXCLUDE = [
  // '<inline current collection fact texts here>',
]
const excludeList = EXCLUDE.map(t => `- ${t}`).join('\n')
const excludeBlock = EXCLUDE.length
  ? `\n- Do NOT duplicate or trivially rephrase any of these facts already in the app:\n${excludeList}`
  : ''

const TOPICS = [
  { key: 'mars-planet', focus: 'The planet Mars itself: geology, atmosphere, climate, moons, seasons, size, gravity, day/year length, water history, comparisons with Earth.' },
  { key: 'early-rovers', focus: 'Early Mars surface missions: Mars Pathfinder and the Sojourner rover, Spirit and Opportunity (Mars Exploration Rovers) — their journeys, discoveries, records, and fates.' },
  { key: 'curiosity-insight', focus: 'The Curiosity rover (Mars Science Laboratory) and the InSight lander: instruments, discoveries, landing (sky crane), marsquakes, ongoing findings.' },
  { key: 'perseverance-ingenuity', focus: 'The Perseverance rover, the Ingenuity helicopter, and the Mars Sample Return effort: landing, sample tubes, flights, MOXIE, microphones, discoveries in Jezero Crater.' },
  { key: 'rover-photography', focus: 'How Mars rovers take and send photos: cameras (Mastcam, Navcam, Hazcam, WATSON, SuperCam), famous images, image transmission via orbiters, panoramas, color calibration targets, why selfies have no visible arm. This is the most important topic — the app is about Mars rover photos.' },
  { key: 'exploration-history', focus: 'Mars exploration history beyond rovers: Mariner 4 first flyby, Viking landers, famous failures and the "Mars curse", international missions (ESA, China Zhurong, India Mangalyaan, UAE Hope), orbiters, records and firsts.' },
  { key: 'future-humans', focus: 'The future of Mars exploration: plans for human missions, challenges (radiation, dust, communication delay, travel time), Mars Sample Return status, growing food, habitats.' },
]

const FACTS_SCHEMA = {
  type: 'object',
  properties: {
    facts: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          text: { type: 'string', description: 'The fact, 1-2 short sentences, plain language for a general audience' },
          source_url: { type: 'string' },
          source_name: { type: 'string', description: 'e.g. NASA JPL, ESA, Smithsonian' },
        },
        required: ['text', 'source_url', 'source_name'],
      },
    },
  },
  required: ['facts'],
}

const SCORED_SCHEMA = {
  type: 'object',
  properties: {
    facts: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          text: { type: 'string' },
          source_url: { type: 'string' },
          source_name: { type: 'string' },
          credibility: { type: 'number', description: '0-50' },
          readability: { type: 'number', description: '0-25' },
          simplicity: { type: 'number', description: '0-25' },
          score: { type: 'number', description: 'credibility+readability+simplicity, 0-100' },
          interest: { type: 'number', description: '0-100, how interesting/surprising for a casual user' },
        },
        required: ['text', 'source_url', 'source_name', 'credibility', 'readability', 'simplicity', 'score', 'interest'],
      },
    },
  },
  required: ['facts'],
}

const FINAL_SCHEMA = {
  type: 'object',
  properties: {
    sections: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          name: { type: 'string' },
          facts: {
            type: 'array',
            items: {
              type: 'object',
              properties: {
                text: { type: 'string' },
                source_url: { type: 'string' },
                source_name: { type: 'string' },
                score: { type: 'number' },
                interest: { type: 'number' },
              },
              required: ['text', 'source_url', 'source_name', 'score', 'interest'],
            },
          },
        },
        required: ['name', 'facts'],
      },
    },
  },
  required: ['sections'],
}

const researchPrompt = (topic) => `You are researching facts for a Mars rover photos mobile app that shows short educational facts to casual users. Every fact will be displayed with a clickable source link, so the link matters as much as the fact.

TOPIC: ${topic.focus}

Use WebSearch and WebFetch to find 20-30 TRUE, interesting facts on this topic. Rules:
- Every fact MUST have a source URL you actually found via search — a page that genuinely states the fact. Strongly prefer primary/credible sources: nasa.gov, jpl.nasa.gov, mars.nasa.gov, science.nasa.gov, esa.int, space agencies, universities, Britannica, major science outlets. Never invent a URL.
- Each fact: 1-2 short sentences, plain language a teenager understands. No jargon without a one-word gloss.
- Mix foundational facts with surprising, delightful, concrete ones. Numbers and superlatives are good.${excludeBlock}
- Quality over quantity: if you can only verify 15 good facts, return 15. Do not pad with weak or dubious facts.

Return the structured output only.`

const scorePrompt = (research, topic) => `You are scoring candidate educational facts for a Mars rover photos app. For each fact below, assess and score:
- credibility (0-50, THE major factor): Is the source authoritative (nasa.gov/jpl/esa = 45-50, major science outlets = 35-44, other = lower)? Does the fact match established knowledge? Spot-check with WebFetch/WebSearch any fact that seems off, and at least 3 facts overall. If a fact appears false or the source doesn't support it, set credibility below 15.
- readability (0-25): clear, well-worded, flows nicely.
- simplicity (0-25): understandable by a casual user with no astronomy background; penalize jargon and complexity.
- score = credibility + readability + simplicity.
- interest (0-100): how surprising/delightful for a casual user scrolling Mars photos.

You may lightly rewrite a fact's text for readability (keep it true to the source). Facts to score (topic: ${topic.key}):
${JSON.stringify(research.facts, null, 2)}

Return the structured output only.`

phase('Research')
const scoredPerTopic = await pipeline(
  TOPICS,
  t => agent(researchPrompt(t), { label: `research:${t.key}`, phase: 'Research', schema: FACTS_SCHEMA, model: 'haiku', effort: 'medium' }),
  (research, t) => {
    if (!research || !research.facts || research.facts.length === 0) return null
    return agent(scorePrompt(research, t), { label: `score:${t.key}`, phase: 'Score', schema: SCORED_SCHEMA, model: 'haiku', effort: 'low' })
  }
)

const allScored = scoredPerTopic
  .filter(Boolean)
  .flatMap((r, i) => (r.facts || []).map(f => ({ ...f, topic: TOPICS[i] ? TOPICS[i].key : 'unknown' })))

log(`${allScored.length} scored candidate facts collected`)

phase('Merge')
const final = await agent(`You are finalizing a curated fact table for a Mars rover photos app. Below are scored candidate facts from ${TOPICS.length} research agents. Your job:
1. Remove duplicates and near-duplicates WITHIN this batch only (keep the higher-scored one).
2. Drop any fact with score below 60 or credibility below 25.
3. Group facts into clear reader-facing sections (prefer the app's canonical sections: "Mars: The Planet", "Water & Ancient Mars", "Rover Missions", "Curiosity & InSight", "Perseverance & Ingenuity", "Photography & Cameras", "Exploration History & Firsts", "Humans on Mars" — adjust only if the material demands it).
4. Within each section sort by score descending. Hard cap: 50 facts per section — but NEVER pad; keep only genuinely good facts.
5. Keep each fact's text, source_url, source_name, score, interest exactly as given (you may fix obvious typos).

Candidates:
${JSON.stringify(allScored, null, 2)}

Return the structured output only.`, { label: 'merge-and-section', phase: 'Merge', schema: FINAL_SCHEMA, model: 'sonnet', effort: 'high' })

const total = final.sections.reduce((n, s) => n + s.facts.length, 0)
log(`Final: ${final.sections.length} sections, ${total} facts`)
return final
