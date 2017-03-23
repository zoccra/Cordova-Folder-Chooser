module.exports = {
	open:function (path){
		var onSuccess = function(path){
			console.log(path);
		};
		var onFail = function(){
			console.log('fail');
		};
		cordova.exec(onSuccess,onFail,"FolderChooser","open",[path]);
	}
}
