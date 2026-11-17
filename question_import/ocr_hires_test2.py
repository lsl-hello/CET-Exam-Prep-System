import easyocr
import os
import time

reader = easyocr.Reader(['en', 'ch_sim'], gpu=False, verbose=False)
outdir = r"e:\Desk\cet6_system\question_import\img_hires"

for fn in ["p002_z3.0.png", "p002_z4.0.png"]:
    print("\n#####", fn, "paragraph=True #####")
    t0 = time.time()
    result = reader.readtext(os.path.join(outdir, fn), detail=1, paragraph=True)
    result.sort(key=lambda x: (round(x[0][0][1] / 15), x[0][0][0]))
    print("time %.1fs items %d" % (time.time() - t0, len(result)))
    for item in result:
        box, text = item
        print(">> %s" % text)
