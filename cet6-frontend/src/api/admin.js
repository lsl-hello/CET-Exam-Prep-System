import request from '@/utils/request'

export const getUserList = params => request.get('/admin/user/list', { params })
export const saveUser = data => request.post('/admin/user', data)
export const deleteUser = id => request.delete(`/admin/user/${id}`)

export const getNoticeList = params => request.get('/admin/notice/list', { params })
export const saveNotice = data => request.post('/admin/notice', data)
export const deleteNotice = id => request.delete(`/admin/notice/${id}`)

export const getWordBookList = params => request.get('/admin/wordbook/list', { params })
export const saveWordBook = data => request.post('/admin/wordbook', data)
export const deleteWordBook = id => request.delete(`/admin/wordbook/${id}`)

export const getWordList = params => request.get('/admin/word/list', { params })
export const saveWord = data => request.post('/admin/word', data)
export const deleteWord = id => request.delete(`/admin/word/${id}`)
export const importWords = (file, bookId) => {
  const form = new FormData()
  form.append('file', file)
  form.append('bookId', bookId)
  return request.post('/admin/word/import', form)
}
// 批量文本导入单词：从 PDF / Excel 复制粘贴文本，按行解析
export const batchTextWords = data => request.post('/admin/word/batch-text', data)

export const getAdminPaperList = params => request.get('/admin/paper/list', { params })
export const savePaper = data => request.post('/admin/paper', data)
export const deletePaper = id => request.delete(`/admin/paper/${id}`)

export const getAdminQuestionList = params => request.get('/admin/question/list', { params })
export const saveQuestion = data => request.post('/admin/question', data)
export const deleteQuestion = id => request.delete(`/admin/question/${id}`)
export const importQuestions = (file, paperId) => {
  const form = new FormData()
  form.append('file', file)
  form.append('paperId', paperId)
  return request.post('/admin/question/import', form)
}

export const getStats = () => request.get('/admin/stats')
export const getLogList = params => request.get('/admin/log/list', { params })
