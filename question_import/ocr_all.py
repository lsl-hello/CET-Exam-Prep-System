import easyocr
import os
import time
import sys

reader = easyocr.Reader(['en', 'ch_sim'], gpu=False, verbose=False)

def ocr_dir(indir, outfile):
    files = sorted(f for f in os.listdir(indir) if f.endswith('.png'))
    with open(outfile, 'w', encoding='utf-8') as out:
        for fn in files:
            t0 = time.time()
            result = reader.readtext(os.path.join(indir, fn), detail=1, paragraph=False)
            # sort by y then x
            result.sort(key=lambda x: (round(x[0][0][1] / 20), x[0][0][0]))
            out.write("\n===== %s =====\n" % fn)
            for box, text, conf in result:
                out.write("[%.2f] %s\n" % (conf, text))
            out.flush()
            print("%s done: %d items, %.1fs" % (fn, len(result), time.time() - t0))

ocr_dir(r"e:\Desk\cet6_system\question_import\img_exam",
        r"e:\Desk\cet6_system\question_import\ocr_exam.txt")
print("=== EXAM DONE ===")
ocr_dir(r"e:\Desk\cet6_system\question_import\img_ans",
        r"e:\Desk\cet6_system\question_import\ocr_ans.txt")
print("=== ANS DONE ===")
