import openpyxl, json, os
files=[r"e:\Desk\cet6_system\question_import\2026年6月第一套.xlsx",
       r"e:\Desk\cet6_system\question_import\2026年6月第二套.xlsx",
       r"e:\Desk\cet6_system\question_import\2026年6月第三套.xlsx"]
for f in files:
    wb=openpyxl.load_workbook(f); ws=wb.active
    print("=====",os.path.basename(f),"data rows:",ws.max_row-1,"=====")
    # row2 writing (题型4), find listening row, find translation row (题型3)
    for r in range(2,ws.max_row+1):
        t=ws.cell(r,1).value
        if t==4 or t==3:
            print(" 题型",t,"| 题干:",str(ws.cell(r,4).value)[:90],"| 答案:",str(ws.cell(r,6).value)[:90])
    # show one listening row with full opts
    for r in range(2,ws.max_row+1):
        if ws.cell(r,1).value==1:
            print(" 听力样例 row",r,"选项:",str(ws.cell(r,5).value)[:120],"答:",ws.cell(r,6).value)
            break
    # cloze row
    for r in range(2,ws.max_row+1):
        if ws.cell(r,2).value==201:
            print(" 选词样例 row",r,"答:",ws.cell(r,6).value,"| 选项:",str(ws.cell(r,5).value)[:80])
            break
print("OK")
