var exec = require('cordova/exec');

exports.open = function (path,onSuccess,onFail) {
    exec(onSuccess,onFail,"FolderChooser","open",[path]);
};