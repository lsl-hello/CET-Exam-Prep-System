import sys
sys.path.insert(0, r"e:\Desk\cet6_system\question_import")
import importlib
import gen_xlsx as g
importlib.reload(g)
for sn in ["第1套","第2套","第3套"]:
    et=g.exam_sets[sn]
    listen=g.parse_listening(et)
    careful=g.parse_careful(et)
    match=g.parse_match(et)
    cloze=g.parse_cloze(et)
    print("===",sn,"===")
    print("listening qnums:", [n for n,_,_ in listen])
    print("careful qnums:", [n for n,_,_,_ in careful])
    print("match qnums:", [n for n,_ in match])
    print("cloze len:", len(cloze))
    wp=g.parse_writing(et)
    tr=g.parse_translation(et)
    print("writing len:", len(wp), "| transl len:", len(tr))
