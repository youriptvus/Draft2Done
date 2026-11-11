import test from 'node:test';
import assert from 'node:assert/strict';
import { ResearchEngine } from '../web/services.js';

const empty = () => ({ status: 200, body: JSON.stringify({ message: { items: [] } }) });
const record = () => ({ status: 200, body: JSON.stringify({ message: { items: [{
  DOI: '10.1234/fictional-photosynthesis', title: ['Fictional photosynthesis classroom record'],
  abstract: '<p>A fictional fixture, never evidence of a live search.</p>'
}] } }) });

test('simulated research failures preserve the input project and permit an explicit retry', async () => {
  const paper = { originalText: 'My own original writing.', workingText: 'My own edited writing.', sources: [] };
  const before = JSON.stringify(paper);
  let requests = 0;
  const engine = new ResearchEngine(async () => {
    requests++;
    if (requests === 1) throw new TypeError('Network is unavailable.');
    return record();
  });
  const failed = await engine.search('photosynthesis', ['crossref']);
  assert.equal(failed.sources.length, 0);
  assert.match(failed.errors[0].message, /unavailable/);
  assert.equal(requests, 1, 'no automatic retry or unrelated provider');
  assert.equal(JSON.stringify(paper), before);
  const retried = await engine.search('photosynthesis', ['crossref']);
  assert.equal(retried.sources.length, 1);
  assert.equal(retried.errors.length, 0);
  assert.equal(requests, 2);
  assert.equal(JSON.stringify(paper), before);
});

for (const [label, response, pattern] of [
  ['HTTP 401', { status: 401, body: 'Denied' }, /401/],
  ['HTTP 503', { status: 503, body: 'Unavailable' }, /503/],
  ['invalid JSON', { status: 200, body: '<html>Invalid response</html>' }, /JSON|Unexpected|valid/i],
  ['invalid provider schema', { status: 200, body: '{"message":{"items":null}}' }, /unexpected response/i],
]) test(`simulated search reports ${label} as a provider error without fabricated sources`, async () => {
  const result = await new ResearchEngine(async () => response).search('photosynthesis', ['crossref']);
  assert.equal(result.sources.length, 0);
  assert.equal(result.errors.length, 1);
  assert.equal(result.errors[0].provider, 'crossref');
  assert.match(result.errors[0].message, pattern);
});

test('simulated genuine empty-result schema stays empty without an error or fabricated record', async () => {
  const result = await new ResearchEngine(async () => empty()).search('unmatched fictional term', ['crossref']);
  assert.deepEqual(result.sources, []);
  assert.deepEqual(result.errors, []);
});

test('canceling a simulated in-flight search aborts its transport and saves no partial result', async () => {
  const controller = new AbortController();
  let observedSignal;
  const engine = new ResearchEngine(async ({ signal }) => {
    observedSignal = signal;
    return new Promise(() => {});
  });
  const pending = engine.search('photosynthesis', ['crossref'], controller.signal);
  await Promise.resolve();
  controller.abort();
  await assert.rejects(pending, error => error.name === 'AbortError');
  assert.equal(observedSignal.aborted, true);
});

test('simulated stalled native transport times out and a later explicit request can succeed', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  let calls = 0, observedSignal;
  const engine = new ResearchEngine(async ({ signal }) => {
    calls++;
    observedSignal = signal;
    return calls === 1 ? new Promise(() => {}) : record();
  });
  const pending = engine.search('photosynthesis', ['crossref']);
  await Promise.resolve();
  t.mock.timers.tick(30_001);
  const result = await pending;
  assert.equal(observedSignal.aborted, true);
  assert.equal(result.sources.length, 0);
  assert.match(result.errors[0].message, /timed out/);
  assert.equal(calls, 1);
  const retry = await engine.search('photosynthesis', ['crossref']);
  assert.equal(retry.sources.length, 1);
  assert.equal(calls, 2);
});

test('simulated slow successful search waits for external response before returning records', async () => {
  let release;
  const engine = new ResearchEngine(() => new Promise(resolve => { release = resolve; }));
  let finished = false;
  const pending = engine.search('photosynthesis', ['crossref']).then(value => { finished = true; return value; });
  await Promise.resolve();
  assert.equal(finished, false);
  release(record());
  const result = await pending;
  assert.equal(result.sources.length, 1);
  assert.equal(result.errors.length, 0);
});
