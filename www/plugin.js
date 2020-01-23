var exec = require('cordova/exec');

exports.saveFileToUSB = function (path, onSuccess, onFail) {
    exec(onSuccess, onFail, "FolderChooser", "saveFileToUSB", [path]);
};

exports.getBackupsFromUSB = function (uri, onSuccess, onFail) {
    exec(onSuccess, onFail, "FolderChooser", "getBackupsFromUSB", [uri]);
};