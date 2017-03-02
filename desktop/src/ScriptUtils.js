const exec = require('child_process').exec;
const psTree = require('ps-tree');

function createEnv(params) {
    let env = {};
    let item;

    for (item in process.env) {
        env[item] = process.env[item];
    }

    for(item in params) {
        env[item] = params[item];
    }

    return env;
}

exports.runCommand = function (cmd, workingDirectory, environment, callback) {
    let child = exec(cmd,
        {
            cwd: workingDirectory,
            env: createEnv(environment)
        },
        function (error, stdout, stderr) {
            if (callback) {
                callback(stdout);
            }
        }
    );

    return child;
}

function killLinux(pid, signal, callback) {
    signal   = signal || 'SIGKILL';
    callback = callback || function () {};
    var killTree = true;
    if(killTree) {
        psTree(pid, function (err, children) {
            [pid].concat(
                children.map(function (p) {
                    return p.PID;
                })
            ).forEach(function (tpid) {
                try { process.kill(tpid, signal) }
                catch (ex) { }
            });
            callback();
        });
    } else {
        try { process.kill(pid, signal) }
        catch (ex) { }
        callback();
    }
}

exports.kill = function(pid) {
    var isWin = /^win/.test(process.platform);
    if(!isWin) {
        killLinux(pid);
    } else {
        //cp.exec('taskkill /PID ' + pid + ' /T /F', function (error, stdout, stderr)
        exec('taskkill /IM java.exe /T /F', function (error, stdout, stderr) {

        });
    }
}

exports.sleep = function (time) {
    return new Promise((resolve) => setTimeout(resolve, time));
}