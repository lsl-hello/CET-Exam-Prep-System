import fitz
import os

os.makedirs(r"e:\Desk\cet6_system\question_import", exist_ok=True)

pdfs = {
    "exam": r"c:\Users\LSL\.trae-cn\attachments\6aab59f6fd8bda6fd582e25f\6d5fe792-4d7d-4f7d-9839-f953d93c5d89_947fefd9-411a-4f44-9e96-a57ebe20c156_2026年6月英语六级真题册（三套全）.pdf",
    "ans": r"c:\Users\LSL\.trae-cn\attachments\6aab59f6fd8bda6fd582e25f\cb75d1a0-ff60-4c36-b42c-d48eee13d70f_027008a1-d7d9-4440-91aa-1736325af080_2026年6月英语六级答案解析册（三套全）.pdf",
}

for tag, path in pdfs.items():
    doc = fitz.open(path)
    out = r"e:\Desk\cet6_system\question_import\%s.txt" % tag
    with open(out, "w", encoding="utf-8") as f:
        for i in range(len(doc)):
            text = doc[i].get_text()
            f.write("\n===== PAGE %d =====\n" % (i + 1))
            f.write(text)
    print(tag, "pages:", len(doc), "->", out)
    doc.close()
