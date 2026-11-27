const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const page = fs.readFileSync('src/main/webapp/WEB-INF/pages/index.jsp', 'utf8');
const inlineScript = page.match(/<script>\s*([\s\S]*?)<\/script>/)[1];
let requests = [];
let scheduled = [];
const context = {
    setFont() {},
    window: {addEventListener() {}},
    $: {ajax(options) {requests.push(options);}},
    setInterval(callback) { scheduled.push(callback); },
    setTimeout(callback) { scheduled.push(callback); },
    console
};
vm.runInNewContext(inlineScript, context);
if (!requests.length && scheduled.length) scheduled.shift()();
assert.strictEqual(requests.length, 1);
context.query();
assert.strictEqual(requests.length, 1, 'a pending request must block the next poll');
assert.strictEqual(scheduled.length, 0, 'do not schedule another request while one is pending');
assert.strictEqual(typeof requests[0].complete, 'function');
requests[0].complete();
assert.strictEqual(scheduled.length, 1);
scheduled.shift()();
assert.strictEqual(requests.length, 2);
console.log('Polling regression passed: pending requests do not overlap.');
