import request from '@/utils/request'

// ===== 背单词 =====
export const getWordBooks = level => request.get('/student/word/books', { params: level ? { level } : {} })
export const getTodayWords = params => request.get('/student/word/today', { params })
export const getWordDetail = wordId => request.get(`/student/word/detail/${wordId}`)
export const submitWordReview = data => request.post('/student/word/review', data)
export const getMyWords = params => request.get('/student/word/my', { params })
export const getWeakWordReport = () => request.get('/student/word/weak-report')
export const getWordStat = () => request.get('/student/word/stat')

// ===== 单词错题本 =====
export const addWrongWord = (wordId, reason = 'manual') =>
  request.post(`/student/word/wrong/${wordId}`, null, { params: { reason } })
export const getWrongWordList = params => request.get('/student/word/wrong', { params })
export const removeWrongWord = id => request.delete(`/student/word/wrong/${id}`)
export const removeWrongWordByWordId = wordId => request.delete(`/student/word/wrong/by-word/${wordId}`)
export const checkWrongWord = wordId => request.get(`/student/word/wrong/check/${wordId}`)
// ===== 学习计划信息（背单词页用） =====
export const getWordPlanInfo = () => request.get('/student/word/plan-info')

// ===== 刷题 =====
export const getPaperList = level => request.get('/student/paper/list', { params: level ? { level } : {} })
export const getQuestionList = params => request.get('/student/question/list', { params })
export const getQuestionDetail = id => request.get(`/student/question/${id}`)
export const getQuestionReveal = id => request.get(`/student/question/reveal/${id}`)
export const submitAnswer = data => request.post('/student/question/submit', data)
export const getErrorList = params => request.get('/student/error/list', { params })
export const markMaster = id => request.put(`/student/error/master/${id}`)
export const favoriteQuestion = questionId => request.post(`/student/error/favorite/${questionId}`)
export const getErrorQuestionIds = () => request.get('/student/error/ids')
export const getWeakQuestionReport = () => request.get('/student/question/weak-report')

// ===== 学习计划 =====
export const getPlanList = () => request.get('/student/plan/list')
export const savePlan = data => request.post('/student/plan', data)
export const deletePlan = id => request.delete(`/student/plan/${id}`)

// ===== 消息 =====
export const getMessageList = params => request.get('/student/message/list', { params })
export const readMessage = id => request.put(`/student/message/read/${id}`)

// ===== 今日背诵 =====
export const getTodayWordList = params => request.get('/student/word/today', { params })

// ===== 中英互译 =====
export const translateText = data => request.post('/student/translate', data)
export const getTranslateRecords = () => request.get('/student/translate/records')
export const deleteTranslateRecord = id => request.delete(`/student/translate/records/${id}`)
export const clearTranslateRecords = () => request.delete('/student/translate/records')
