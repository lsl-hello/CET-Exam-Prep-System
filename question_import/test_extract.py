import fitz
import pdfplumber

path = r"c:\Users\LSL\.trae-cn\attachments\6aab59f6fd8bda6fd582e25f\6d5fe792-4d7d-4f7d-9839-f953d93c5d89_947fefd9-411a-4f44-9e96-a57ebe20c156_2026年6月英语六级真题册（三套全）.pdf"

# Try pymupdf various modes
doc = fitz.open(path)
print("=== PyMuPDF page 1 various modes ===")
page = doc[0]
for mode in ["text", "blocks", "words", "html", "xml", "dict", "rawdict"]:
    t = page.get_text(mode)
    if isinstance(t, (list, dict)):
        s = str(t)
    else:
        s = t
    print("--- mode:", mode, "len:", len(s), "---")
    print(s[:500])
doc.close()

print("\n=== pdfplumber page 1 ===")
with pdfplumber.open(path) as pdf:
    p = pdf.pages[0]
    txt = p.extract_text() or ""
    print("text len:", len(txt))
    print(txt[:500])
    # try chars
    chars = p.chars
    print("num chars:", len(chars))
    if chars:
        print("first char:", chars[0])
