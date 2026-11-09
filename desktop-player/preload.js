const { contextBridge } = require('electron');
contextBridge.exposeInMainWorld('aksesiDesktop', Object.freeze({platform:process.platform}));