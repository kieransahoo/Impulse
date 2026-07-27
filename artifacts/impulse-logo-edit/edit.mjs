import fs from "node:fs/promises";
import { FileBlob, PresentationFile } from "@oai/artifact-tool";

const workspace = "/Users/kieransahoo/Ipsator/Impulse/artifacts/impulse-logo-edit";
const source = `${workspace}/template-starter.pptx`;
const output = "/Users/kieransahoo/Ipsator/Impulse/Impulse_AI_PPT_With_Logo.pptx";
const iconPath = `${workspace}/impulse-icon.svg`;
const renderDir = `${workspace}/final-render`;

async function writeBlob(path, blob) {
  await fs.writeFile(path, new Uint8Array(await blob.arrayBuffer()));
}

const presentation = await PresentationFile.importPptx(await FileBlob.load(source));
const slide = presentation.slides.getItem(0);
const brandLine = slide.shapes.items.find(
  (shape) => shape.name === "Google Shape;17;p3",
);
if (!brandLine) throw new Error("Could not find the slide 1 brand line.");

brandLine.position = { left: 88, top: 48, width: 456, height: 34 };

const iconBytes = await fs.readFile(iconPath);
slide.images.add({
  blob: iconBytes,
  contentType: "image/svg+xml",
  name: "Impulse official app icon",
  alt: "Impulse app icon: white italic I on a near-black rounded square",
  fit: "contain",
  position: { left: 44, top: 46, width: 36, height: 36 },
});

const notes = slide.speakerNotes;
notes.setText(`${notes.text.trim()}

[Brand Asset]
- Official Impulse app icon from apps/frontend/app/src/main/res/drawable/ic_launcher_background.xml and ic_launcher_foreground.xml.`);

await fs.mkdir(renderDir, { recursive: true });
for (const [index, currentSlide] of presentation.slides.items.entries()) {
  await writeBlob(
    `${renderDir}/slide-${index + 1}.png`,
    await presentation.export({ slide: currentSlide, format: "png", scale: 1 }),
  );
  const layout = await currentSlide.export({ format: "layout" });
  await fs.writeFile(`${renderDir}/slide-${index + 1}.layout.json`, await layout.text());
}
await writeBlob(
  `${renderDir}/montage.webp`,
  await presentation.export({ format: "webp", montage: true, scale: 1 }),
);

const pptx = await PresentationFile.exportPptx(presentation);
await pptx.save(output);
console.log(output);
