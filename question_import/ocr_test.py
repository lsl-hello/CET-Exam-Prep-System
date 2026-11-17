import easyocr
import os
import time

# Initialize reader for English + Chinese, GPU disabled (CPU only)
print("Initializing EasyOCR reader (en+zh, CPU)...")
t0 = time.time()
reader = easyocr.Reader(['en', 'ch_sim'], gpu=False, verbose=False)
print("Init time: %.1fs" % (time.time() - t0))

# Test on first exam page
img = r"e:\Desk\cet6_system\question_import\img_exam\p001.png"
t0 = time.time()
result = reader.readtext(img, detail=1, paragraph=False)
print("OCR time page1: %.1fs, items: %d" % (time.time() - t0, len(result)))
print("--- TEXT (sorted by position) ---")
# Sort by y then x
result.sort(key=lambda x: (x[0][0][1], x[0][0][0]))
for box, text, conf in result:
    print("[%.2f] %s" % (conf, text))
