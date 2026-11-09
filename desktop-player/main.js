const { app, BrowserWindow, shell } = require('electron');
const path = require('node:path');
function createWindow() {
  const win = new BrowserWindow({width:1280,height:800,minWidth:800,minHeight:500,backgroundColor:'#000000',autoHideMenuBar:true,webPreferences:{preload:path.join(__dirname,'preload.js'),contextIsolation:true,nodeIntegration:false,sandbox:true}});
  win.loadURL('https://aksesi.space/player.html');
  win.webContents.setWindowOpenHandler(({url}) => {
    if (url.startsWith('https://aksesi.space/')) return {action:'allow'};
    if (url.startsWith('https://')) shell.openExternal(url);
    return {action:'deny'};
  });
  win.on('page-title-updated', event => event.preventDefault());
}
app.whenReady().then(() => {app.commandLine.appendSwitch('autoplay-policy','no-user-gesture-required');createWindow();app.on('activate',()=>{if(BrowserWindow.getAllWindows().length===0)createWindow();});});
app.on('window-all-closed',()=>{if(process.platform!=='darwin')app.quit();});