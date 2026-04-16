// Input: hour (optional integer 0-23)
// Output: getTimeGreeting - Chinese time-of-day greeting string
// Pos: src/views/Dashboard/ - Dashboard view utilities
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export function getTimeGreeting(hour) {
  if (hour === undefined || hour === null) hour = new Date().getHours()
  if (hour >= 5 && hour <= 11) return '上午好'
  if (hour >= 12 && hour <= 17) return '下午好'
  return '晚上好'
}
