import fitz
import os

os.makedirs(r"e:\Desk\cet6_system\question_import\img_exam", exist_ok=True)
os.makedirs(r"e:\Desk\cet6_system\question_import\img_ans", exist_ok=True)

pdfs = {
    "exam": (r"c:\Users\LSL\.trae-cn\attachments\6aab59f6fd8bda6fd582e25f\6d5fe792-4d7d-4f7d-9839-f953d93c5d89_947fefd9-411a-4f44-9e96-a57ebe20c156_2026年6月英语六级真题册（三套全）.pdf",
             r"e:\Desk\cet6_system\question_import\img_exam"),
    "ans": (r"c:\Users\LSL\.trae-cn\attachments\6aab59f6fd8bda6fd582e25f\cb75d1a0-ff60-4c36-b42c-d48eee13d70f_027008a1-d7d9-4440-91aa-1736325af080_2026年6月英语六级答案解析册（三套全）.pdf",
            r"e:\Desk\cet6_system\question_import\img_ans"),
}

# Render at 200 DPI for good OCR/visual quality but manageable size
for tag, (path, outdir) in pdfs.items():
    doc = fitz.open(path)
    zoom = 2.0  # ~192 dpi
    mat = fitz.Matrix(zoom, zoom)
    for i in range(len(doc)):
        page = doc[i]
        pix = page.get_pixmap(matrix=mat)
        out = os.path.join(outdir, "p%03d.png" % (i + 1))
        pix.save(out)
    print(tag, "pages rendered:", len(doc))
    doc.close()
