const {app, BrowserWindow} = require('electron');
const path = require('path');
const url = require('url');
const request = require('request');
const http = require("http");
const ScriptUtils = require("./src/ScriptUtils.js");

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let win;
let server_proc;

// url to load NGB, with all panels hidden
let serverUrl = "http://localhost:8080/catgenome#/?rewrite=Off&screenshot=On&toolbar=On&layout=%7B%220%22%3A%7B%221%22%3A%220%22%2C%222%22%3A%220%22%2C%223%22%3A%221%22%2C%22t%22%3A%220%22%2C%22p%22%3A%221%22%2C%22blockedPopoutsThrowError%22%3A%220%22%2C%22closePopoutsOnUnload%22%3A%220%22%2C%22showPopoutIcon%22%3A%221%22%2C%22showMaximiseIcon%22%3A%220%22%2C%22showCloseIcon%22%3A%220%22%2C%22hasHeaders%22%3A%220%22%7D%2C%224%22%3A%7B%225%22%3A3%2C%226%22%3A50%2C%227%22%3A100%2C%228%22%3A30%2C%229%22%3A300%2C%22a%22%3A200%7D%2C%22b%22%3A%7B%22c%22%3A%226%22%2C%22d%22%3A%227%22%2C%22e%22%3A%228%22%2C%22f%22%3A%229%22%2C%22popin%22%3A%22pop%20in%22%7D%2C%22g%22%3A%5B%7B%22n%22%3A%221%22%2C%22l%22%3A%222%22%2C%22k%22%3A100%2C%22m%22%3A100%2C%22t%22%3A%220%22%2C%22o%22%3A%22%22%2C%22g%22%3A%5B%7B%22l%22%3A%224%22%2C%22i%22%3A%7B%22position%22%3A%22center%22%7D%2C%22k%22%3A100%2C%22n%22%3A%220%22%2C%22t%22%3A%220%22%2C%22o%22%3A%22%22%2C%22s%22%3A0%2C%22g%22%3A%5B%7B%22o%22%3A%22Browser%22%2C%22l%22%3A%225%22%2C%22h%22%3A%22angularModule%22%2C%22i%22%3A%7B%22icon%22%3A%22video_label%22%2C%22panel%22%3A%22ngbBrowser%22%2C%22position%22%3A%22center%22%2C%22o%22%3A%22Browser%22%2C%22name%22%3A%22layout%3Ebrowser%22%2C%22key%22%3A%22browser%22%2C%22htmlModule%22%3A%22ngb-browser%22%7D%2C%22n%22%3A%220%22%2C%22t%22%3A%220%22%7D%5D%7D%5D%7D%5D%2C%22n%22%3A%220%22%2C%22t%22%3A%220%22%2C%22o%22%3A%22%22%2C%22q%22%3A%5B%5D%2C%22maximisedItemId%22%3A%7B%7D%7D"

function createWindow() {
    // Create the browser window.
    win = new BrowserWindow({width: 800, height: 600, backgroundColor: '#3498db'});

    // and load the index.html of the app.
    win.loadURL(url.format({
            pathname: path.join(__dirname, 'index.html'),
            protocol: 'file:',
            slashes: true
        }));

    // Open the DevTools.
    ScriptUtils.sleep(14000).then(() => tryLoad());

    // Emitted when the window is closed.
    win.on('closed', () => {
        // Dereference the window object, usually you would store windows
        // in an array if your app supports multi windows, this is the time
        // when you should delete the corresponding element.
        win = null
    })
}

var tryCount = 0;
function tryLoad() {
    try {
        var options = {
            host: "localhost",
            port: 8080,
            path: "/catgenome/restapi/version"
        };

        var req = http.get(options, function (response) {
            response.on('data', function (res) {
                console.log("NGB loaded successfully");
                //console.log(res)
                win.webContents.on('did-finish-load', function () {
                    win.maximize();
                    win.show();
                });

                win.loadURL(serverUrl);

                win.hide()
            })
        });

        req.on('error', function (err) {
            console.log("Waiting NGB application to load: " + tryCount++);
            ScriptUtils.sleep(1000).then(() => {
                tryLoad();
            });
        });

    } catch (e) {
        console.log("Waiting NGB application to load");
        // Usage!
        ScriptUtils.sleep(1000).then(() => {
            tryLoad();
        });
    }
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', createWindow)

server_proc = ScriptUtils.runCommand('java -jar ' + path.join(__dirname + '/lib', 'catgenome.jar'), __dirname + '/lib',
                                     {}, (stdout) => console.log(stdout));

// Quit when all windows are closed.
app.on('window-all-closed', () => {
    // On macOS it is common for applications and their menu bar
    // to stay active until the user quits explicitly with Cmd + Q
    if (process.platform !== 'darwin') {
        ScriptUtils.kill(server_proc.pid);
        app.quit()
    }
});

//exec("java -jar " + path.join(__dirname + '/lib', 'catgenome.jar'))

app.on('activate', () => {
    // On macOS it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (win === null) {
        createWindow();
    }
});

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.