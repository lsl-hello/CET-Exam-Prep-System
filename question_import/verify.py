import openpyxl, json, os
files=[r"e:\Desk\cet6_system\question_import\2026年6月第一套.xlsx",
       r"e:\Desk\cet6_system\question_import\2026年6月第二套.xlsx",
       r"e:\Desk\cet6_system\question_import\2026年6月第三套.xlsx"]
for f in files:
    wb=openpyxl.load_workbook(f)
    ws=wb.active
    print("=====",os.path.basename(f),"rows(excl header):",ws.max_row-1,"=====")
    # count by 题型
    cnt={}
    for r in range(2,ws.max_row+1):
        t=ws.cell(r,1).value
        cnt[t]=cnt.get(t,0)+1
    print("题型计数:",cnt)
    # validate row 2 and 3
    for r in (2,3):
        vals=[ws.cell(r,c).value for c in range(1,8)]
        print("row",r,"| A型:",vals[0],"| B子型:",vals[1],"| C难:",vals[2],"| D题干:",(str(vals[3])[:60]+"..."),"| E选项:",str(vals[4])[:60],"| F答:",vals[5],"| G解:",str(vals[6])[:30])
        # validate 选项 is JSON array
        try:
            json.loads(vals[4]); ok=True
        except Exception as e:
            ok=False
        assert isinstance(vals[0],int), "题型不是数字"
        assert isinstance(vals[1],int), "子题型不是数字"
        assert isinstance(vals[2],int), "难度不是数字"
        assert ok, "选项列不是合法JSON"
    print()
print("VALIDATION PASSED")
