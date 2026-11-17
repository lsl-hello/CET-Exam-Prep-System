import fitz
import easyocr
import os
import time
import sys

# Re-render all pages at 3x zoom into img3
pdfs = {
    "exam": (r"c:\Users\LSL\.trae-cn\attachments\6aab59f6fd8bda6fd582e25f\6d5fe792-4d7d-4f7d-9839-f953d93c5d89_947fefd9-411a-4f44-9e96-a57ebe20c156_2026年6月英语六级真题册（三套全）.pdf",
             r"e:\Desk\cet6_system\question_import\img3_exam"),
    "ans": (r"c:\Users\LSL\.trae-cn\attachments\6aab59f6fd8bda6fd582e25f\cb75d1a0-ff60-4c36-b42c-d48eee13d70f_027008a1-d7d9-4440-91aa-1736325af080_2026年6月英语六级答案解析册（三套全）.pdf",
            r"e:\Desk\cet6_system\question_import\img3_ans"),
}

zoom = 3.0
mat = fitz.Matrix(zoom, zoom)
for tag, (path, outdir) in pdfs.items():
    os.makedirs(outdir, exist_ok=True)
    doc = fitz.open(path)
    for i in range(len(doc)):
        out = os.path.join(outdir, "p%03d.png" % (i + 1))
        if not os.path.exists(out):
            doc[i].get_pixmap(matrix=mat).save(out)
    print(tag, "rendered", len(doc), "pages")
    doc.close()

# OCR with paragraph mode
reader = easyocr.Reader(['en', 'ch_sim'], gpu=False, verbose=False)

for tag, outdir in [("exam", pdfs["exam"][1]), ("ans", pdfs["ans"][1])]:
    files = sorted(f for f in os.listdir(outdir) if f.endswith('.png'))
    outfile = r"e:\Desk\cet6_system\question_import\ocr3_%s.txt" % tag
    with open(outfile, 'w', encoding='utf-8') as out:
        for fn in files:
            t0 = time.time()
            result = reader.readtext(os.path.join(outdir, fn), detail=1, paragraph=True)
            result.sort(key=lambda x: (round(x[0][0][1] / 15), x[0][0][0]))
            out.write("\n===== %s =====\n" % fn)
            for item in result:
                box, text = item
                out.write(">> %s\n" % text)
            out.flush()
            print("%s %s: %d paras, %.1fs" % (tag, fn, len(result), time.time() - t0))
print("=== ALL DONE ===")
