import fitz
import easyocr
import os
import time

# Re-render page 2 at higher DPI
path = r"c:\Users\LSL\.trae-cn\attachments\6aab59f6fd8bda6fd582e25f\6d5fe792-4d7d-4f7d-9839-f953d93c5d89_947fefd9-411a-4f44-9e96-a57ebe20c156_2026年6月英语六级真题册（三套全）.pdf"
doc = fitz.open(path)
outdir = r"e:\Desk\cet6_system\question_import\img_hires"
os.makedirs(outdir, exist_ok=True)
for zoom in [3.0, 4.0]:
    mat = fitz.Matrix(zoom, zoom)
    pix = doc[1].get_pixmap(matrix=mat)  # page 2
    out = os.path.join(outdir, "p002_z%.1f.png" % zoom)
    pix.save(out)
    print("saved", out, os.path.getsize(out))
doc.close()

reader = easyocr.Reader(['en', 'ch_sim'], gpu=False, verbose=False)

for fn in ["p002_z3.0.png", "p002_z4.0.png"]:
    print("\n#####", fn, "paragraph=True #####")
    t0 = time.time()
    result = reader.readtext(os.path.join(outdir, fn), detail=1, paragraph=True)
    result.sort(key=lambda x: (round(x[0][0][1] / 15), x[0][0][0]))
    print("time %.1fs items %d" % (time.time() - t0, len(result)))
    for box, text, conf in result:
        print("[%.2f] %s" % (conf, text))
