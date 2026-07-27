import fs from "node:fs/promises";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const OUT = "/Users/kieransahoo/Ipsator/Impulse/Impulse_AI_RAG_Architecture_App_Theme.pptx";
const RENDER = "/Users/kieransahoo/Ipsator/Impulse/artifacts/impulse-architecture-build/rendered";
const HERO = "/Users/kieransahoo/Ipsator/Impulse/artifacts/impulse-architecture-build/assets/content-to-knowledge-impulse-theme.png";

const C = {
  ink: "#24231F", muted: "#6D6A63", rule: "#DFDBD0", panel: "#F0EDE5",
  blue: "#CC4824", pale: "#F4DACF", light: "#F8E7E0", white: "#FFFEFA",
  green: "#26734D", amber: "#806C52", paper: "#F7F5EF", lime: "#D6EE72",
  dark: "#494640",
};
const FONT = "Arial";
const DISPLAY_FONT = "Georgia";

function box(slide, name, x, y, w, h, fill = C.white, line = C.rule, radius = 8) {
  return slide.shapes.add({
    geometry: radius ? "roundRect" : "rect", name,
    position: { left: x, top: y, width: w, height: h },
    fill, line: { style: "solid", fill: line, width: 1.5 },
    ...(radius ? { borderRadius: radius } : {}),
  });
}

function text(slide, name, value, x, y, w, h, size = 24, color = C.ink, bold = false, align = "left", font = FONT) {
  const s = slide.shapes.add({
    geometry: "textbox", name,
    position: { left: x, top: y, width: w, height: h },
    fill: "none", line: { style: "solid", fill: "none", width: 0 },
  });
  s.text = value;
  s.text.style = {
    fontSize: size, typeface: font, color, bold, alignment: align,
    verticalAlignment: "middle", autoFit: "shrinkText",
  };
  return s;
}

function title(slide, value, n) {
  text(slide, `slide-${n}-title`, value, 42, 28, 1150, 72, 38, C.ink, false, "left", DISPLAY_FONT);
  text(slide, `slide-${n}-number`, String(n).padStart(2, "0"), 1188, 660, 50, 22, 13, C.muted, false, "right");
}

function arrow(slide, name, x, y, w, h, fill = C.blue) {
  return slide.shapes.add({
    geometry: "rightArrow", name,
    position: { left: x, top: y, width: w, height: h },
    fill, line: { style: "solid", fill, width: 0 },
  });
}

function node(slide, name, x, y, w, h, head, body, accent = C.blue, fill = C.white) {
  box(slide, `${name}-box`, x, y, w, h, fill, C.rule, 8);
  slide.shapes.add({
    geometry: "rect", name: `${name}-accent`,
    position: { left: x, top: y, width: 8, height: h },
    fill: accent, line: { style: "solid", fill: accent, width: 0 },
  });
  text(slide, `${name}-head`, head, x + 22, y + 13, w - 34, 32, 21, C.ink, true);
  text(slide, `${name}-body`, body, x + 22, y + 48, w - 34, h - 58, 16, C.muted);
}

function notes(slide, sources, talk) {
  slide.speakerNotes.textFrame.setText(`${talk}\n\n[Sources]\n${sources.map(s => `- ${s}`).join("\n")}`);
}

const p = Presentation.create({ slideSize: { width: 1280, height: 720 } });

// Slide 1 — cover image field
{
  const s = p.slides.add();
  s.background.fill = C.paper;
  const imageBytes = await fs.readFile(HERO);
  s.images.add({
    blob: imageBytes, contentType: "image/png", alt: "Online content flowing through AI into organized vector knowledge",
    fit: "cover", position: { left: 560, top: 0, width: 720, height: 720 },
  });
  text(s, "cover-kicker", "IMPULSE • PERSONAL KNOWLEDGE + PLANNING", 44, 48, 500, 34, 17, C.blue, true);
  text(s, "cover-title", "Impulse", 44, 154, 470, 82, 64, C.ink, false, "left", DISPLAY_FONT);
  text(s, "cover-tagline", "Save once. Understand once.\nUse it when it matters.", 44, 240, 500, 120, 36, C.ink, false, "left", DISPLAY_FONT);
  text(s, "cover-subtitle", "Saved videos, posts and web pages become searchable memories, grounded answers and practical plans.", 44, 382, 490, 90, 22, C.muted);
  text(s, "cover-team-label", "TEAM", 44, 548, 90, 24, 16, C.blue, true);
  text(s, "cover-team", "Kiran Kumar Sahoo  •  Apurvaa Avinandita  •  Jagadheshvar B P", 44, 576, 490, 54, 17, C.ink, true);
  notes(s, ["Generated visual: OpenAI ImageGen; prompt recorded in task history."],
    "Impulse solves a simple problem: useful ideas are scattered across saved links. The application turns those sources into structured, searchable memory and grounded answers.");
}

// Slide 2 — problem and solution
{
  const s = p.slides.add();
  s.background.fill = C.paper;
  title(s, "A saved link is not yet useful knowledge", 2);
  text(s, "problem-lead", "Customer problem", 44, 122, 250, 32, 22, C.muted, true);
  text(s, "problem-big", "Links are fragmented.\nDetails are forgotten.\nGeneric AI lacks context.", 44, 168, 480, 190, 35, C.ink, true);
  arrow(s, "problem-arrow", 555, 250, 120, 44, C.blue);
  text(s, "solution-lead", "How Impulse solves it", 710, 122, 300, 32, 22, C.blue, true);
  node(s, "solution-1", 710, 170, 490, 92, "Understand once", "Extract compact facts, topics and useful actions.", C.blue, C.light);
  node(s, "solution-2", 710, 282, 490, 92, "Store for retrieval", "Keep human-readable data and its vector meaning.", C.blue, C.light);
  node(s, "solution-3", 710, 394, 490, 92, "Answer with evidence", "Retrieve relevant memories before the final LLM responds.", C.blue, C.light);
  box(s, "boundary", 44, 520, 1156, 92, C.panel, C.panel, 0);
  text(s, "boundary-text", "Target customers: students, creators, travelers and professionals who save useful content but cannot find or reuse it when needed.", 72, 535, 1100, 60, 20, C.ink, false);
  notes(s, [
    "docs/01-product-overview.md",
    "docs/10-browser-extension.md",
    "apps/frontend/app/src/main/java/com/impulse/ui/main/MainScreen.kt",
  ], "Saving a URL alone does not preserve meaning. The extension captures content, the Android application provides the user experience, and both rely on the same backend memory pipeline.");
}

// Slide 3 — ingestion architecture
{
  const s = p.slides.add();
  s.background.fill = C.paper;
  title(s, "One end-to-end flow turns a URL into searchable memory", 3);
  text(s, "ingest-caption", "Capture → trusted backend → AI understanding → searchable memory", 44, 100, 900, 32, 20, C.muted);
  // arrows first
  arrow(s, "a1", 225, 282, 55, 36);
  arrow(s, "a2", 474, 282, 55, 36);
  arrow(s, "a3", 723, 282, 55, 36);
  arrow(s, "a4", 972, 282, 55, 36);
  node(s, "capture", 44, 208, 181, 184, "1  Capture", "Chrome or Android sends URL, visible content and user note.", C.blue);
  node(s, "backend", 280, 208, 194, 184, "2  Backend", "Validates source, user and payload. Calls the AI service.", C.ink);
  node(s, "extract", 529, 208, 194, 184, "3  AI service", "Collects available text, captions and page details for the AI.", C.amber);
  node(s, "llm1", 778, 208, 194, 184, "4  First LLM", "Creates summary, category, tags, topics and actions.", C.blue, C.light);
  node(s, "persist", 1027, 208, 181, 184, "5  Persist", "Stores structured memory plus a 768-value embedding.", C.green);
  box(s, "backend-duty", 44, 450, 1164, 122, C.panel, C.panel, 0);
  text(s, "backend-duty-head", "What is saved", 68, 466, 250, 30, 20, C.ink, true);
  text(s, "backend-duty-body", "PostgreSQL: URL, title, summary, category, tags, topics and actions   •   pgvector: 768 numbers representing semantic meaning", 68, 506, 1090, 44, 21, C.ink);
  notes(s, [
    "apps/extension/utils/extractors.js",
    "apps/backend/src/main/kotlin/com/impulse/backend/memory/MemoryService.kt",
    "apps/backend/src/main/kotlin/com/impulse/backend/memory/AiMemoryProcessor.kt",
    "apps/ai-service/memory_processor.py",
  ], "The client never talks directly to the database or Gemini. Spring Boot validates the request, calls the FastAPI AI service, validates the returned 768-value embedding, and saves the memory.");
}

// Detailed storage reference is incorporated into the end-to-end architecture.
if (false) {
  const s = p.slides.add();
  s.background.fill = C.paper;
  title(s, "PostgreSQL stores the facts; pgvector stores their meaning", 4);
  text(s, "storage-intro", "One memory record, two ways to retrieve it", 58, 100, 740, 32, 20, C.muted);
  node(s, "postgres", 44, 166, 548, 365, "PostgreSQL • structured post memory",
    "Identity\nmemory_id • user_id • source_url • platform\n\nSource text\ntitle • description • user_note • thumbnail_url\n\nAI text tags\nsummary • category • tags[] • topics[] • actions[]\n\nOperations\ncreated_at • collections • saved-plan references", C.ink, C.white);
  node(s, "pgvector", 688, 166, 520, 365, "pgvector • semantic index",
    "Embedding\n768 floating-point values per memory\n\nRepresents\nsemantic meaning of the compact structured memory\n\nUsed for\nquery-to-memory cosine similarity\n\nCombined with\nkeywords • topics • actions • recency • relevance gate", C.blue, C.light);
  arrow(s, "storage-join", 602, 316, 74, 42, C.blue);
  text(s, "storage-bottom", "The vector is not the post itself—it is a numeric map used to find related memories.", 44, 570, 1164, 42, 22, C.blue, true, "center");
  notes(s, [
    "apps/backend/src/main/kotlin/com/impulse/backend/memory/Memory.kt",
    "docs/03-memory-design.md",
    "docs/04-rag-design.md",
  ], "PostgreSQL keeps readable and auditable facts. Pgvector is an extension inside PostgreSQL that keeps the 768-value embedding used to compare meaning.");
}

// Slide 4 — complete LLM and RAG loop
{
  const s = p.slides.add();
  s.background.fill = C.paper;
  title(s, "LLM and RAG: understand once, find the right memory, answer", 4);
  text(s, "rag-ingest-label", "INGEST • happens once per new source", 44, 108, 440, 28, 17, C.blue, true);
  // top arrows first
  arrow(s, "r1", 245, 190, 50, 32);
  arrow(s, "r2", 500, 190, 50, 32);
  arrow(s, "r3", 755, 190, 50, 32);
  arrow(s, "r4", 1010, 190, 50, 32);
  node(s, "rag-source", 44, 150, 201, 112, "Source content", "Caption, metadata, visible text", C.ink);
  node(s, "rag-llm1", 295, 150, 205, 112, "First LLM", "Turns content into clean notes and tags", C.blue, C.light);
  node(s, "rag-embed1", 550, 150, 205, 112, "Embedding", "Meaning becomes 768 numbers", C.blue);
  node(s, "rag-store", 805, 150, 205, 112, "Memory store", "PostgreSQL + pgvector", C.green);
  node(s, "rag-ready", 1060, 150, 148, 112, "Reusable", "Analyze once", C.ink, C.lime);

  text(s, "rag-query-label", "ASK • happens for every question", 44, 314, 440, 28, 17, C.blue, true);
  arrow(s, "q1", 245, 400, 50, 32);
  arrow(s, "q2", 500, 400, 50, 32);
  arrow(s, "q3", 755, 400, 50, 32);
  arrow(s, "q4", 1010, 400, 50, 32);
  node(s, "rag-question", 44, 360, 201, 112, "User question", "Goal and constraints", C.ink);
  node(s, "rag-qembed", 295, 360, 205, 112, "Question meaning", "Question becomes 768 numbers", C.blue);
  node(s, "rag-retrieve", 550, 360, 205, 112, "RAG search", "Finds close meaning and matching words", C.amber, C.light);
  node(s, "rag-context", 805, 360, 205, 112, "Top memories", "At most 6; weak matches removed", C.green);
  node(s, "rag-llm2", 1060, 360, 148, 112, "Final LLM", "Grounded answer", C.ink, C.lime);

  box(s, "rag-formula", 44, 520, 1164, 112, C.panel, C.panel, 0);
  text(s, "rag-formula-head", "The backend is the safety boundary", 72, 532, 390, 28, 20, C.blue, true);
  text(s, "rag-formula-text", "Before saving: required fields + exactly 768 embedding values. Before answering: correct user only, weak matches removed, duplicates removed, maximum 6 memories.", 72, 565, 1108, 48, 19, C.ink);
  notes(s, [
    "docs/04-rag-design.md",
    "apps/backend/src/main/kotlin/com/impulse/backend/planning/MemoryRetrievalService.kt",
    "apps/backend/src/main/kotlin/com/impulse/backend/planning/PlanningService.kt",
    "apps/ai-service/memory_processor.py",
  ], "The first LLM converts source content into reusable memory. At query time the question is embedded, hybrid retrieval ranks the user's memories, and only the best six are sent to the final LLM for a grounded response.");
}

// Slide 5 — future scope and close
{
  const s = p.slides.add();
  s.background.fill = C.paper;
  title(s, "Future scope: richer inputs, faster retrieval, more control", 5);
  text(s, "future-intro", "Add capability in stages without changing the core architecture", 44, 102, 880, 30, 20, C.muted);
  node(s, "future-1", 44, 172, 270, 250, "1  Multimodal analysis",
    "Transcribe audio\nAnalyze selected video frames\nRead on-screen text\nCombine speech, image and caption", C.blue, C.light);
  node(s, "future-2", 338, 172, 270, 250, "2  More sources",
    "Approved social integrations\nDocuments and podcasts\nPrivate sources with consent\nDuplicate and source-quality checks", C.amber, C.white);
  node(s, "future-3", 632, 172, 270, 250, "3  Scale retrieval",
    "Async processing and retries\npgvector ANN search\nReranking and caching\nRe-embed when models improve", C.green, C.light);
  node(s, "future-4", 926, 172, 282, 250, "4  User control",
    "Edit AI-generated tags\nFeedback on recommendations\nExplain why each source was used\nPrivacy and retention controls", C.ink, C.white);
  box(s, "future-close", 44, 486, 1164, 112, C.panel, C.panel, 0);
  text(s, "future-close-head", "The architecture stays the same", 72, 500, 350, 32, 21, C.blue, true);
  text(s, "future-close-body", "Capture → understand → embed → store → retrieve → generate. Future features improve each stage without breaking the trusted backend boundary.", 72, 538, 1080, 46, 22, C.ink);
  notes(s, [
    "docs/11-creatorbrain-impulse-progress.md",
    "docs/08-link-ingestion-plan.md",
    "docs/04-rag-design.md",
  ], "Future work adds audio and frame analysis, more approved sources, asynchronous processing, faster vector search, reranking, and stronger user controls. The core flow remains stable: understand, store, retrieve, then generate.");
}

// Slide 6 — Q&A
{
  const s = p.slides.add();
  s.background.fill = C.paper;
  const imageBytes = await fs.readFile(HERO);
  s.images.add({
    blob: imageBytes, contentType: "image/png",
    alt: "Content flowing into AI knowledge for the closing question slide",
    fit: "cover", position: { left: 730, top: 0, width: 550, height: 720 },
  });
  text(s, "qa-kicker", "IMPULSE • CREATORBRAIN", 44, 52, 400, 30, 17, C.blue, true);
  text(s, "qa-title", "Questions?", 44, 214, 560, 92, 62, C.ink, false, "left", DISPLAY_FONT);
  text(s, "qa-subtitle", "Save once. Understand once.\nRetrieve whenever it matters.", 44, 328, 560, 96, 28, C.muted);
  box(s, "qa-prompt", 44, 512, 600, 92, C.panel, C.panel, 0);
  text(s, "qa-prompt-text", "Capture → Understand → Embed → Retrieve → Answer", 72, 532, 544, 48, 21, C.ink, true, "center");
  text(s, "qa-number", "06", 672, 660, 40, 22, 13, C.muted, false, "right");
  notes(s, ["Generated visual: OpenAI ImageGen; prompt recorded in task history."],
    "Impulse creates a reusable personal knowledge layer from content the user already values. Questions and feedback are welcome.");
}

await fs.mkdir(RENDER, { recursive: true });
for (const [i, slide] of p.slides.items.entries()) {
  const png = await p.export({ slide, format: "png", scale: 1 });
  await fs.writeFile(`${RENDER}/slide-${i + 1}.png`, new Uint8Array(await png.arrayBuffer()));
  const layout = await slide.export({ format: "layout" });
  await fs.writeFile(`${RENDER}/slide-${i + 1}.layout.json`, await layout.text());
}
const montage = await p.export({ format: "webp", montage: true, scale: 1 });
await fs.writeFile(`${RENDER}/montage.webp`, new Uint8Array(await montage.arrayBuffer()));
const pptx = await PresentationFile.exportPptx(p);
await pptx.save(OUT);
console.log(OUT);
