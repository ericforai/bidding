// 测试 Element Plus el-input textarea maxlength 行为
// 使用 jsdom 模拟浏览器环境

const { JSDOM } = require('jsdom');

// 创建一个简单的 HTML 页面来测试 Element Plus 的行为
const html = `
<!DOCTYPE html>
<html>
<head>
  <title>Test</title>
</head>
<body>
  <div id="app">
    <textarea id="test-textarea" maxlength="50000"></textarea>
    <div id="result"></div>
  </div>
  <script>
    const textarea = document.getElementById('test-textarea');
    const result = document.getElementById('result');

    function testSetValue(value) {
      textarea.value = value;
      const event = new Event('input', { bubbles: true });
      textarea.dispatchEvent(event);
      return textarea.value.length;
    }

    function testPaste(value) {
      const clipboardData = { getData: () => value };
      const pasteEvent = new ClipboardEvent('paste', {
        bubbles: true,
        cancelable: true,
        clipboardData: clipboardData
      });
      textarea.dispatchEvent(pasteEvent);
      return textarea.value.length;
    }

    // 模拟测试
    console.log('=== 原生 textarea maxlength 测试 ===');

    // Test 1: 填入 38737 字符
    const test38737 = '测'.repeat(38737);
    const result38737 = testSetValue(test38737);
    console.log('填入 38737 字符 -> 结果:', result38737);

    // Test 2: 填入 50000 字符
    textarea.value = '';
    const test50000 = '测'.repeat(50000);
    const result50000 = testSetValue(test50000);
    console.log('填入 50000 字符 -> 结果:', result50000);

    // Test 3: 填入 60000 字符 (应该被截断到 50000)
    textarea.value = '';
    const test60000 = '测'.repeat(60000);
    const result60000 = testSetValue(test60000);
    console.log('填入 60000 字符 -> 结果:', result60000);

    // Test 4: 测试浏览器原生 maxlength 行为
    console.log('');
    console.log('=== 浏览器原生 maxlength 行为 ===');
    console.log('textarea.maxLength:', textarea.maxLength);
    console.log('HTML maxlength 属性应该转换为 maxLength DOM property');
  </script>
</body>
</html>
`;

const dom = new JSDOM(html, { runScripts: 'dangerously' });
const document = dom.window.document;
const textarea = document.getElementById('test-textarea');

console.log('textarea.maxLength property:', textarea.maxLength);
console.log('');

// 测试直接赋值
console.log('=== 直接赋值测试 ===');
textarea.value = '测'.repeat(38737);
console.log('赋值 38737 字符 -> value.length:', textarea.value.length);

textarea.value = '测'.repeat(50000);
console.log('赋值 50000 字符 -> value.length:', textarea.value.length);

textarea.value = '测'.repeat(60000);
console.log('赋值 60000 字符 -> value.length:', textarea.value.length, '(浏览器原生不截断)');

// 触发 input 事件
console.log('');
console.log('=== 触发 input 事件后 ===');
const inputEvent = new dom.window.Event('input', { bubbles: true });
textarea.dispatchEvent(inputEvent);
console.log('input 事件后 value.length:', textarea.value.length);

dom.window.close();
