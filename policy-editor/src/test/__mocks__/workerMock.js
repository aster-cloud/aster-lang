// Mock for Web Worker imports (monaco editor workers)
class Worker {
  constructor(stringUrl) {
    this.url = stringUrl;
    this.onmessage = null;
  }

  postMessage(msg) {
    // Mock implementation
  }

  terminate() {
    // Mock implementation
  }
}

module.exports = Worker;
