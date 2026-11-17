export const permission = {
  mounted(el, binding) {
    const roles = binding.value
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    const role = userInfo?.role || 'visitor'
    if (roles && !roles.includes(role)) {
      el.parentNode?.removeChild(el)
    }
  }
}
