module.exports = {
	open:function (path,onSuccess,onFail){
		cordova.exec(onSuccess,onFail,"FolderChooser","open",[path]);
	}
}
