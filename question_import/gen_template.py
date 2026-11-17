import os
import openpyxl

OUT_DIR = r'e:\Desk\cet6_system\question_import'
os.makedirs(OUT_DIR, exist_ok=True)
wb = openpyxl.Workbook()
ws = wb.active
ws.title = 'questions'

# 表头（导入程序跳过第1行，从第2行开始读）
ws.append(['题型', '子题型', '难度', '题干', '选项', '标准答案', '解析'])

# 示例1：听力单选
ws.append([
    1, 101, 2,
    'What did the man do yesterday?',
    '["A. Went to the cinema.", "B. Stayed at home.", "C. Visited his friend.", "D. Played basketball."]',
    'A',
    '男士明确提到他昨天去看了电影。'
])

# 示例2：阅读仔细阅读（原文+问题放题干）
ws.append([
    2, 203, 2,
    '原文：The phenomenon of urbanization has reshaped modern society...\n\n问题：What does the author mainly discuss?',
    '["A. Urban population growth.", "B. Causes and effects of urbanization.", "C. Rural-urban migration policies.", "D. Housing prices in cities."]',
    'B',
    '文章首段点明主题，全文围绕城市化成因与影响展开。'
])

# 示例3：翻译（主观题，选项为空数组）
ws.append([
    3, 301, 2,
    '请将下面段落翻译成英语：\n中国结是一种传统的手工编织工艺品…',
    '[]',
    'Chinese knotting is a traditional hand-woven craft...',
    '本段考查被动语态与文化名词翻译。'
])

# 示例4：写作（主观题，选项为空数组）
ws.append([
    4, 401, 2,
    'Directions: For this part, you are allowed 30 minutes to write an essay on the importance of innovation. Write at least 150 words but no more than 200 words.',
    '[]',
    'Innovation has become a buzzword in recent years...',
    '立意+结构+语言三方面评分。'
])

out = os.path.join(OUT_DIR, '试题导入模板.xlsx')
wb.save(out)
print('saved:', out)
