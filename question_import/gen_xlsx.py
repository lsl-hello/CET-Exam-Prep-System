# -*- coding: utf-8 -*-
"""Generate 3 xlsx for CET-6 2026年6月 from OCR text."""
import os, re, json, openpyxl

BASE = r"e:\Desk\cet6_system\question_import"
EXAM_OCR = os.path.join(BASE, "ocr3_exam.txt")
ANS_OCR = os.path.join(BASE, "ocr3_ans.txt")
OUTDIR = BASE

def clean(s):
    if not s: return s
    rep = [
        ("JOU ","you "),("JOU'","you'"),("JOU","you"),("Jolr","your"),("JouR","your"),
        ("Jour","your"),("JoUr","your"),("Joul","your"),("JOUT","your"),
        ("yOUI","your"),
        ("WOIk","work"),("WoIk","work"),("WOrk","work"),("WOIK","work"),("woIk","work"),("woik","work"),
        ("Iun ","run "),("CareCr","career"),("CaCer","career"),
        ("tat ","that "),("tat.","that."),
        (" 讧 "," in "),("讧 ","in "),("讧","in"),(" 边 "," in "),("边 ","in "),
        (" 让 "," it "),("让's","it's"),("让$","it's"),("让 ","it "),
        (" 叩 "," up "),("叩 ","up "),
        ("Iost","most"),("IOre","more"),("I0re","more"),
        ("OICe","once"),("OIC ","once "),
        ("SeC ","see "),("seC ","see "),
        ("IeI ","let "),
        ("WOII","will"),("WOrld","world"),
        ("CauSe","cause"),
        ("undelstand","understand"),("gderstand","understand"),
        ("YOUI","your"),("QIe ","are "),("Qll ","all "),
        ("llUst","must"),("IIUst","must"),
        ("wlth","with"),("Wllh","with"),
        ("al1","all"),("anD","and"),
        ("巳)","E)"),("卫)","E)"),("珏)","H)"),("玏)","H)"),("工)","I)"),("[)","I)"),
        ("0)","O)"),("召)","B)"),("下)","F)"),("玎)","K)"),
    ]
    for a,b in rep: s=s.replace(a,b)
    s=re.sub(r"  +"," ",s)
    s=s.replace(" .",".").replace(" ,",",")
    return s.strip()

def read_paras(path):
    pages=[]; cur=None; paras=[]
    for line in open(path,"r",encoding="utf-8"):
        line=line.rstrip("\n")
        m=re.match(r"===== (p\d+\.png) =====",line)
        if m:
            if cur is not None: pages.append((cur,paras))
            cur=m.group(1); paras=[]
        elif line.startswith(">> "): paras.append(line[3:])
        elif line.strip()=="": continue
        else: paras.append(line)
    if cur is not None: pages.append((cur,paras))
    return pages

exam_pages=read_paras(EXAM_OCR)
ans_pages=read_paras(ANS_OCR)

def detect_set_pages(pages, skip_last=False):
    sets={"第1套":[],"第2套":[],"第3套":[]}
    cur="第1套"
    for p,paras in pages:
        if p=="p001.png": continue
        joined="\n".join(paras)
        if "笫1套" in joined or ("第1套" in joined and "答案速查" not in joined): cur="第1套"
        elif "笫2套" in joined or ("第2套" in joined and "答案速查" not in joined): cur="第2套"
        elif "笫3套" in joined or ("第3套" in joined and "答案速查" not in joined): cur="第3套"
        if skip_last and "答案速查" in joined: continue
        if p=="p024.png": continue
        sets[cur].append((p,joined))
    return {k:"\n".join(t for _,t in v) for k,v in sets.items()}

exam_sets=detect_set_pages(exam_pages)
ans_sets=detect_set_pages(ans_pages, skip_last=True)

# ---------------- Authoritative ANSWER tables (from speed-check + detailed) ----------------
ANSWERS={
 "第1套":{
   "listen":["B","C","A","D","A","C","B","A","A","D","C","D","B","D","A","A","C","B","B","C","D","D","B","A","C"],
   "cloze":["J","I","D","C","F","K","B","M","A","E"],          # 26-35
   "match":["D","E","M","F","B","K","G","C","M","J"],          # 36-45 (some inferred)
   "careful":["A","B","C","D","A","C","B","A","D","D"],        # 46-55
 },
 "第2套":{
   "listen":["A","A","B","D","D","C","B","C","A","C","D","D","A","C","A","B","A","C","B","B","D","C","B","D","C"],
   "cloze":["D","O","A","N","E","M","J","I","B","H"],          # 26-35
   "match":["E","J","G","M","D","B","C","F","K","B"],          # 36-45
   "careful":["C","A","B","C","D","C","B","D","B","A"],        # 46-55
 },
 "第3套":{
   "cloze":["B","A","N","D","E","M","O","I","C","J"],          # 26-35
   "match":["E","A","G","C","P","I","B","N","D","O"],          # 36-45
   "careful":["B","C","D","C","B","A","A","D","D","A"],        # 46-55
 },
}

# Word banks (15 words A-O) per set
WORDBANK={
 "第1套":["anywhere","asserts","constituent","correlated","determining","devastating","enacted","entails","exemption","impose","notified","notwithstanding","presume","rehabilitation","reproaching"],
 "第2套":["behavioural","eliminate","fragile","global","implications","skeptically","overlapping","maximise","traits","prone","underlying","personally","presume","prototypes","reassure"],
 "第3套":["ascertain","potential","buffer","correlated","rigid","proximity","crave","statistics","demographics","substantially","legitimately","underlie","mandated","rigid","proximity"],
}
# Hardcoded writing prompts (from OCR, used as fallback) and translation source (fallback)
WRITING_PROMPT={
 "第1套":"Directions: For this part, you are allowed 30 minutes to write an essay that begins with the sentence \"Faced with growing globalization, students realize that deep understanding of traditional Chinese culture helps cultivate cultural confidence.\" You can make comments, cite examples or use your personal experiences to develop your essay. You should write at least 150 words but no more than 200 words. You should copy the sentence given in quotes at the beginning of your essay.",
 "第2套":"Directions: For this part, you are allowed 30 minutes to write an essay that begins with the sentence \"With the trend of urbanization, it is vitally important for city residents to develop a stronger love for nature.\" You can make comments, cite examples or use your personal experiences to develop your essay. You should write at least 150 words but no more than 200 words. You should copy the sentence given in quotes at the beginning of your essay.",
 "第3套":"Directions: For this part, you are allowed 30 minutes to write an essay that begins with the sentence \"In an era of rapid social development, it is vitally important to enhance young people's sense of responsibility.\" You can make comments, cite examples or use your personal experiences to develop your essay. You should write at least 150 words but no more than 200 words. You should copy the sentence given in quotes at the beginning of your essay.",
}
TRANSLATION_SRC={
 "第1套":"科技进步使中国的医疗事业得到快速发展。近年来，远程医疗使优质医疗资源惠及更多患者。医学专家可以通过视频为偏远地区患者提供咨询，指导当地医生制定并实施治疗方案。人工智能辅助诊断系统帮助医生识别患者的早期症状，提高诊断准确率。电子健康档案的普及让医生能够快速了解患者病史，避免重复检查。线上医疗咨询平台极大地方便了患者就诊。科技与医疗的深度融合更好地满足了人民的健康需求。",
 "第2套":"电子商务的蓬勃发展正在深刻改变中国农村的面貌。随着互联网和物流网络快速向农村延伸，越来越多的农村居民通过电子商务平台将农产品直接销售给城市消费者。近年来，直播带货（live-streaming commerce）使城市消费者能够与卖家实时互动，已成为促进农村发展的一种新方式。电子商务打破了城乡之间的信息壁垒，拓宽了农产品销售渠道，帮助农村居民增收致富。数字经济与乡村振兴的深度融合，正在为中国农村发展注入前所未有的活力。",
 "第3套":"近年来，中国无人机（drone）产业蓬勃发展，出口量跃升为全球第一。中国的无人机以出色的稳定性、高清（high-definition）拍摄能力和便捷的操作，受到全球消费者的喜爱，在民用领域得到广泛应用。无人机已被国家纳入战略性新兴产业，逐步成为能满足多种场景需求的智能助手。在农村，无人机可精准识别农作物病虫害，高效喷洒农药。在城市，无人机可快速运送生鲜食品。在不久的将来，无人机能够在更多领域实现其独特价值，为经济社会发展注入新动能。",
}

print("loaded sets:", {k:len(v) for k,v in exam_sets.items()})

# ---------------- Parsers ----------------
OPT_SPLIT = re.compile(r'\s*([A-D])\s*\)\s*')

def parse_listening(text):
    """Return list of (qnum, stem, [A,B,C,D]). stem usually empty for listening."""
    out=[]
    # question start: number (1-25) optionally followed by . or 。 then A ) or A (with options)
    starts=list(re.finditer(r'(?<!\d)(\d{1,2})\.?[。\.\s]+\s*A\s*\)', text))
    for i,m in enumerate(starts):
        n=int(m.group(1))
        if n<1 or n>25: continue
        start=m.start()
        end=starts[i+1].start() if i+1<len(starts) else len(text)
        seg=text[start:end]
        # capture 4 options by letter A) ... B) ... C) ... D) ...  (')' optional)
        mm=re.search(r'A\s*\)?\s*(.*?)\s+B\s*\)?\s*(.*?)\s+C\s*\)?\s*(.*?)\s+D\s*\)?\s*(.*?)(?:Questions|\d{1,2}\.|\Z)', seg, re.S)
        opts=[]
        if mm:
            opts=[clean(mm.group(k)) for k in (1,2,3,4)]
        else:
            parts=re.split(r'\s*([A-D])\s*\)\s*', seg)
            for j in range(2,len(parts),2):
                if len(opts)<4: opts.append(clean(parts[j]))
        opts=[re.sub(r'(Questions|Section|Part\s|Directions).*','',o, flags=re.S).strip() for o in opts][:4]
        out.append((n, "", opts))
    seen={}
    for n,s,o in out:
        if n not in seen and len(o)>=3: seen[n]=(s,o)
    return [(n,seen[n][0],seen[n][1]) for n in sorted(seen)]

def parse_careful(text):
    """Section C: 2 passages x5 Q (46-55). Return list of (qnum, passage, stem, opts)."""
    out=[]
    chunks=re.split(r'Passage (One|Two)', text)
    passages={}
    for i in range(1,len(chunks),2):
        label=chunks[i]; body=chunks[i+1] if i+1<len(chunks) else ""
        passages[label]=body
    qmap={"One":list(range(46,51)),"Two":list(range(51,56))}
    for label,body in passages.items():
        first_q=qmap[label][0]
        mq=re.search(r'\b%d[\.。]'%first_q, body)
        ptext=clean(body[:mq.start()]) if mq else clean(body)
        for n in qmap[label]:
            # N. <stem> A )... B )... C )... D )...  (option letters may lack ) )
            pat=re.compile(r'\b%d[\.。]\s*(.*?)\s+A\s*\)?\s*(.*?)\s+B\s*\)?\s*(.*?)\s+C\s*\)?\s*(.*?)\s+D\s*\)?\s*(.*?)(?=\b\d{1,2}[\.。]|\Z)'%n, re.S)
            mm=pat.search(body)
            if mm:
                stem=clean(mm.group(1))
                opts=[clean(mm.group(k)) for k in (2,3,4,5)]
                if len(opts)>=4:
                    out.append((n, ptext, stem, opts))
    out.sort()
    return out

def parse_match(text):
    """Section B: 10 statements 36-45. Return list (qnum, statement)."""
    out=[]
    for n in range(36,46):
        # statement starts after "N." and ends before "N+1." 
        pat=re.compile(r'\b%d\.\s*(.*?)(?=\b%d\.|\Z)'%(n,n+1), re.S)
        mm=pat.search(text)
        if mm:
            st=clean(mm.group(1))
            st=re.sub(r'\s+',' ',st)
            out.append((n, st))
    return out

def parse_cloze(text):
    """Section A: return passage text (with blanks)."""
    ma=re.search(r'Section\s*A', text)
    mb=re.search(r'Section\s*B', text)
    seg=text[ma.start():mb.start()] if (ma and mb) else text
    # find end of directions (through the centre, flexible)
    m=re.search(r'through the centre[.。_\s]*', seg, re.S|re.I)
    if not m:
        m=re.search(r'more than once[.。_\s]*', seg, re.S|re.I)
    start=m.end() if m else 0
    body=seg[start:]
    # find word bank start: a run like "A <word> I) <word> B) <word>"
    wb=re.search(r'\bA\s*\w+\s*[A-O]\s*\)\s*\w+', body)
    if wb:
        body=body[:wb.start()]
    return clean(body)

def parse_writing(text):
    # isolate Writing section (between 'Writ' and 'Listening Comprehension')
    mw=re.search(r'Writ\w*', text)
    ml=re.search(r'Listening Comprehension', text)
    if mw and ml and mw.start()<ml.start():
        sect=text[mw.start():ml.start()]
    else:
        sect=text
    # match Directions ... up to "You should write" (writing prompt always has this)
    m=re.search(r'(Directions[:：]?.*?You should write[^。]*words[。.]?)', sect, re.S|re.I)
    if m and re.search(r'essay|write', m.group(1), re.I):
        p=clean(m.group(1))
        m2=re.search(r'(You should copy.*?essay)', sect, re.S|re.I)
        if m2: p=p+" "+clean(m2.group(1))
        return p
    # fallback: Directions block containing 'essay' up to first 。 period
    m=re.search(r'(Directions[:：]?[^。]{20,600})', sect, re.S)
    if m and re.search(r'essay|write', m.group(1), re.I):
        return clean(m.group(1))
    return ""

def parse_translation(text):
    # Chinese passage after "Part ... Translation ... Directions ... Answer Sheet 2"
    m=re.search(r'Translation[^\n]*\n?\s*Directions[:：]?[^\n]*Answer Sheet 2?\.?\s*(.*?)(?:2026年6月|H5c|讧书号)', text, re.S)
    if m:
        return clean(m.group(1))
    return ""

# ---------------- Answer/analysis extraction from answer OCR ----------------
def parse_analyses(set_name, ans_text):
    """Return dict qnum(int)->(answer_letter, analysis_text)."""
    out={}
    # Pattern: "N.答案为 X)" or "答案为 X)" with question number nearby.
    # The detailed answer paragraphs often start with "N." then have "答案为 X)"
    # We look for each question 1..55: find "N." then nearest "答案为"
    for n in range(1,56):
        # find occurrence of "N." (with chinese period 。 or .)
        pat=re.compile(r'\b%d[.。][^A-Z]{0,40}?答案为\s*([A-O])[)）]?'%n)
        mm=pat.search(ans_text)
        if not mm:
            # try "答案为 X)" followed by, and N. somewhere before
            pat2=re.compile(r'\b%d[.。]'%n)
            mn=pat2.search(ans_text)
            if mn:
                tail=ans_text[mn.start():mn.start()+1500]
                ma=re.search(r'答案为\s*([A-O])[)）]?', tail)
                if ma:
                    mm=ma; offset=mn.start()+ma.start()
                else:
                    continue
            else:
                continue
        letter=mm.group(1) if mm.group(1) else ""
        # analysis: text from answer marker to next question or 干扰 marker
        end_pos=mm.end()
        # find next "N." question or next 答案为
        nxt=re.search(r'\b\d{1,2}[.。]\s*(?:答案为|What|Why|How|According|What does)', ans_text[end_pos:end_pos+1500])
        seg=ans_text[end_pos:end_pos+(nxt.start() if nxt else 600)]
        # keep up to 干扰项排除 marker, take rationale
        rationale=re.search(r'(.*?)(?:干扰项排除|干扰项)', seg, re.S)
        ana=rationale.group(1) if rationale else seg
        ana=re.sub(r'\s+',' ',ana).strip()
        ana=ana[:300]
        out[n]=(letter, ana)
    return out

# ---------------- Build rows ----------------
def english_only(s):
    """From bilingual interleaved text, keep English-dominant sentences."""
    s=re.sub(r'\s+',' ',s)
    # split into chunks by Chinese-period/space; keep chunks that are mostly ASCII
    out=[]
    for chunk in re.split(r'(?<=[。.!?])\s+', s):
        chunk=chunk.strip()
        if not chunk: continue
        ascii_ratio=sum(1 for c in chunk if ord(c)<128)/max(len(chunk),1)
        # exclude pure-Chinese chunks
        if ascii_ratio>0.7 and re.search(r'[A-Za-z]', chunk):
            out.append(chunk)
    return " ".join(out)


def opt_json(opts):
    arr=[]
    for i,o in enumerate(opts):
        L=chr(ord('A')+i)
        arr.append("%s. %s"%(L,o))
    return json.dumps(arr, ensure_ascii=False)

def opt_letters(n=10):
    arr=[chr(ord('A')+i) for i in range(n)]
    return json.dumps(arr, ensure_ascii=False)

def wordbank_json(words):
    arr=["%s. %s"%(chr(ord('A')+i), w) for i,w in enumerate(words)]
    return json.dumps(arr, ensure_ascii=False)

def build_rows(set_name):
    etext=exam_sets[set_name]
    atext=ans_sets.get(set_name,"")
    an=parse_analyses(set_name, atext)
    rows=[]
    A=ANSWERS[set_name]
    PH="(OCR未能提取该题文本，请在后台补填题干/选项)"

    # ---- Writing ----
    wprompt=parse_writing(etext)
    if not wprompt or len(wprompt)<60:
        wprompt=WRITING_PROMPT[set_name]
    essay_ref=""
    occs=list(re.finditer(r'参考范文', atext))
    if occs:
        seg=atext[occs[0].end():occs[0].end()+2500]
        seg=re.split(r'话题词汇|写作模板|参考译文\s|Part\s', seg)[0]
        essay_ref=clean(english_only(seg))[:1500]
    rows.append([4,401,2,wprompt,"[]",essay_ref or "(见答案解析册参考范文)",""])

    # ---- Listening ----
    if "listen" in A:
        listen={n:(s,o) for n,s,o in parse_listening(etext)}
        for n in range(1,26):
            sub=101 if n<=8 else (102 if n<=15 else 103)
            ans=A["listen"][n-1]
            _,ana=an.get(n,("", ""))
            if n in listen and len(listen[n][1])>=3:
                stem,opts=listen[n]
                rows.append([1,sub,2,stem, opt_json(opts), ans, ana])
            else:
                rows.append([1,sub,2,"听力第%d题（%s）：%s"%(n,"Section A" if sub==101 else "Section B" if sub==102 else "Section C", PH),"[\"A. \",\"B. \",\"C. \",\"D. \"]", ans, ana])

    # ---- Reading Section A 选词填空 ----
    cloze_passage=parse_cloze(etext)
    if len(cloze_passage)<200 or len(cloze_passage)>6000:
        cloze_passage="(选词填空原文未能完整提取，请在后台补填；下方为本题15词备选)"
    wb=WORDBANK[set_name]
    if len(wb)<15:
        wb=wb+["?"]*(15-len(wb))
    for idx,n in enumerate(range(26,36)):
        ans=A["cloze"][idx]
        _,ana=an.get(n,("", ""))
        rows.append([2,201,2,"选词填空（题%d，原文含编号空）：%s"%(n, cloze_passage), wordbank_json(wb[:15]), ans, ana])

    # ---- Reading Section B 长篇阅读 ----
    match={n:st for n,st in parse_match(etext)}
    for n in range(36,46):
        idx=n-36
        ans=A["match"][idx]
        _,ana=an.get(n,("", ""))
        stmt=match.get(n) or ("长篇阅读第%d题陈述句：%s"%(n,PH))
        rows.append([2,202,2, stmt, opt_letters(13), ans, ana])

    # ---- Reading Section C 仔细阅读 ----
    careful={n:(p,s,o) for n,p,s,o in parse_careful(etext)}
    for n in range(46,56):
        idx=n-46
        ans=A["careful"][idx]
        _,ana=an.get(n,("", ""))
        if n in careful:
            p,s,o=careful[n]
            full="原文："+p+"\n\n问题："+s
            rows.append([2,203,2, full, opt_json(o), ans, ana])
        else:
            rows.append([2,203,2,"仔细阅读第%d题：%s"%(n,PH),"[\"A. \",\"B. \",\"C. \",\"D. \"]", ans, ana])

    # ---- Translation ----
    src=parse_translation(etext)
    if not src or len(src)<60:
        src=TRANSLATION_SRC[set_name]
    trans_ref=""
    occs=list(re.finditer(r'参考译文', atext))
    if occs:
        last=occs[-1]
        seg=atext[last.end():last.end()+1500]
        seg=re.split(r'话题词汇|写作模板|\d{4}年\d|答案详解', seg)[0]
        trans_ref=clean(english_only(seg))[:1200]
    rows.append([3,301,2, src, "[]", trans_ref or "(见答案解析册参考译文)",""])

    return rows

# ---------------- Write xlsx ----------------
def write_xlsx(path, rows):
    wb=openpyxl.Workbook()
    ws=wb.active
    ws.title="Sheet1"
    header=["题型","子题型","难度","题干","选项","标准答案","解析"]
    ws.append(header)
    for r in rows:
        ws.append(r)
    wb.save(path)

sets_order=["第1套","第2套","第3套"]
fnames={"第1套":"2026年6月第一套.xlsx","第2套":"2026年6月第二套.xlsx","第3套":"2026年6月第三套.xlsx"}
counts={}
for sn in sets_order:
    rows=build_rows(sn)
    p=os.path.join(OUTDIR, fnames[sn])
    write_xlsx(p, rows)
    counts[sn]=rows
    print("%s -> %d rows, %s"%(sn, len(rows), p))
print("DONE")

