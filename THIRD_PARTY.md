# Third-party software and attribution

## OpenCode (adapted source)

PhoneCode's agent draws on **OpenCode** (https://github.com/anomalyco/opencode, https://opencode.ai):
its prompt structure, tool schemas, and loop design are adapted from OpenCode's, and several tools
(webfetch, todo, external-directory, question, plan_exit, task) mirror OpenCode's equivalents.

OpenCode is MIT licensed. The required notice is preserved here:

    MIT License

    Copyright (c) 2025 opencode

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

PhoneCode is an independent project and is not built by, endorsed by, or affiliated with the OpenCode
team (Anomaly). "OpenCode", "OpenCode Zen", and "OpenCode Go" are used nominatively to describe
interoperability and origins.

# Vendored binary artifacts

Prebuilt third-party files committed into the app, with pinned sources so each is auditable: re-fetch
from the pinned version and compare the SHA-256.

## Mermaid (diagram rendering)

- **File:** `app/src/main/assets/mermaid.min.js`
- **What:** the Mermaid diagram engine, inlined into a WebView to render ` ```mermaid ` blocks (trees,
  graphs, flowcharts, sequence/state/ER diagrams) on-device with no network.
- **Source:** https://www.npmjs.com/package/mermaid, `dist/mermaid.min.js` (the UMD build exposing
  `window.mermaid`).
- **Pinned version:** 10.9.3
- **SHA-256:** `5a8ec91820bd55afef049068489369910e5d6ce70c8103952f27e29d3e76e8bc`
- **License:** MIT.

To re-verify:

    curl -fsSL https://cdn.jsdelivr.net/npm/mermaid@10.9.3/dist/mermaid.min.js | shasum -a 256

## PRoot (Linux userland)

See `app/src/main/jniLibs/PROVENANCE.md` for the arm64 `libproot.so` + `libproot-loader.so` provenance
(green-green-avk/build-proot-android, pinned commit, SHA-256, licenses).
